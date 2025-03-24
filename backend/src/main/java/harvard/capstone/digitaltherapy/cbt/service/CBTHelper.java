package harvard.capstone.digitaltherapy.cbt.service;

import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
public class CBTHelper {
    private static final Logger logger = LoggerFactory.getLogger(CBTHelper.class);
    private final S3Utils s3Service;
    public CBTHelper(S3Utils s3Service) {
        this.s3Service = s3Service;
    }
    public ResponseEntity<StreamingResponseBody> downloadTextFile(@PathVariable String fileName) {
        try {
            // Validate filename
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Create streaming response
            StreamingResponseBody responseBody = outputStream -> {
                try {
                    s3Service.streamFileFromS3("dta-root", fileName, outputStream);
                } catch (IOException e) {
                    logger.error("Error streaming file: {}", e.getMessage());
                    throw new RuntimeException("Error streaming file", e);
                }
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(responseBody);

        } catch (Exception e) {
            logger.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public File convertMultiPartToTextFile(MultipartFile file) throws IOException {
        File convFile = Files.createTempFile("upload_", "_temp").toFile();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new FileWriter(convFile, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        }
        return convFile;
    }

    public File convertMultiPartToBinaryFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
