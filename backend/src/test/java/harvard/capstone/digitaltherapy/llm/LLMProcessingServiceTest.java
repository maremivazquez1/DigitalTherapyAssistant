package harvard.capstone.digitaltherapy.llm;

import harvard.capstone.digitaltherapy.llm.service.BedrockService;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.llm.service.S3StorageService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LLMProcessingServiceTest {

    @Mock
    private BedrockService bedrockService;

    @InjectMocks
    private LLMProcessingService llmProcessingService;

    @Mock
    private S3StorageService s3Service;

    /**
     * Tests the constructor of LLMProcessingService to ensure proper initialization
     * with BedrockService and S3StorageService dependencies.
     */
    @Test
    public void testLLMProcessingServiceConstructor() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Verify that the LLMProcessingService is created with the mocked dependencies
        verify(bedrockService, times(0)).generateTextWithNovaLite(anyString());
        verify(s3Service, times(0)).readTextFromS3(anyString());

        // Additional assertions can be added here to verify the state of llmProcessingService if needed
    }

    @Test
    public void testProcessGeneratesAndStoresResponse() throws IOException {
        // Arrange
        String inputS3Path = "s3://dta-root/input.txt";
        String prompt = "Test prompt";
        String generatedResponse = "Generated response";
        String expectedOutputPath = "s3://dta-root/input-response.txt";
        String simplifiedOutputPath = "input-response.txt";  // Path used for writing

        // Mock the required behaviors
        when(s3Service.readTextFromS3(inputS3Path)).thenReturn(prompt);
        when(bedrockService.generateTextWithNovaLite(prompt)).thenReturn(generatedResponse);

        // Act
        String result = llmProcessingService.process(inputS3Path);

        // Assert
        assertEquals(expectedOutputPath, result);

        // Verify the interactions
        verify(s3Service).readTextFromS3(inputS3Path);
        verify(s3Service).writeTextToS3(simplifiedOutputPath, generatedResponse);  // Using simplified path
        verify(bedrockService).generateTextWithNovaLite(prompt);
    }



    /**
     * Tests the behavior of the process method when an IOException occurs while reading from S3.
     * This test verifies that the method properly propagates the IOException thrown by the S3 service.
     */
    @Test
    public void testProcessIOExceptionOnS3Read() throws IOException {
        String inputS3Path = "s3://dta-root/test-input.txt";
        when(s3Service.readTextFromS3(inputS3Path)).thenThrow(new IOException("S3 read error"));

        assertThrows(IOException.class, () -> llmProcessingService.process(inputS3Path));
    }
}
