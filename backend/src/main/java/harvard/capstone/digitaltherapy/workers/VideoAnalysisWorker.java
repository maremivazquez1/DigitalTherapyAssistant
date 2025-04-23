package harvard.capstone.digitaltherapy.workers;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.*;

/**
 * Handles asynchronous face detection in video files stored on S3.
 * Utilizes AWS Rekognition to extract the top three facial emotions per face detection frame,
 * then maps the results into a simplified, clean JSON structure for downstream systems.
 */
public class VideoAnalysisWorker {

    private static final Logger logger = LoggerFactory.getLogger(VideoAnalysisWorker.class);

    private final RekognitionClient rekognitionClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CompletableFuture<String>> jobFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Constructs a VideoAnalysisWorker instance with default AWS credentials and region.
     */
    public VideoAnalysisWorker() {
        this.rekognitionClient = RekognitionClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
    }

    /**
     * Converts an s3://bucket/key URI into an S3Object instance.
     * Validates and splits the bucket and key from the URL.
     *
     * @param s3Url The full S3 URI in s3://bucket/key format
     * @return An S3Object containing the bucket and object key
     * @throws IllegalArgumentException if the S3 URI is malformed
     */
    private S3Object parseS3Url(String s3Url) {
        try {
            if (!s3Url.startsWith("s3://")) {
                throw new IllegalArgumentException("Invalid S3 URL format. URL must start with 's3://'");
            }
            String path = s3Url.substring(5);
            int bucketEndIndex = path.indexOf('/');
            if (bucketEndIndex == -1) {
                throw new IllegalArgumentException("Invalid S3 URL. Could not find '/' to separate bucket and key.");
            }
            String bucket = path.substring(0, bucketEndIndex);
            String key = path.substring(bucketEndIndex + 1);
            return S3Object.builder().bucket(bucket).name(key).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing S3 URL: " + s3Url, e);
        }
    }

    /**
     * Initiates a Rekognition face detection job on a video stored in S3.
     *
     * @param s3Url The S3 path to the video (e.g., s3://bucket/video.mp4)
     * @return CompletableFuture that resolves to a serialized JSON summary of the detection results
     */
    public CompletableFuture<String> detectFacesFromVideoAsync(String s3Url) {
        S3Object s3Object = parseS3Url(s3Url);
        Video video = Video.builder().s3Object(s3Object).build();

        StartFaceDetectionRequest request = StartFaceDetectionRequest.builder()
            .video(video)
            .faceAttributes(FaceAttributes.ALL)
            .build();

        StartFaceDetectionResponse startResponse = rekognitionClient.startFaceDetection(request);
        String jobId = startResponse.jobId();

        logger.info("Started face detection job: {}", jobId);

        CompletableFuture<String> future = new CompletableFuture<>();
        jobFutures.put(jobId, future);
        schedulePolling(jobId, future, 0);

        return future;
    }

    /**
     * Periodically polls Rekognition for the status of the job.
     * On success, converts the results into a simplified custom structure.
     *
     * @param jobId The Rekognition job ID returned by the start request
     * @param future The future that will be completed when the job finishes
     * @param retryCount The number of poll attempts so far (used for timeout logic)
     */
    private void schedulePolling(String jobId, CompletableFuture<String> future, int retryCount) {
        scheduler.schedule(() -> {
            try {
                GetFaceDetectionResponse response = rekognitionClient.getFaceDetection(
                    GetFaceDetectionRequest.builder().jobId(jobId).maxResults(1000).build()
                );

                String status = response.jobStatusAsString();
                logger.debug("Polling job {} (attempt {}): {}", jobId, retryCount + 1, status);

                switch (status) {
                    case "SUCCEEDED" -> {
                        logger.info("Face detection job {} completed successfully with {} faces.", jobId, response.faces().size());

                        FaceDetectionResponse customResponse = mapToCustomResponse(response);
                        String json = objectMapper.writeValueAsString(customResponse);
                        logger.info("Serialized JSON response: {}", json);

                        future.complete(json);
                        jobFutures.remove(jobId);
                    }
                    case "FAILED" -> {
                        logger.error("Face detection job {} failed.", jobId);
                        future.completeExceptionally(new RuntimeException("Face detection job failed: " + jobId));
                        jobFutures.remove(jobId);
                    }
                    case "IN_PROGRESS" -> {
                        if (retryCount < 60) {
                            schedulePolling(jobId, future, retryCount + 1);
                        } else {
                            logger.warn("Job {} timed out after {} attempts.", jobId, retryCount);
                            future.completeExceptionally(new TimeoutException("Face detection job timed out: " + jobId));
                            jobFutures.remove(jobId);
                        }
                    }
                    default -> {
                        logger.warn("Unknown status for job {}: {}", jobId, status);
                        future.completeExceptionally(new IllegalStateException("Unexpected job status: " + status));
                        jobFutures.remove(jobId);
                    }
                }
            } catch (Exception e) {
                logger.error("Polling error for job " + jobId, e);
                future.completeExceptionally(e);
                jobFutures.remove(jobId);
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Converts a Rekognition GetFaceDetectionResponse into a simplified FaceDetectionResponse
     * that includes only the timestamp, overall face confidence, and the top 3 emotions.
     *
     * @param response The original Rekognition face detection response
     * @return A simplified version of the data suitable for external use
     */
    private FaceDetectionResponse mapToCustomResponse(GetFaceDetectionResponse response) {
        FaceDetectionResponse customResponse = new FaceDetectionResponse();
        customResponse.setJobStatus(response.jobStatusAsString());

        List<FaceDetection> faceDetections = new ArrayList<>();
        for (software.amazon.awssdk.services.rekognition.model.FaceDetection faceDetection : response.faces()) {
            FaceDetection customFaceDetection = new FaceDetection();
            customFaceDetection.setTimestamp(faceDetection.timestamp());

            FaceDetail faceDetail = new FaceDetail();
            faceDetail.setConfidence(faceDetection.face().confidence());

            List<software.amazon.awssdk.services.rekognition.model.Emotion> rekEmotions = faceDetection.face().emotions();

            List<Emotion> topEmotions = rekEmotions.stream()
                .sorted((a, b) -> Double.compare(b.confidence(), a.confidence()))
                .limit(3)
                .map(e -> new Emotion(e.typeAsString(), e.confidence()))
                .toList();

            faceDetail.setEmotions(topEmotions);
            customFaceDetection.setFace(faceDetail);
            faceDetections.add(customFaceDetection);
        }

        customResponse.setFaces(faceDetections);
        return customResponse;
    }

    /* ------------------------------ Custom Output POJOs ------------------------------ */

    /**
     * Container for emotion type and confidence score.
     * Represents simplified output from AWS Rekognition Emotion class.
     */
    private static class Emotion {
        private String type;
        private double confidence;

        public Emotion() {}

        public Emotion(String type, double confidence) {
            this.type = type;
            this.confidence = confidence;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    /**
     * Represents a single detected face at a given timestamp,
     * including confidence and top 3 facial emotions.
     */
    private static class FaceDetail {
        private double confidence;
        private List<Emotion> emotions;

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public List<Emotion> getEmotions() { return emotions; }
        public void setEmotions(List<Emotion> emotions) { this.emotions = emotions; }
    }

    /**
     * Combines timestamp and face detail result into a single detection record.
     */
    private static class FaceDetection {
        private long timestamp;
        private FaceDetail face;

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public FaceDetail getFace() { return face; }
        public void setFace(FaceDetail face) { this.face = face; }
    }

    /**
     * Final JSON-mapped structure containing job status and a list of face detections.
     */
    private static class FaceDetectionResponse {
        private String jobStatus;
        private List<FaceDetection> faces;

        public String getJobStatus() { return jobStatus; }
        public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

        public List<FaceDetection> getFaces() { return faces; }
        public void setFaces(List<FaceDetection> faces) { this.faces = faces; }
    }
}
