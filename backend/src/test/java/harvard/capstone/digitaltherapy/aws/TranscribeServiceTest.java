package harvard.capstone.digitaltherapy.aws;

import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TranscribeServiceTest {

    @Mock
    private AmazonTranscribe amazonTranscribe;  // Mock AmazonTranscribe service

    @InjectMocks
    private TranscribeService transcribeService;  // Inject mock into service class

    @Test
    public void testStartTranscriptionJobWhenJobExistsAndCompleted() {
        // Mock mediaUri and jobName
        String mediaUri = "s3://dta-root/dta-speech-translation-storage/temp-audio.mp3";
        String jobName = "MyTranscriptionJob";

        // Mock GetTranscriptionJobRequest with the given jobName
        GetTranscriptionJobRequest getRequest = new GetTranscriptionJobRequest()
                .withTranscriptionJobName(jobName);

        // Mock response for an existing and completed transcription job
        TranscriptionJob transcriptionJob = new TranscriptionJob()
                .withTranscriptionJobName(jobName)
                .withTranscriptionJobStatus(TranscriptionJobStatus.COMPLETED)
                .withTranscript(new Transcript().withTranscriptFileUri("https://s3.amazonaws.com/dta-root/my-transcription-output.json"));

        // Create the GetTranscriptionJobResult and set the transcription job
        GetTranscriptionJobResult getResult = new GetTranscriptionJobResult()
                .withTranscriptionJob(transcriptionJob);

        // Mock the call to getTranscriptionJob to return the mocked result
        when(amazonTranscribe.getTranscriptionJob(getRequest)).thenReturn(getResult);

        // Mock the call to startTranscriptionJob (to simulate job being started)
        StartTranscriptionJobResult startResult = new StartTranscriptionJobResult()
                .withTranscriptionJob(transcriptionJob);
        when(amazonTranscribe.startTranscriptionJob(any(StartTranscriptionJobRequest.class)))
                .thenReturn(startResult);  // Fix mock for startTranscriptionJob

        // Call the method under test with the mocked mediaUri and jobName
        String result = transcribeService.startTranscriptionJob(mediaUri, jobName);

        // Verify that the correct URL is returned
        assertNotNull(result, "The result should not be null.");
        assertEquals("https://s3.amazonaws.com/dta-root/my-transcription-output.json", result, "The result should be the correct URL.");
    }

    @Test
    public void testStartTranscriptionJobWhenJobDoesNotExist() throws InterruptedException {
        // Mock mediaUri and jobName
        String mediaUri = "s3://dta-root/dta-speech-translation-storage/temp-audio.mp3";
        String jobName = "MyTranscriptionJob";
    
        // Mock GetTranscriptionJobRequest to simulate that the job doesn't exist
        GetTranscriptionJobRequest getRequest = new GetTranscriptionJobRequest()
                .withTranscriptionJobName(jobName);
    
        // Mock GetTranscriptionJobResult to simulate the job not existing
        GetTranscriptionJobResult getTranscriptionJobResult = mock(GetTranscriptionJobResult.class);
        when(amazonTranscribe.getTranscriptionJob(getRequest)).thenReturn(getTranscriptionJobResult);
    
        // Mock the TranscriptionJob being returned when getTranscriptionJob is called on the result
        TranscriptionJob transcriptionJobInProgress = new TranscriptionJob()
                .withTranscriptionJobName(jobName)
                .withTranscriptionJobStatus(TranscriptionJobStatus.IN_PROGRESS);
        when(getTranscriptionJobResult.getTranscriptionJob()).thenReturn(transcriptionJobInProgress);
    
        // Simulate job status change after a short interval
        TranscriptionJob transcriptionJobCompleted = new TranscriptionJob()
                .withTranscriptionJobName(jobName)
                .withTranscriptionJobStatus(TranscriptionJobStatus.COMPLETED)
                .withTranscript(new Transcript().withTranscriptFileUri("https://s3.amazonaws.com/dta-root/my-transcription-output.json"));
        when(amazonTranscribe.getTranscriptionJob(getRequest)).thenReturn(getTranscriptionJobResult);
        when(getTranscriptionJobResult.getTranscriptionJob()).thenReturn(transcriptionJobCompleted);
    
        // Mock the call to startTranscriptionJob to simulate job being started
        StartTranscriptionJobResult startResult = new StartTranscriptionJobResult()
                .withTranscriptionJob(transcriptionJobInProgress);
        when(amazonTranscribe.startTranscriptionJob(any(StartTranscriptionJobRequest.class)))
                .thenReturn(startResult);
    
        // Call the method under test
        String result = transcribeService.startTranscriptionJob(mediaUri, jobName);
    
        // Verify that the URL of the transcribed file is returned
        assertNotNull(result, "The result should not be null.");
        assertEquals("https://s3.amazonaws.com/dta-root/my-transcription-output.json", result, "The result should be the correct URL.");
    }
}
