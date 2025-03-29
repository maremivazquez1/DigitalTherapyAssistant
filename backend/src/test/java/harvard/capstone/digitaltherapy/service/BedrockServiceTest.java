package harvard.capstone.digitaltherapy.service;

import harvard.capstone.digitaltherapy.aws.service.BedrockService;
import harvard.capstone.digitaltherapy.aws.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BedrockServiceTest {

    private BedrockRuntimeClient bedrockRuntimeClient;
    private S3StorageService s3StorageService;
    private BedrockService bedrockService;

    @BeforeEach
    public void setUp() throws Exception {
        bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
        s3StorageService = mock(S3StorageService.class);
        bedrockService = new BedrockService(bedrockRuntimeClient, s3StorageService);

        // Inject systemPrompt via reflection
        var field = BedrockService.class.getDeclaredField("systemPrompt");
        field.setAccessible(true);
        field.set(bedrockService, "You are a helpful assistant.");
    }
}
