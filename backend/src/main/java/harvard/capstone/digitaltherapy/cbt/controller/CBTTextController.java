package harvard.capstone.digitaltherapy.cbt.controller;

import harvard.capstone.digitaltherapy.authentication.service.UserLoginService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class CBTTextController {
    private static final Logger logger = LoggerFactory.getLogger(CBTTextController.class);
    private static final String ALLOWED_CONTENT_TYPE = "text/plain";
    private final S3Utils s3Service;
    @Autowired
    private CBTHelper cbtHelper;

    public CBTTextController(S3Utils s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(value = "/cbt-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadTextFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            // Validate content type
            if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            // Convert and upload file
            File convertedFile = cbtHelper.convertMultiPartToTextFile(file);
            String response = s3Service.uploadFile(convertedFile.getAbsolutePath(), originalFilename);

            // Cleanup temp file
            if (!convertedFile.delete()) {
                logger.warn("Failed to delete temporary file: {}", convertedFile.getAbsolutePath());
            }
            ResponseEntity<StreamingResponseBody> response_file = cbtHelper.downloadTextFile(originalFilename);
            if (response_file.getStatusCode() == HttpStatus.OK && response_file.getBody() != null) {
                // Convert StreamingResponseBody to String
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                response_file.getBody().writeTo(baos);
                String fileContent = baos.toString(StandardCharsets.UTF_8);

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(fileContent);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to retrieve file content");
            }

        } catch (IOException e) {
            logger.error("Error processing file upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process file: " + e.getMessage());
        }
    }


}
