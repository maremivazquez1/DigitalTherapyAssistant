package harvard.capstone.digitaltherapy.aws;

import harvard.capstone.digitaltherapy.aws.service.PollyService;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public void testConvertTextToSpeech_Success() throws IOException {
        // Load sample text from file
        String filePath = "src/test/resources/transcript-text-sample-1.txt";
        String textFromS3 = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);

        // Mock the downloadTextFromS3 method to return the mock text
        when(amazonS3.getObject(any(GetObjectRequest.class)))
                .thenReturn(new com.amazonaws.services.s3.model.S3Object() {{
                    setObjectContent(new ByteArrayInputStream(textFromS3.getBytes()));
                }});

        // Mock Polly's response
        SynthesizeSpeechResult mockResult = new SynthesizeSpeechResult();
        InputStream mockAudioStream = new ByteArrayInputStream("mock audio data".getBytes());
        mockResult.setAudioStream(mockAudioStream);

        when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class))).thenReturn(mockResult);

        // Call the method with a mock S3 URL
        String resultUrl = pollyService.convertTextToSpeech("https://dta-root.s3.amazonaws.com/dta-speech-translation-storage/sample.txt", "test-file");

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
        // Use lenient() to avoid UnnecessaryStubbingException
        lenient().when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class)))
                .thenThrow(new RuntimeException("Polly service failure"));

        // Assert that the method throws an exception
        Exception exception = assertThrows(Exception.class, () -> {
            pollyService.convertTextToSpeech("Error case", "test-error");
        });

        // Verify the exception message exists
        assertNotNull(exception.getMessage());

        // Verify that S3 was never called since Polly failed
        verifyNoInteractions(amazonS3);
    }

}
