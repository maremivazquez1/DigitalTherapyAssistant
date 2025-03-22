package harvard.capstone.digitaltherapy.cbt.controller;

import harvard.capstone.digitaltherapy.utility.S3FileUploadUtil;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class CBTSessionController {

    private static final String ALLOWED_CONTENT_TYPE = "audio/mpeg";
    private static final String OUTPUT_FILE_PATH = "backend/src/main/resources/cbt_session.mp3";
    private final S3FileUploadUtil s3Service;

    public CBTSessionController(S3FileUploadUtil s3Service) {
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
            Path outputPath = Paths.get(OUTPUT_FILE_PATH);
            // Ensure directory exists
            Files.createDirectories(outputPath.getParent());

            // Create output file if it doesn't exist
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
            }

            File output = outputPath.toFile();
            FileSystemResource resource = new FileSystemResource(output);

            // Verify resource exists and is readable
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ALLOWED_CONTENT_TYPE))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"processed_file.mp3\"")
                    .body(resource);

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
