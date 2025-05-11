package harvard.capstone.digitaltherapy.aws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.util.Map;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class RekognitionService {

    @Autowired
    private RekognitionClient rekognitionClient;

    private static final Logger logger = LoggerFactory.getLogger(RekognitionService.class);

    // In-memory tracking (consider external store for production)
    private final Map<String, CompletableFuture<String>> jobFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Async
    public CompletableFuture<String> detectFacesFromVideoAsync(String s3Url) {
        S3Object s3Object = parseS3Url(s3Url);
        Video video = Video.builder().s3Object(s3Object).build();

        StartFaceDetectionRequest request = StartFaceDetectionRequest.builder()
            .video(video)
            .faceAttributes(FaceAttributes.ALL)
            .build();

        StartFaceDetectionResponse startResponse = rekognitionClient.startFaceDetection(request);
        String jobId = startResponse.jobId();

        logger.info("Started async face detection job: {}", jobId);

        CompletableFuture<String> future = new CompletableFuture<>();
        jobFutures.put(jobId, future);

        schedulePolling(jobId, future, 0);

        return future;
    }

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

                        // Map the GetFaceDetectionResponse to your custom FaceDetectionResponse
                        FaceDetectionResponse customResponse = mapToCustomResponse(response);

                        // Serialize your mapped custom response to JSON
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

    private FaceDetectionResponse mapToCustomResponse(GetFaceDetectionResponse response) {
        FaceDetectionResponse customResponse = new FaceDetectionResponse();
        customResponse.setJobStatus(response.jobStatusAsString());

        // Map the FaceDetection and FaceDetail
        List<FaceDetection> faceDetections = new ArrayList<>();
        for (software.amazon.awssdk.services.rekognition.model.FaceDetection faceDetection : response.faces()) {
            FaceDetection customFaceDetection = new FaceDetection();
            customFaceDetection.setTimestamp(faceDetection.timestamp());

            FaceDetail faceDetail = new FaceDetail();
            faceDetail.setConfidence(faceDetection.face().confidence());
            
            // Map emotions
            List<Emotion> emotions = new ArrayList<>();
            Emotion topEmotion = null;
            for (software.amazon.awssdk.services.rekognition.model.Emotion rekEmotion : faceDetection.face().emotions()) {
                Emotion current = new Emotion(rekEmotion.typeAsString(), rekEmotion.confidence());
                if (topEmotion == null || current.getConfidence() > topEmotion.getConfidence()) {
                    topEmotion = current;
                }
            }
            if (topEmotion != null) {
                emotions.add(topEmotion);
            }
            faceDetail.setEmotions(emotions);

            customFaceDetection.setFace(faceDetail);

            faceDetections.add(customFaceDetection);
        }
        customResponse.setFaces(faceDetections);

        return customResponse;
    }

    /* ---------------------------------- Face Detail Mapping ---------------------------------- */

    private static class FaceDetail {
        private double confidence;
        private List<Emotion> emotions;
    
        public double getConfidence() {
            return confidence;
        }
    
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    
        public List<Emotion> getEmotions() {
            return emotions;
        }
    
        public void setEmotions(List<Emotion> emotions) {
            this.emotions = emotions;
        }
    }

    private static class Emotion {
        private String type;
        private double confidence;
    
        public Emotion() {}
    
        public Emotion(String type, double confidence) {
            this.type = type;
            this.confidence = confidence;
        }
    
        public String getType() {
            return type;
        }
    
        public void setType(String type) {
            this.type = type;
        }
    
        public double getConfidence() {
            return confidence;
        }
    
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    }
    
    private static class FaceDetection {
        private long timestamp;
        private FaceDetail face;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public FaceDetail getFace() {
            return face;
        }

        public void setFace(FaceDetail face) {
            this.face = face;
        }
    }

    private static class FaceDetectionResponse {
        private String jobStatus;
        private List<FaceDetection> faces;

        public String getJobStatus() {
            return jobStatus;
        }

        public void setJobStatus(String jobStatus) {
            this.jobStatus = jobStatus;
        }

        public List<FaceDetection> getFaces() {
            return faces;
        }

        public void setFaces(List<FaceDetection> faces) {
            this.faces = faces;
        }
    }
}
