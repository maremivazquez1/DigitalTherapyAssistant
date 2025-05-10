package harvard.capstone.digitaltherapy.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.http.SdkHttpResponse;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.BinaryMessage;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class S3UtilsTest {

    @Mock
    private S3Client mockS3Client;

    @Mock
    private S3Waiter mockS3Waiter;

    @TempDir
    Path tempDir;

    private S3Utils s3Utils;

    private final String bucketName = "testBucket";
    private final String region = "us-west-2";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Utils = new S3Utils(bucketName, region);
        s3Utils.s3Client = mockS3Client;

        // Mock head bucket to indicate bucket exists
        when(mockS3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        // Mock S3 waiter
        when(mockS3Client.waiter()).thenReturn(mockS3Waiter);
    }

    /**
     * Tests the uploadFile method when an S3Exception is thrown during the upload process.
     * This test verifies that the method catches the S3Exception and rethrows it as a RuntimeException.
     */
    @Test
    void testUploadFile_S3ExceptionThrown() {
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
        // Mock SdkHttpResponse with an error status code
        SdkHttpResponse mockSdkResponse = SdkHttpResponse.builder()
                .statusCode(500)
                .build();

        // Create mock PutObjectResponse with the error SdkHttpResponse
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(mockSdkResponse);

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile("/path/to/file", "testKey")
        );
    }

    @Test
    void test_downloadFileFromS3_S3Exception() {
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

    /**
     * Tests the uploadFile(InputStream, String, String) method with successful upload.
     * This test verifies that the method correctly uploads a file from an InputStream
     * and returns the expected S3 URI.
     */
    @Test
    void testUploadFileInputStream_Success() throws IOException {
        // Test data
        String keyName = "test/file.txt";
        String contentType = "text/plain";
        String fileContent = "This is a test file";
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        // Mock responses
        // Mock SdkHttpResponse
        SdkHttpResponse mockSdkResponse = SdkHttpResponse.builder()
                .statusCode(200)
                .build();

        // Create SdkResponse with the SdkHttpResponse
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(mockSdkResponse);

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Execute method
        String result = s3Utils.uploadFile(inputStream, keyName, contentType);

        // Verify result
        assertEquals("s3://" + bucketName + "/" + keyName, result);

        // Verify the method called S3Client with correct parameters
        verify(mockS3Client).putObject(
                argThat((PutObjectRequest request) ->
                        request.bucket().equals(bucketName) &&
                                request.key().equals(keyName) &&
                                request.contentType().equals(contentType)
                ),
                any(RequestBody.class)
        );
    }


    /**
     * Tests the uploadFile(InputStream, String, String) method when an IOException occurs.
     * This test verifies that the method catches the IOException and rethrows it as a RuntimeException.
     */
    @Test
    void testUploadFileInputStream_IOException() throws IOException {
        // Test data
        String keyName = "test/file.txt";
        String contentType = "text/plain";

        // Create a mock InputStream that throws IOException
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        when(mockInputStream.readAllBytes()).thenThrow(new IOException("Test IO error"));

        // Execute method and verify exception
        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile(mockInputStream, keyName, contentType)
        );
    }

    /**
     * Tests the downloadFileAsString method with successful download.
     * This test verifies that the method correctly downloads a file and returns its content as a String.
     */
    @Test
    void testDownloadFileAsString_Success() {
        // Test data
        String keyName = "test/file.txt";
        String fileContent = "This is a test file content";

        // Create mock response
        GetObjectResponse mockGetResponse = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> responseStream =
                new ResponseInputStream<>(mockGetResponse,
                        AbortableInputStream.create(
                                new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8))
                        ));

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // Execute method
        String result = s3Utils.downloadFileAsString(keyName);

        // Verify result
        assertEquals(fileContent, result);

        // Verify the method called S3Client with correct parameters
        verify(mockS3Client).getObject(
                argThat((GetObjectRequest request) ->
                        request.bucket().equals(bucketName) &&
                                request.key().equals(keyName)
                )
        );
    }

    /**
     * Tests the downloadFileAsString method when an S3Exception is thrown.
     * This test verifies that the method catches the S3Exception and rethrows it as a RuntimeException.
     */
    @Test
    void testDownloadFileAsString_S3Exception() {
        // Test data
        String keyName = "test/file.txt";

        // Mock the behavior to throw S3Exception
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("File not found")
                        .build());

        // Execute method and verify exception
        assertThrows(RuntimeException.class, () ->
                s3Utils.downloadFileAsString(keyName)
        );
    }

    /**
     * Tests the downloadFileAsString method when an IOException occurs during reading.
     * This test verifies that the method catches the IOException and rethrows it as a RuntimeException.
     */
    @Test
    void testDownloadFileAsString_IOException() {
        // Test data
        String keyName = "test/file.txt";

        // Create mock response with a stream that will throw IOException when read
        GetObjectResponse mockGetResponse = GetObjectResponse.builder().build();
        InputStream mockStream = Mockito.mock(InputStream.class);
        try {
            when(mockStream.read(any(byte[].class))).thenThrow(new IOException("Test IO error"));
        } catch (IOException e) {
            fail("Exception during test setup: " + e.getMessage());
        }

        ResponseInputStream<GetObjectResponse> responseStream =
                new ResponseInputStream<>(mockGetResponse,
                        AbortableInputStream.create(mockStream));

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // Execute method and verify exception
        assertThrows(RuntimeException.class, () ->
                s3Utils.downloadFileAsString(keyName)
        );
    }

    @Test
    void uploadFile_Success() {
        // Create a temporary file
        File tempFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("test content");
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        PutObjectResponse putObjectResponse = (PutObjectResponse) PutObjectResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                        .statusCode(200)
                        .build())
                .build();
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);
        when(mockS3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        String result = s3Utils.uploadFile(tempFile.getAbsolutePath(), "test.txt");
        assertEquals("s3://testBucket/test.txt", result);
    }

    @Test
    void uploadFile_BucketDoesNotExist() {
        // Mock bucket doesn't exist
        when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        // Mock successful bucket creation
        when(mockS3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        PutObjectResponse putObjectResponse = (PutObjectResponse) PutObjectResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                        .statusCode(200)
                        .build())
                .build();
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

        File tempFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("test content");
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        String result = s3Utils.uploadFile(tempFile.getAbsolutePath(), "test.txt");
        assertEquals("s3://testBucket/test.txt", result);
    }

    @Test
    void downloadFileFromS3_Success() throws IOException {
        // Mock successful response
        ResponseInputStream<GetObjectResponse> mockResponse = mock(ResponseInputStream.class);
        when(mockResponse.read(any(byte[].class)))
                .thenReturn(10) // Return some bytes first
                .thenReturn(-1); // Then signal end of stream
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockResponse);

        File result = s3Utils.downloadFileFromS3("test-bucket", "test.txt");
        assertTrue(result.exists());
        assertFalse(result.length() > 0);
    }

    @Test
    void uploadAudioBinaryFile_Success() {
        // Create test audio data
        byte[] audioData = "test audio data".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(audioData);
        BinaryMessage message = new BinaryMessage(buffer);

// Mock successful response
        PutObjectResponse putObjectResponse = (PutObjectResponse) PutObjectResponse.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                        .statusCode(200)
                        .build())
                .build();

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);
        when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());

        String result = s3Utils.uploadAudioBinaryFile(message, "audio_test.mp3");
        assertEquals("s3://testBucket/audio_test.mp3", result);
    }

    @Test
    void downloadFileAsString_Success() {
        String expectedContent = "test content";
        byte[] contentBytes = expectedContent.getBytes();

        // Create a ByteArrayInputStream with the test content
        ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);

        // Create a custom ResponseInputStream implementation
        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                GetObjectResponse.builder()
                        .contentLength((long) contentBytes.length)
                        .build(),
                inputStream
        );

        // Mock the S3Client to return our ResponseInputStream
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        // Test the method
        String result = s3Utils.downloadFileAsString("test.txt");

        // Verify the result
        assertEquals(expectedContent, result);
    }


    @Test
    void generatePresignedUrl_Success() {
        String result = S3Utils.generatePresignedUrl("test-bucket", "test.txt", Duration.ofMinutes(5));
        assertNotNull(result);
    }

    @Test
    void uploadFile_Failure() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Upload failed").build());

        File tempFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("test content");
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile(tempFile.getAbsolutePath(), "test.txt"));
    }
}