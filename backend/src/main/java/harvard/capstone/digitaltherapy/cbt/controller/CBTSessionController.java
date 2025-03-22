package harvard.capstone.digitaltherapy.cbt.controller;

import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class CBTSessionController {

    private static final String ALLOWED_CONTENT_TYPE = "audio/mpeg";
    private static final String OUTPUT_FILE_PATH = "backend/src/main/resources/cbt_session.mp3";
    private final S3Utils s3Service;

    public CBTSessionController(S3Utils s3Service) {
        this.s3Service = s3Service;
    }
    @PostMapping(value = "/cbt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileSystemResource> cbtSession(@RequestParam("file") MultipartFile file,  @RequestParam("type") String type,
                                                         @RequestParam("contentType") String contentType) {
        // Validate file
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        // Validate content type
        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        try {
            File convertedFile = convertMultiPartToFile(file);
            String keyName = file.getOriginalFilename();
            String response = s3Service.uploadFile(convertedFile.getAbsolutePath(), keyName);
            convertedFile.delete(); // Cleanup temp file
            File response_file = s3Service.downloadFileFromS3("dta-root", keyName);
            // Create a FileSystemResource from the file
            FileSystemResource fileResource = new FileSystemResource(response_file);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

            // Return the file as a ResponseEntity with appropriate headers
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileResource);

        } catch (IOException e) {
            // Log the error here using a proper logging framework
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
