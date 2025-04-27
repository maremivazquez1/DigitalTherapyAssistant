package harvard.capstone.digitaltherapy.aws.service;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PollyService {

    private final AmazonPolly amazonPolly;
    private final S3Utils s3Utils;
    private static final String S3_BUCKET_NAME = "dta-root";

    public PollyService(AmazonPolly amazonPolly, S3Utils s3Utils) {
        this.amazonPolly = amazonPolly;
        this.s3Utils = s3Utils;
    }

    /**
     * Text-to-Speech service function
     *
     * @param s3Url The S3 URL where the text is stored
     * @param fileName File name to upload to S3. Should uniquely identify the file by user.
     * @return The synthesized S3 .mp3 file location
     */
    public String synthesizeSpeech(String s3Url, String fileName) {
        try {
            // Extract bucket and key from S3 URL
            String[] parts = s3Url.split("/", 4);
            String bucketName = parts[2];
            String key = parts[3];

            // Download the text file from S3
            File textFile = s3Utils.downloadFileFromS3(bucketName, key);
            String textContent = new String(Files.readAllBytes(textFile.toPath()));

            // Synthesize speech using Polly
            SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
                    .withText(textContent)
                    .withVoiceId("Joanna")
                    .withOutputFormat("mp3");

            SynthesizeSpeechResult synthesizeSpeechResult = amazonPolly.synthesizeSpeech(synthesizeSpeechRequest);
            InputStream audioStream = synthesizeSpeechResult.getAudioStream();

            // Save the audio stream to a temporary file
            Path tempAudioFile = Files.createTempFile("polly-output-", ".mp3");
            Files.copy(audioStream, tempAudioFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Upload the audio file to S3 using S3Utils
            String s3Uri = s3Utils.uploadFile(tempAudioFile.toString(), fileName + ".mp3");

            // Clean up temporary files
            Files.deleteIfExists(tempAudioFile);
            Files.deleteIfExists(textFile.toPath());

            return s3Uri;
        } catch (IOException e) {
            throw new RuntimeException("Error processing text-to-speech request", e);
        }
    }
}
