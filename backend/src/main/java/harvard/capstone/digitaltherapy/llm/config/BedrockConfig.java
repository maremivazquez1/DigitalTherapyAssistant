package harvard.capstone.digitaltherapy.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        // The AWS SDK will automatically use credentials from ~/.aws/credentials
        System.out.println("Creating BedrockRuntimeClient with region: " + region);

        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .build();
    }
}
