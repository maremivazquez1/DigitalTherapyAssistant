package harvard.capstone.digitaltherapy.llm.service;

/**
 * S3StorageService provides utility methods for reading and writing plain text content
 * to and from AWS S3 buckets. It abstracts low-level S3 interactions using S3Utils.
 *
 * It supports reading UTF-8 encoded text from S3 paths and writing generated content
 * (e.g., LLM responses) back to S3 using temporary files.
 *
 * This service is intended to be used by higher-level services that orchestrate
 * workflows involving S3, such as LLMProcessingService.
 */

import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class S3StorageService {
    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);
    private final S3Utils s3Utils;

    @Autowired
    public S3StorageService(S3Utils s3Utils) {
        this.s3Utils = s3Utils;
    }

    /**
     * Reads text content from an S3 path.
     *
     * @param s3Path S3 path in the format s3://bucket-name/path/to/file.txt
     * @return The text content of the file as a UTF-8 encoded string
     * @throws IOException If there is an error reading the file
     * @throws IllegalArgumentException If the S3 path format is invalid
     */
    public String readTextFromS3(String s3Path) throws IOException {
        validateS3Path(s3Path);
        String bucket = extractBucketName(s3Path);
        String key = extractKey(s3Path);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            s3Utils.streamFileFromS3(bucket, key, outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error reading text from S3: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates that an S3 path has the correct format.
     *
     * @param s3Path The S3 path to validate
     * @throws IllegalArgumentException If the path is not in the format s3://bucket-name/path/to/file
     */
    private void validateS3Path(String s3Path) {
        if (s3Path == null || !s3Path.startsWith("s3://") || !s3Path.contains("/") || s3Path.length() < 6) {
            throw new IllegalArgumentException(
                    "Invalid S3 path format. Expected: s3://bucket-name/path/to/file, Got: " + s3Path);
        }

        // Check if there's a slash after bucket name
        String withoutPrefix = s3Path.substring(5); // Remove "s3://"
        if (!withoutPrefix.contains("/")) {
            throw new IllegalArgumentException(
                    "Invalid S3 path format. Missing key part after bucket name. Expected: s3://bucket-name/path/to/file");
        }
    }

    /**
     * Extracts the bucket name from an S3 path.
     *
     * @param s3Path S3 path in the format s3://bucket-name/path/to/file.txt
     * @return The bucket name
     */
    private String extractBucketName(String s3Path) {
        // Example: s3://bucket-name/path/to/file.txt -> bucket-name
        String withoutPrefix = s3Path.replace("s3://", "");
        int slashIndex = withoutPrefix.indexOf('/');
        if (slashIndex < 0) {
            throw new IllegalArgumentException(
                    "Invalid S3 path format. Missing key part after bucket name: " + s3Path);
        }
        return withoutPrefix.substring(0, slashIndex);
    }

    /**
     * Extracts the key (path inside bucket) from an S3 path.
     *
     * @param s3Path S3 path in the format s3://bucket-name/path/to/file.txt
     * @return The key (path inside bucket)
     */
    private String extractKey(String s3Path) {
        // Example: s3://bucket-name/path/to/file.txt -> path/to/file.txt
        String withoutPrefix = s3Path.replace("s3://", "");
        int slashIndex = withoutPrefix.indexOf('/');
        if (slashIndex < 0 || slashIndex == withoutPrefix.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid S3 path format. Missing or empty key part after bucket name: " + s3Path);
        }
        return withoutPrefix.substring(slashIndex + 1);
    }

    /**
     * Writes text content to an S3 path.
     *
     * @param s3Path S3 path in the format s3://bucket-name/path/to/file.txt
     * @param content The text content to write
     * @throws IOException If there is an error writing the file
     * @throws IllegalArgumentException If the S3 path format is invalid
     */
    public void writeTextToS3(String s3Path, String content) throws IOException {
        //validateS3Path(s3Path);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("llm-output", ".txt");

            try (Writer writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
                writer.write(content);
            }

            s3Utils.uploadFile(tempFile.getAbsolutePath(), s3Path);

        } catch (RuntimeException e) {
            logger.error("Error writing text to S3: {}", e.getMessage());
            throw e;
        } finally {
            // Make sure we always try to delete the temp file
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }
}