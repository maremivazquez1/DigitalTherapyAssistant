package harvard.capstone.digitaltherapy.llm;

import harvard.capstone.digitaltherapy.llm.service.S3StorageService;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3StorageServiceTest {

    @Mock
    private S3Utils s3Utils;

    private S3StorageService s3StorageService;

    @BeforeEach
    public void setUp() {
        s3StorageService = new S3StorageService(s3Utils);
    }

    @Test
    public void testReadTextFromS3_Success() throws IOException {
        // Setup
        String expectedContent = "This is test content from S3";
        String s3Path = "s3://test-bucket/path/to/file.txt";

        // Mock the S3Utils behavior
        doAnswer(invocation -> {
            ByteArrayOutputStream outputStream = invocation.getArgument(2);
            outputStream.write(expectedContent.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(s3Utils).streamFileFromS3(eq("test-bucket"), eq("path/to/file.txt"), any(ByteArrayOutputStream.class));

        // Execute
        String result = s3StorageService.readTextFromS3(s3Path);

        // Verify
        assertEquals(expectedContent, result);
        verify(s3Utils).streamFileFromS3(eq("test-bucket"), eq("path/to/file.txt"), any(ByteArrayOutputStream.class));
    }

    @Test
    public void testReadTextFromS3_EmptyFile() throws IOException {
        // Setup
        String s3Path = "s3://test-bucket/path/to/empty-file.txt";

        // Mock the S3Utils behavior - do nothing to simulate empty file
        doAnswer(invocation -> null)
                .when(s3Utils).streamFileFromS3(eq("test-bucket"), eq("path/to/empty-file.txt"), any(ByteArrayOutputStream.class));

        // Execute
        String result = s3StorageService.readTextFromS3(s3Path);

        // Verify
        assertEquals("", result);
        verify(s3Utils).streamFileFromS3(eq("test-bucket"), eq("path/to/empty-file.txt"), any(ByteArrayOutputStream.class));
    }

    @Test
    public void testReadTextFromS3_IOExceptionHandling() throws IOException {
        // Setup
        String s3Path = "s3://test-bucket/path/to/error-file.txt";

        // Mock the S3Utils behavior to throw IOException
        doThrow(new IOException("Simulated S3 read error"))
                .when(s3Utils).streamFileFromS3(anyString(), anyString(), any(ByteArrayOutputStream.class));

        // Execute and verify exception is propagated
        assertThrows(IOException.class, () -> s3StorageService.readTextFromS3(s3Path));
        verify(s3Utils).streamFileFromS3(eq("test-bucket"), eq("path/to/error-file.txt"), any(ByteArrayOutputStream.class));
    }

    @Test
    public void testReadTextFromS3_InvalidPathFormat() {
        // Test with invalid path formats
        String[] invalidPaths = {
                "invalid-path",       // No s3:// prefix
                "s3:/missing-slash",  // Missing a slash
                "s3://only-bucket"    // Missing key part
        };

        for (String path : invalidPaths) {
            assertThrows(IllegalArgumentException.class, () -> s3StorageService.readTextFromS3(path));
        }
    }

    @Test
    public void testWriteTextToS3_Success() throws IOException {
        // Setup
        String content = "Content to write to S3";
        String s3Path = "s3://test-bucket/path/to/output-file.txt";
        String expectedS3Uri = "s3://test-bucket/path/to/output-file.txt";

        // Mock the S3Utils behavior - return the S3 URI
        when(s3Utils.uploadFile(anyString(), anyString())).thenReturn(expectedS3Uri);

        // Execute
        s3StorageService.writeTextToS3(s3Path, content);

        // Verify
        ArgumentCaptor<String> filePathCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Utils).uploadFile(filePathCaptor.capture(), eq(s3Path));

        // Verify temp file was created (but would be deleted by the method)
        String capturedFilePath = filePathCaptor.getValue();
        assertTrue(capturedFilePath.startsWith(System.getProperty("java.io.tmpdir")));
        assertTrue(capturedFilePath.contains("llm-output"));
        assertTrue(capturedFilePath.endsWith(".txt"));

        // Verify the file doesn't exist anymore (should be deleted)
        assertFalse(new File(capturedFilePath).exists());
    }

    @Test
    public void testWriteTextToS3_RuntimeExceptionHandling() throws IOException {
        // Setup
        String content = "Content that will fail to upload";
        String s3Path = "s3://test-bucket/path/to/error-output.txt";

        // Mock the S3Utils behavior to throw RuntimeException during upload
        when(s3Utils.uploadFile(anyString(), eq(s3Path)))
                .thenThrow(new RuntimeException("Simulated S3 write error"));

        // Execute and verify exception is propagated
        assertThrows(RuntimeException.class, () -> s3StorageService.writeTextToS3(s3Path, content));
    }

    @Test
    public void testExtractBucketName_ValidFormat() throws Exception {
        // Test different valid S3 paths
        String[] testPaths = {
                "s3://bucket-name/path/file.txt",
                "s3://my-bucket/path/to/some/deep/file.json",
                "s3://test-bucket-123/file.csv"
        };

        String[] expectedBuckets = {
                "bucket-name",
                "my-bucket",
                "test-bucket-123"
        };

        for (int i = 0; i < testPaths.length; i++) {
            // Use reflection to access private method
            String extractedBucket = invokeExtractBucketName(testPaths[i]);
            assertEquals(expectedBuckets[i], extractedBucket);
        }
    }

    @Test
    public void testExtractKey_ValidFormat() throws Exception {
        // Test different valid S3 paths
        String[] testPaths = {
                "s3://bucket-name/path/file.txt",
                "s3://my-bucket/path/to/some/deep/file.json",
                "s3://test-bucket-123/file.csv"
        };

        String[] expectedKeys = {
                "path/file.txt",
                "path/to/some/deep/file.json",
                "file.csv"
        };

        for (int i = 0; i < testPaths.length; i++) {
            // Use reflection to access private method
            String extractedKey = invokeExtractKey(testPaths[i]);
            assertEquals(expectedKeys[i], extractedKey);
        }
    }

    @Test
    public void testExtractBucketName_NoKeyPath() {
        // Test a path with no key part (just a bucket)
        String invalidPath = "s3://only-bucket"; // Missing key part

        // Changed from IndexOutOfBoundsException to IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> invokeExtractBucketName(invalidPath));
    }

    // Helper method to invoke private extractBucketName method via reflection
    private String invokeExtractBucketName(String s3Path) throws Exception {
        java.lang.reflect.Method method = S3StorageService.class.getDeclaredMethod("extractBucketName", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(s3StorageService, s3Path);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // This is crucial - we need to unwrap the InvocationTargetException
            // to get the actual exception thrown by the method
            if (e.getCause() != null) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    // Helper method to invoke private extractKey method via reflection
    private String invokeExtractKey(String s3Path) throws Exception {
        java.lang.reflect.Method method = S3StorageService.class.getDeclaredMethod("extractKey", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(s3StorageService, s3Path);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() != null) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Test
    public void testWriteTextToS3_ContentVerification() throws IOException {
        // Setup
        String content = "Test content for verification";
        String s3Path = "s3://test-bucket/path/to/verify-file.txt";

        // Mock to capture the temporary file before it's deleted
        when(s3Utils.uploadFile(anyString(), eq(s3Path))).thenAnswer(invocation -> {
            String tempFilePath = invocation.getArgument(0);

            // Read the content of the file before it gets deleted
            String fileContent = new String(Files.readAllBytes(new File(tempFilePath).toPath()), StandardCharsets.UTF_8);

            // Verify the content matches what was supposed to be written
            assertEquals(content, fileContent);

            return s3Path;
        });

        // Execute
        s3StorageService.writeTextToS3(s3Path, content);

        // Verify the method was called
        verify(s3Utils).uploadFile(anyString(), eq(s3Path));
    }
}