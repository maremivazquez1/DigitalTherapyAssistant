package harvard.capstone.digitaltherapy.aws;

import harvard.capstone.digitaltherapy.aws.service.PollyService;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class PollyServiceTest {

    @Mock
    private AmazonPolly amazonPolly;

    @Mock
    private AmazonS3 amazonS3; // Mock AmazonS3 to simulate the upload process

    @InjectMocks
    private PollyService pollyService;

    @Test
    public void testConvertTextToSpeech() throws IOException {
        // Mock the response from AmazonPolly
        SynthesizeSpeechResult mockResult = new SynthesizeSpeechResult();
        InputStream mockAudioStream = new ByteArrayInputStream("mock audio data".getBytes());
        mockResult.setAudioStream(mockAudioStream);
        
        // Mock AmazonPolly to return the mocked result
        when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class))).thenReturn(mockResult);

        // Run the method under test
        String result = pollyService.convertTextToSpeech("Hello, Polly!", "test-file");

        // Verify the result (this assumes you only return the URL in your method)
        String expectedUrl = "https://dta-root.s3.amazonaws.com/dta-speech-translation-storage/test-file.mp3";
        assertEquals(expectedUrl, result);

        // Verify if the mock was called to upload the file to S3
        String s3FileName = "dta-speech-translation-storage/test-file.mp3";
        verify(amazonS3).putObject(argThat(putObjectRequest -> 
                putObjectRequest.getBucketName().equals("dta-root") &&
                putObjectRequest.getKey().equals(s3FileName)
        ));
    }
}
