package harvard.capstone.digitaltherapy.aws.controller;

import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/aws")
public class AwsController {

    @Autowired
    private PollyService pollyService;

    @Autowired
    private TranscribeService transcribeService;

    /**
     * Text-to-Speech service function
     *
     * @param text Text to translate
     * @param fileName File name to upload to S3. Should uniquely identify the file by user.
     * @return The synthesized S3 .mp3 file location
     */
    @PostMapping("/synthesize")
    public ResponseEntity<?> synthesizeTextToSpeech(@RequestBody Map<String, String> requestBody) {
        try {
            String text = requestBody.get("text");
            String fileName = requestBody.get("fileName");

            if (text == null || text.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Text input is empty", "code", 400));
            }

            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid file name", "code", 400));
            }

            String audioUrl = pollyService.convertTextToSpeech(text, fileName);
            return ResponseEntity.ok(Map.of("audioFileUrl", audioUrl));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AWS Polly synthesis failed: " + e.getMessage(), "code", 500));
        }
    }

    /**
     * Speech-to-Text service function
     *
     * @param mediUri .mp3 S3 asset URI. Expected format: s3://dta-root/dta-speech-translation-storage/[autiofileID].mp3
     * @param jobName user/job id string for the AWS Transcribe service
     * @return The translated text from the Transcribe job
     */
    // mediaUri = s3://dta-root/dta-speech-translation-storage/[autiofileID].mp3
    // jobName = user/job id string
    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeSpeech(@RequestBody Map<String, String> requestBody) {
        try {
            String mediaUri = requestBody.get("mediaUri");
            String jobName = requestBody.get("jobName");

            if (mediaUri == null || mediaUri.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid media URI", "code", 400));
            }

            if (jobName == null || jobName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid job name", "code", 400));
            }

            String transcript = transcribeService.startTranscriptionJob(mediaUri, jobName);
            return ResponseEntity.ok(Map.of("transcribedText", transcript));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AWS Transcribe processing failed: " + e.getMessage(), "code", 500));
        }
    }
}
