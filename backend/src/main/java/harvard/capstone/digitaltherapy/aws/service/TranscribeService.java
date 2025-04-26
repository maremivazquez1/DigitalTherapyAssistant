package harvard.capstone.digitaltherapy.aws.service;

import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.DeleteTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.TranscriptionJob;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TranscribeService {

    @Autowired
    private AmazonTranscribe amazonTranscribe;

    /**
     * Speech-to-Text service function
     *
     * @param mediUri .mp3 S3 asset URI. Expected format: s3://dta-root/dta-speech-translation-storage/[autiofileID].mp3
     * @param jobName user/job id string for the AWS Transcribe service
     * @return The URL for the translated file from the Transcribe job
     */
    public String startTranscriptionJob(String mediaUri, String jobName) {
        if (mediaUri == null || mediaUri.isEmpty()) {
            throw new IllegalArgumentException("Invalid media URI");
        }
        if (jobName == null || jobName.isEmpty()) {
            throw new IllegalArgumentException("Invalid job name");
        }

        // Check if a transcription job already exists with the given jobName
        GetTranscriptionJobRequest getRequest = new GetTranscriptionJobRequest().withTranscriptionJobName(jobName);
        try {
            // Check the status of the existing transcription job
            GetTranscriptionJobResult result = amazonTranscribe.getTranscriptionJob(getRequest);
            TranscriptionJob existingJob = result.getTranscriptionJob();

            // If the job is in "COMPLETED" or "FAILED" state, delete the existing job results
            if ("COMPLETED".equals(existingJob.getTranscriptionJobStatus()) || 
                "FAILED".equals(existingJob.getTranscriptionJobStatus())) {
                deleteTranscriptionJob(jobName); // Delete existing job results
            }
        } catch (Exception e) {
            // Job not found, continue to create a new job
            // Do not throw any exceptions
        }

        // Create request object for starting the job
        StartTranscriptionJobRequest request = new StartTranscriptionJobRequest()
                .withTranscriptionJobName(jobName)
                .withLanguageCode("en-US")
                .withMedia(new Media().withMediaFileUri(mediaUri))
                .withOutputBucketName("dta-root");

        // Start the transcription job
        StartTranscriptionJobResult result = amazonTranscribe.startTranscriptionJob(request);
        TranscriptionJob job = result.getTranscriptionJob();

        // Wait for the job to complete
        int requestDelayMs = 200;
        int maxDelayMs = 5000;
        while (!job.getTranscriptionJobStatus().equals("COMPLETED") && 
               !job.getTranscriptionJobStatus().equals("FAILED")) {
            try {
                // Sleep for a short interval before checking the status again
                Thread.sleep(requestDelayMs);  // Wait request delay
                if (requestDelayMs < maxDelayMs)
                    requestDelayMs += 500;
                else
                    requestDelayMs = maxDelayMs;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "Job interrupted";
            }

            // Create a request to fetch the current status of the transcription job
            getRequest = new GetTranscriptionJobRequest()
                    .withTranscriptionJobName(jobName);

            // Retrieve the updated job status
            job = amazonTranscribe.getTranscriptionJob(getRequest).getTranscriptionJob();
        }

        // If the job is completed, get the URL of the transcribed file
        if ("COMPLETED".equals(job.getTranscriptionJobStatus())) {
            String bucketName = "dta-root";
            String fileKey = job.getTranscript().getTranscriptFileUri().substring(job.getTranscript().getTranscriptFileUri().lastIndexOf('/') + 1);
            String fileUrl = "https://s3.amazonaws.com/" + bucketName + "/" + fileKey;
            return fileUrl;
        } else {
            return "Transcription job failed.";
        }
    }

    // Method to delete the transcription job if it's completed or failed
    public void deleteTranscriptionJob(String jobName) {
        // Create the delete request for the transcription job
        DeleteTranscriptionJobRequest deleteRequest = new DeleteTranscriptionJobRequest()
                .withTranscriptionJobName(jobName);

        // Delete the transcription job
        try {
            // Delete the transcription job and get the result
            amazonTranscribe.deleteTranscriptionJob(deleteRequest);
            System.out.println("Transcription job " + jobName + " deleted successfully.");
        } catch (Exception e) {
            // Handle any errors that may occur when deleting the job
            System.out.println("Failed to delete transcription job " + jobName + ": " + e.getMessage());
        }
    }
}
