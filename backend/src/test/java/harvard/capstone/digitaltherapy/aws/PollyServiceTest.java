package harvard.capstone.digitaltherapy.aws;

import harvard.capstone.digitaltherapy.aws.service.PollyService;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PollyServiceTest {

    @Mock
    private AmazonPolly amazonPolly;

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private PollyService pollyService;

    @Test
    public void testConvertTextToSpeech_Success() {
        // Mock Polly's response
        SynthesizeSpeechResult mockResult = new SynthesizeSpeechResult();
        InputStream mockAudioStream = new ByteArrayInputStream("mock audio data".getBytes());
        mockResult.setAudioStream(mockAudioStream);

        when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class))).thenReturn(mockResult);

        // Call the method
        String resultUrl = pollyService.convertTextToSpeech("Hello, Polly!", "test-file");

        // Expected S3 file URL
        String expectedUrl = "https://dta-root.s3.amazonaws.com/dta-speech-translation-storage/test-file.mp3";
        assertEquals(expectedUrl, resultUrl, "S3 URL should match the expected format");

        // Verify Polly was called with the correct request
        ArgumentCaptor<SynthesizeSpeechRequest> requestCaptor = ArgumentCaptor.forClass(SynthesizeSpeechRequest.class);
        verify(amazonPolly).synthesizeSpeech(requestCaptor.capture());
        SynthesizeSpeechRequest capturedRequest = requestCaptor.getValue();

        assertEquals("Hello, Polly!", capturedRequest.getText());
        assertEquals("Joanna", capturedRequest.getVoiceId());
        assertEquals("mp3", capturedRequest.getOutputFormat());

        // Verify that the file was uploaded to S3
        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(putRequestCaptor.capture());

        PutObjectRequest capturedPutRequest = putRequestCaptor.getValue();
        assertEquals("dta-root", capturedPutRequest.getBucketName());
        assertEquals("dta-speech-translation-storage/test-file.mp3", capturedPutRequest.getKey());
    }

    @Test
    public void testConvertTextToSpeech_AmazonPollyFailure() {
        // Mock Polly throwing an exception
        when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class)))
                .thenThrow(new RuntimeException("Polly service failure"));

        // Assert that the method throws an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pollyService.convertTextToSpeech("Error case", "test-error");
        });

        // Verify the exception message
        assertEquals("Polly service failure", exception.getMessage());

        // Verify that S3 was never called since Polly failed
        verifyNoInteractions(amazonS3);
    }
}
