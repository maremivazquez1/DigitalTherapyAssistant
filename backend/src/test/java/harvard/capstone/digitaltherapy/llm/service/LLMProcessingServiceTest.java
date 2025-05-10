package harvard.capstone.digitaltherapy.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LLMProcessingServiceTest {

    @Mock
    private BedrockService bedrockService;

    @Mock
    private S3StorageService s3Service;

    @InjectMocks
    private LLMProcessingService llmProcessingService;

    private final String TEST_INPUT_PATH = "s3://dta-root/test/input.txt";
    private final String TEST_PROMPT = "This is a test prompt";
    private final String TEST_RESPONSE = "This is a test response";

    @Test
    public void testProcess() throws IOException {
        // Arrange
        when(s3Service.readTextFromS3(anyString())).thenReturn(TEST_PROMPT);
        when(bedrockService.generateTextWithNovaLite(TEST_PROMPT)).thenReturn(TEST_RESPONSE);

        // Act
        String result = llmProcessingService.process(TEST_INPUT_PATH);

        // Assert
        assertEquals("s3://dta-root/test/input-response.txt", result);
        verify(s3Service).readTextFromS3(TEST_INPUT_PATH);
        verify(bedrockService).generateTextWithNovaLite(TEST_PROMPT);
        verify(s3Service).writeTextToS3("test/input-response.txt", TEST_RESPONSE);
    }

    @Test
    public void testProcessWithNoExtension() throws IOException {
        // Arrange
        String inputPath = "s3://dta-root/test/input";
        when(s3Service.readTextFromS3(anyString())).thenReturn(TEST_PROMPT);
        when(bedrockService.generateTextWithNovaLite(TEST_PROMPT)).thenReturn(TEST_RESPONSE);

        // Act
        String result = llmProcessingService.process(inputPath);

        // Assert
        assertEquals("s3://dta-root/test/input-response.txt", result);
        verify(s3Service).readTextFromS3(inputPath);
        verify(s3Service).writeTextToS3("test/input-response.txt", TEST_RESPONSE);
    }

    @Test
    public void testProcessHandlesExceptions() throws IOException {
        // Arrange
        IOException exception = new IOException("Test error");
        when(s3Service.readTextFromS3(anyString())).thenThrow(exception);

        // Act & Assert
        try {
            llmProcessingService.process(TEST_INPUT_PATH);
        } catch (IOException e) {
            assertEquals(exception, e);
        }

        verify(s3Service).readTextFromS3(TEST_INPUT_PATH);
        verifyNoInteractions(bedrockService);
        verify(s3Service, never()).writeTextToS3(anyString(), anyString());
    }
}
