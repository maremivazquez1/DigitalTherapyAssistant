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
import java.nio.file.Files;
import java.util.Base64;

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

    public File convertMultiPartToBinaryFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }


    public MultipartFile createMultipartFileFromBase64(String base64Audio, String fileName) {
        // Remove the data:audio/mp3;base64, prefix if present
        String base64Data = base64Audio.contains(",") ?
                base64Audio.substring(base64Audio.indexOf(",") + 1) : base64Audio;

        byte[] audioData = Base64.getDecoder().decode(base64Data);

        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "audio/mpeg";
            }

            @Override
            public boolean isEmpty() {
                return audioData.length == 0;
            }

            @Override
            public long getSize() {
                return audioData.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return audioData;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(audioData);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(audioData);
                }
            }
        };
    }

    public String convertFileToBase64(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }
}
