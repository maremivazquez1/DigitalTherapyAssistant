package harvard.capstone.digitaltherapy.aws.config;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for AwsConfig class
 *
 * This test verifies that all AWS client beans are correctly configured and can be instantiated.
 * It uses a Spring Boot test context with AWS credentials from the environment.
 */
@SpringBootTest(classes = AwsConfig.class)
@TestPropertySource(properties = {
        "aws.accessKeyId=${AWS_ACCESS_KEY_ID:test-key}",
        "aws.secretKey=${AWS_SECRET_ACCESS_KEY:test-secret}"
})
public class AwsConfigTest {

    @Autowired
    private AwsConfig awsConfig;

    @Test
    public void testAmazonPollyBeanCreation() {
        AmazonPolly pollyClient = awsConfig.amazonPolly();
        assertNotNull(pollyClient, "Amazon Polly client should not be null");
    }

    @Test
    public void testAmazonTranscribeBeanCreation() {
        AmazonTranscribe transcribeClient = awsConfig.amazonTranscribe();
        assertNotNull(transcribeClient, "Amazon Transcribe client should not be null");
    }

    @Test
    public void testAmazonS3BeanCreation() {
        AmazonS3 s3Client = awsConfig.amazonS3();
        assertNotNull(s3Client, "Amazon S3 client should not be null");
    }

    @Test
    public void testRekognitionClientBeanCreation() {
        RekognitionClient rekognitionClient = awsConfig.rekognitionClient();
        assertNotNull(rekognitionClient, "Amazon Rekognition client should not be null");
    }
}
