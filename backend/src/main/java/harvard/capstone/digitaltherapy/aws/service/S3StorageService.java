package harvard.capstone.digitaltherapy.aws.service;

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

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class S3StorageService {

    private final S3Utils s3Utils;

    @Autowired
    public S3StorageService(S3Utils s3Utils) {
        this.s3Utils = s3Utils;
    }

    public String readTextFromS3(String s3Path) throws IOException {
        String bucket = extractBucketName(s3Path);
        String key = extractKey(s3Path);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        s3Utils.streamFileFromS3(bucket, key, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String extractBucketName(String s3Path) {
        // Example: s3://bucket-name/path/to/file.txt -> bucket-name
        String withoutPrefix = s3Path.replace("s3://", "");
        return withoutPrefix.substring(0, withoutPrefix.indexOf('/'));
    }

    private String extractKey(String s3Path) {
        // Example: s3://bucket-name/path/to/file.txt -> path/to/file.txt
        String withoutPrefix = s3Path.replace("s3://", "");
        return withoutPrefix.substring(withoutPrefix.indexOf('/') + 1);
    }

    public void writeTextToS3(String s3Path, String content) throws IOException {
        File tempFile = File.createTempFile("llm-output", ".txt");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
        s3Utils.uploadFile(tempFile.getAbsolutePath(), s3Path);
        tempFile.delete();
    }
}
