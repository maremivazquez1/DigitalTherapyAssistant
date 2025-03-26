package harvard.capstone.digitaltherapy.aws.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Bean
    public AmazonPolly amazonPolly() {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAXOFMNMSEUS4FV5XU", "0ZkJuDrMXSz/CkStynIPUb6pJSxbIDWPVU2rPF4u");
        return AmazonPollyClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    public AmazonTranscribe amazonTranscribe() {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAXOFMNMSEUS4FV5XU", "0ZkJuDrMXSz/CkStynIPUb6pJSxbIDWPVU2rPF4u");
        return AmazonTranscribeClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    public AmazonS3 amazonS3() {
        AWSCredentials credentials = new BasicAWSCredentials("AKIAXOFMNMSEUS4FV5XU", "0ZkJuDrMXSz/CkStynIPUb6pJSxbIDWPVU2rPF4u");
        return AmazonS3Client.builder()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
    }
}
