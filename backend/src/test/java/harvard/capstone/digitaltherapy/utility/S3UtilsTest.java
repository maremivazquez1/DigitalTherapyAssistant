package harvard.capstone.digitaltherapy.utility;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile("/path/to/file", "test.mp3")
        );
    }

    /**
     * Tests the uploadFile method when the S3 upload operation fails.
     * This test verifies that a RuntimeException is thrown when the S3 client
     * returns an unsuccessful response.
     */
    @Test
    void testUploadFile_S3UploadFails() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(null);
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        assertThrows(RuntimeException.class, () ->
                s3Utils.uploadFile("/path/to/file", "test.mp3")
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

    @Test
    void testUploadFile_UserSpecificPrefix() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadFile("/path/to/file", "test.mp3");

        // Verify the key contains user-specific prefix
        assertTrue(result.contains("users/testuser/sessions/"));
        assertTrue(result.contains("audio_"));
        assertTrue(result.endsWith(".mp3"));
    }

    @Test
    void testUploadFile_SystemUserWhenNoAuth() {
        // Clear security context
        SecurityContextHolder.clearContext();

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadFile("/path/to/file", "test.mp3");

        // Verify the key contains system user prefix
        assertTrue(result.contains("users/system/sessions/"));
        assertTrue(result.contains("audio_"));
        assertTrue(result.endsWith(".mp3"));
    }

    @Test
    void testUploadAudioFile() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadAudioFile("/path/to/audio.mp3");

        // Verify the key contains user-specific prefix and audio type
        assertTrue(result.contains("users/testuser/sessions/"));
        assertTrue(result.contains("audio_"));
        assertTrue(result.endsWith(".mp3"));
    }

    @Test
    void testUploadVideoFile() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadVideoFile("/path/to/video.mp4");

        // Verify the key contains user-specific prefix and video type
        assertTrue(result.contains("users/testuser/sessions/"));
        assertTrue(result.contains("video_"));
        assertTrue(result.endsWith(".mp4"));
    }

    @Test
    void testUploadTextFile() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadTextFile("/path/to/text.txt");

        // Verify the key contains user-specific prefix and text type
        assertTrue(result.contains("users/testuser/sessions/"));
        assertTrue(result.contains("text_"));
        assertTrue(result.endsWith(".txt"));
    }

    @Test
    void testUploadCustomFile() {
        // Setup security context
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        S3Utils s3Utils = new S3Utils("testBucket", "us-west-2");
        s3Utils.s3Client = mockS3Client;

        // Mock successful upload
        PutObjectResponse mockResponse = Mockito.mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse()).thenReturn(Mockito.mock(software.amazon.awssdk.http.SdkHttpResponse.class));
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // Perform upload
        String result = s3Utils.uploadCustomFile("/path/to/custom.json", "custom", "json");

        // Verify the key contains user-specific prefix and custom type
        assertTrue(result.contains("users/testuser/sessions/"));
        assertTrue(result.contains("custom_"));
        assertTrue(result.endsWith(".json"));
    }
}
