package harvard.capstone.digitaltherapy.llm;

import harvard.capstone.digitaltherapy.llm.service.BedrockService;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.llm.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class LLMProcessingServiceTest {

    private BedrockService bedrockService;
    private S3StorageService s3Service;
    private LLMProcessingService llmProcessingService;

    @BeforeEach
    public void setUp() {
        bedrockService = mock(BedrockService.class);
        s3Service = mock(S3StorageService.class);
        llmProcessingService = new LLMProcessingService(bedrockService, s3Service);
    }

    @Test
    public void testProcessReturnsExpectedOutputPath() throws IOException {
        String inputS3Path = "s3://dta-root/session/input.txt";
        String prompt = "What is CBT?";
        String generatedResponse = "CBT stands for Cognitive Behavioral Therapy.";
        String expectedOutputPath = "s3://dta-root/session/input-response.txt";

        when(s3Service.readTextFromS3(inputS3Path)).thenReturn(prompt);
        when(bedrockService.generateTextWithNovaLite(prompt)).thenReturn(generatedResponse);

        String result = llmProcessingService.process(inputS3Path);

        assertEquals(expectedOutputPath, result);
        verify(s3Service).readTextFromS3(inputS3Path);
        verify(bedrockService).generateTextWithNovaLite(prompt);
        verify(s3Service).writeTextToS3("session/input-response.txt", generatedResponse);
    }

    @Test
    public void testGenerateOutputPathWithExtension() throws IOException {
        String inputPath = "s3://dta-root/session/data.txt";
        String result = llmProcessingService.process(inputPath);
        assertEquals("s3://dta-root/session/data-response.txt", result);
    }

    @Test
    public void testGenerateOutputPathWithoutExtension() throws IOException {
        String inputPath = "s3://dta-root/session/data";
        String result = llmProcessingService.process(inputPath);
        assertEquals("s3://dta-root/session/data-response.txt", result);
    }
}
