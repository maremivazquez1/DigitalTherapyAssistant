package harvard.capstone.digitaltherapy.aws.service;

import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.model.*;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class TranscribeService {

    @Autowired
    private AmazonTranscribe amazonTranscribe;

    // Executor service for asynchronous task handling
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Speech-to-Text service function for quick chunks.
     * Starts the transcription job asynchronously and returns the result URL when ready.
     *
     * @param mediaUri .mp3 S3 asset URI. Expected format: s3://dta-root/dta-speech-translation-storage/[audiofileID].mp3
     * @param jobName  user/job id string for the AWS Transcribe service
     * @return A CompletableFuture that will eventually hold the URL for the translated file
     */
    public CompletableFuture<String> startTranscriptionJobAsync(String mediaUri, String jobName) {
        if (mediaUri == null || mediaUri.isEmpty()) {
            throw new IllegalArgumentException("Invalid media URI");
        }
        if (jobName == null || jobName.isEmpty()) {
            throw new IllegalArgumentException("Invalid job name");
        }

        // Return a CompletableFuture that will be completed when transcription job is finished
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, check if a job with the same name exists and delete it if found
                deleteExistingTranscriptionJob(jobName);

                // Create the transcription job request
                StartTranscriptionJobRequest request = new StartTranscriptionJobRequest()
                        .withTranscriptionJobName(jobName)
                        .withLanguageCode("en-US")
                        .withMedia(new Media().withMediaFileUri(mediaUri))
                        .withOutputBucketName("dta-root");

                // Start the transcription job
                StartTranscriptionJobResult result = amazonTranscribe.startTranscriptionJob(request);
                TranscriptionJob job = result.getTranscriptionJob();

                // Poll the transcription job asynchronously for the result
                return pollTranscriptionJobResult(job);
            } catch (Exception e) {
                e.printStackTrace();
                return "Error starting transcription job.";
            }
        }, executorService); // Use the executor service to run the task asynchronously
    }

    /**
     * Checks if a transcription job with the given name exists and deletes it if found.
     *
     * @param jobName The name of the transcription job to check and delete if it exists.
     */
    private void deleteExistingTranscriptionJob(String jobName) {
        try {
            // Create a request to get the transcription job status
            GetTranscriptionJobRequest getRequest = new GetTranscriptionJobRequest()
                    .withTranscriptionJobName(jobName);

            // Try to fetch the job status
            GetTranscriptionJobResult getResult = amazonTranscribe.getTranscriptionJob(getRequest);

            // If the job exists, delete it
            if (getResult != null && getResult.getTranscriptionJob() != null) {
                amazonTranscribe.deleteTranscriptionJob(new DeleteTranscriptionJobRequest().withTranscriptionJobName(jobName));
                System.out.println("Deleted existing transcription job with name: " + jobName);
            }
        } catch (NoSuchKeyException e) {
            // If the job doesn't exist, just ignore it.
            System.out.println("No existing job with name: " + jobName);
        } catch (Exception e) {
            System.err.println("Error while trying to delete the existing transcription job: " + e.getMessage());
        }
    }

    /**
     * Polls the transcription job until it's completed or failed.
     *
     * @param job The transcription job to monitor
     * @return The URL of the transcribed file when the job completes
     */
    private String pollTranscriptionJobResult(TranscriptionJob job) {
        // Create request for checking job status
        GetTranscriptionJobRequest getRequest = new GetTranscriptionJobRequest()
                .withTranscriptionJobName(job.getTranscriptionJobName());

        int requestDelayMs = 1000; // Poll every second
        int maxDelayMs = 5000;    // Max delay for polling

        while (true) {
            try {
                // Fetch job status
                job = amazonTranscribe.getTranscriptionJob(getRequest).getTranscriptionJob();

                // If job completed or failed, exit the loop
                if ("COMPLETED".equals(job.getTranscriptionJobStatus()) || "FAILED".equals(job.getTranscriptionJobStatus())) {
                    break;
                }

                // Sleep before checking again
                Thread.sleep(requestDelayMs);
                if (requestDelayMs < maxDelayMs) {
                    requestDelayMs += 500;  // Increase delay up to max value
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Job interrupted.";
            }
        }

        // If job is completed, return the URL of the transcribed file
        if ("COMPLETED".equals(job.getTranscriptionJobStatus())) {
            String fileUrl = job.getTranscript().getTranscriptFileUri();
            return "https://s3.amazonaws.com/dta-root/" + fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        } else {
            return "Transcription job failed.";
        }
    }

    // You can also add methods for deleting jobs or handling exceptions.
}
