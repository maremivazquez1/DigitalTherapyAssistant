package harvard.capstone.digitaltherapy.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.net.URL;

@Service
public class S3Utils {
    private static final Logger logger = LoggerFactory.getLogger(S3Utils.class);
    S3Client s3Client;
    private final String bucketName;

    public S3Utils(@Value("${aws.s3.bucketName}") String bucketName,
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
                        .apiCallTimeout(Duration.ofMinutes(30))
                        .apiCallAttemptTimeout(Duration.ofMinutes(20)))
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
                String s3Uri = String.format("s3://%s/%s", bucketName, keyName);
                return s3Uri;
            } else {
                throw new RuntimeException("Failed to upload file to S3");
            }
        } catch (S3Exception e) {
            logger.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    // Method to retrieve file from S3 and save it locally
    public File downloadFileFromS3(String bucketName, String fileKey) throws IOException {
        // Build the GetObjectRequest
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)    // The S3 bucket name
                .key(fileKey)          // The file's key (path in the bucket)
                .build();

        // Retrieve the file from S3
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

        // Create a temporary file to store the file locally
        Path tempFilePath = Files.createTempFile("downloaded-file-", ".tmp");
        Files.copy(s3Object, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Convert the Path to a File
        return tempFilePath.toFile();
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



        public void streamFileFromS3(String bucketName, String fileKey, OutputStream outputStream)
                throws IOException {
            try {
                // First check if file exists
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build();

                try {
                    s3Client.headObject(headRequest);
                } catch (S3Exception e) {
                    if (e.statusCode() == 404) {
                        throw new FileNotFoundException("File not found in S3: " + fileKey);
                    }
                    throw new IOException("Error checking file in S3: " + e.getMessage(), e);
                }

                // Get the object
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build();

                try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = s3Object.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            } catch (S3Exception e) {
                throw new IOException("Error streaming file from S3: " + e.getMessage(), e);
            }
        }

        public static String generatePresignedUrl(String bucket, String key, Duration duration) {
            try (S3Presigner presigner = S3Presigner.create()) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(getObjectRequest)
                        .build();

                URL signedUrl = presigner.presignGetObject(presignRequest).url();
                return signedUrl.toString();
            }
        }
    }

