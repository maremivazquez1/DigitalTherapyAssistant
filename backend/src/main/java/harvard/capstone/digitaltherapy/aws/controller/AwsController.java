package harvard.capstone.digitaltherapy.aws.controller;

import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
    public String synthesizeTextToSpeech(@RequestBody String text, @RequestParam String fileName) throws IOException {
        return pollyService.convertTextToSpeech(text, fileName);
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
    public String transcribeSpeech(@RequestBody String mediaUri, @RequestParam String jobName) {
        return transcribeService.startTranscriptionJob(mediaUri, jobName);
    }
}
