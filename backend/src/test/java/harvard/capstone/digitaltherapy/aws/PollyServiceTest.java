package harvard.capstone.digitaltherapy.aws;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PollyServiceTest {

    @Mock
    private AmazonPolly amazonPolly;

    @Mock
    private S3Utils s3Utils;

    private PollyService pollyService;

    @BeforeEach
    public void setUp() {
        pollyService = new PollyService(amazonPolly, s3Utils);
    }

    @Test
    public void testSynthesizeSpeech_Success() throws IOException {
        // Given
        String s3Url = "s3://test-bucket/path/to/text.txt";
        String fileName = "test-audio";
        String expectedText = "Hello, world!";
        String expectedS3Uri = "s3://dta-root/users/testuser/sessions/session123/audio_test-audio.mp3";

        // Create a temporary text file
        Path tempTextFile = Files.createTempFile("test-text-", ".txt");
        Files.write(tempTextFile, expectedText.getBytes());

        // Mock S3Utils behavior
        when(s3Utils.downloadFileFromS3(anyString(), anyString()))
                .thenReturn(tempTextFile.toFile());

        // Mock Polly behavior
        SynthesizeSpeechResult mockResult = mock(SynthesizeSpeechResult.class);
        when(mockResult.getAudioStream())
                .thenReturn(new ByteArrayInputStream("audio data".getBytes()));
        when(amazonPolly.synthesizeSpeech(any(SynthesizeSpeechRequest.class)))
                .thenReturn(mockResult);

        // Mock S3Utils upload
        when(s3Utils.uploadFile(anyString(), anyString()))
                .thenReturn(expectedS3Uri);

        // When
        String result = pollyService.synthesizeSpeech(s3Url, fileName);

        // Then
        assertEquals(expectedS3Uri, result);
        verify(s3Utils).downloadFileFromS3("test-bucket", "path/to/text.txt");
        verify(amazonPolly).synthesizeSpeech(any(SynthesizeSpeechRequest.class));
        verify(s3Utils).uploadFile(anyString(), eq("test-audio.mp3"));

        // Clean up
        Files.deleteIfExists(tempTextFile);
    }

    @Test
    public void testSynthesizeSpeech_IOException() throws IOException {
        // Given
        String s3Url = "s3://test-bucket/path/to/text.txt";
        String fileName = "test-audio";

        // Mock S3Utils to throw IOException
        when(s3Utils.downloadFileFromS3(anyString(), anyString()))
                .thenThrow(new IOException("Test error"));

        // When/Then
        assertThrows(RuntimeException.class, () ->
                pollyService.synthesizeSpeech(s3Url, fileName)
        );

        verify(s3Utils).downloadFileFromS3("test-bucket", "path/to/text.txt");
        verifyNoInteractions(amazonPolly);
    }
}
