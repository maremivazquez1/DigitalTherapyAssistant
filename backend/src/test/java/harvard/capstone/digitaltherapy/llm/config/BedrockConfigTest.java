package harvard.capstone.digitaltherapy.llm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BedrockConfig.class)
class BedrockConfigTest {

    @Autowired
    private BedrockConfig bedrockConfig;

    @Test
    void testBedrockRuntimeClientCreation() {
        // Test that the client is created and is not null
        BedrockRuntimeClient client = bedrockConfig.bedrockRuntimeClient();
        assertNotNull(client, "BedrockRuntimeClient should not be null");
    }

    @Test
    void testBedrockRuntimeClientWithCustomRegion() {
        // We need to create a new configuration instance since we can't easily override
        // the property in an existing bean
        BedrockConfig customConfig = new BedrockConfig();

        // Set a custom region using reflection
        try {
            java.lang.reflect.Field regionField = BedrockConfig.class.getDeclaredField("region");
            regionField.setAccessible(true);
            regionField.set(customConfig, "us-west-2");
        } catch (Exception e) {
            fail("Failed to set region field: " + e.getMessage());
        }

        // Get the client and verify it exists
        BedrockRuntimeClient client = customConfig.bedrockRuntimeClient();
        assertNotNull(client, "BedrockRuntimeClient should not be null with custom region");

        // Note: We can't directly test the region of the built client as it's not exposed
        // in a way that's easily testable without mocking the AWS SDK
    }
}
