package harvard.capstone.digitaltherapy.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
public class S3FileUploadUtil {
    private static final Logger logger = LoggerFactory.getLogger(S3FileUploadUtil.class);
    private final S3Client s3Client;
    private final String bucketName;

    public S3FileUploadUtil(@Value("${aws.s3.bucketName}") String bucketName,
                           @Value("${aws.region}") String region) {
        this.bucketName = bucketName;
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .numRetries(3)
                .retryCondition(RetryCondition.defaultRetryCondition())
                .backoffStrategy(BackoffStrategy.defaultStrategy())
                .build();

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(config -> config
                        .retryPolicy(retryPolicy)
                        .apiCallTimeout(Duration.ofMinutes(5))
                        .apiCallAttemptTimeout(Duration.ofMinutes(2)))
                .build();
    }

    public String uploadFile(String filePath, String keyName) {
        try {
            // Check if bucket exists
            if (!doesBucketExist(bucketName)) {
                createBucket(bucketName);
            }
            Path path = Paths.get(filePath);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromFile(path));

            if (response != null && response.sdkHttpResponse().isSuccessful()) {
                logger.info("File successfully uploaded to S3: {}", keyName);
                return "File uploaded to S3: " + keyName;
            } else {
                throw new RuntimeException("Failed to upload file to S3");
            }
        } catch (S3Exception e) {
            logger.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    private boolean doesBucketExist(String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private void createBucket(String bucketName) {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .locationConstraint(s3Client.serviceClientConfiguration().region().toString())
                                    .build()
                    )
                    .build();

            s3Client.createBucket(createBucketRequest);
            logger.info("Created bucket: {}", bucketName);

            // Wait for the bucket to be created
            s3Client.waiter().waitUntilBucketExists(
                    HeadBucketRequest.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (S3Exception e) {
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                logger.info("Bucket {} already exists and is owned by you", bucketName);
            } else {
                logger.error("Error creating bucket: {}", e.getMessage());
                throw new RuntimeException("Failed to create bucket: " + e.getMessage());
            }
        }
    }
}
