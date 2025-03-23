package harvard.capstone.digitaltherapy.aws.service;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

@Service
public class PollyService {

    @Autowired
    private AmazonPolly amazonPolly;

    private static final String S3_BUCKET_NAME = "dta-root";
    private static final String S3_BUCKET_FOLDER = "dta-speech-translation-storage/";

    public String convertTextToSpeech(String text, String fileName) throws IOException {
        // Create the request
        SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
                .withText(text)
                .withVoiceId("Joanna")  // Set the voice for Polly
                .withOutputFormat("mp3");

        // Call Polly to synthesize speech
        SynthesizeSpeechResult synthesizeSpeechResult = amazonPolly.synthesizeSpeech(synthesizeSpeechRequest);

        // Get the audio stream from the result
        InputStream audioStream = synthesizeSpeechResult.getAudioStream();

        // Create a temporary file to write the audio
        File tempFile = File.createTempFile("polly-audio-", ".mp3");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // TODO: Upload the file to S3
        String s3FileName = S3_BUCKET_FOLDER + fileName + ".mp3";
        // amazonS3.putObject(new PutObjectRequest(S3_BUCKET_NAME, s3FileName, tempFile));

        // Delete the temporary file after uploading
        if (tempFile.exists()) {
            tempFile.delete();
        }

        // Construct and return the S3 URL of the uploaded file
        return "https://" + S3_BUCKET_NAME + ".s3.amazonaws.com/" + s3FileName;
    }
}
