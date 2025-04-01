package harvard.capstone.digitaltherapy.utility;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class S3UtilsTest {

    /**
     * Tests the uploadFile method when an S3Exception is thrown during the upload process.
     * This test verifies that the method catches the S3Exception and rethrows it as a RuntimeException.
     */
    @Test
    void testUploadFile_S3ExceptionThrown() {
        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile("/path/to/file", "testKey")
        );
    }

    /**
     * Tests the uploadFile method when the S3 upload operation fails.
     * This test verifies that a RuntimeException is thrown when the S3 client
     * returns an unsuccessful response.
     */
    @Test
    void testUploadFile_S3UploadFails() {
        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(null);
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile("/path/to/file", "testKey")
        );
    }

    @Test
    void test_downloadFileFromS3_S3Exception() {
        // Mock S3Client
        S3Client mockS3Client = Mockito.mock(S3Client.class);

        // Set up the S3Utils instance with the mock client
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock the behavior to throw S3Exception
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("File not found")
                        .statusCode(404)
                        .build());

        // Assert that S3Exception is thrown
        assertThrows(S3Exception.class, () ->
                s3Utils.downloadFileFromS3("testBucket", "testKey")
        );
    }



}
