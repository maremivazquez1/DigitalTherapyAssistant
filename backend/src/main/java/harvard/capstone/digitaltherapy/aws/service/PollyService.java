package harvard.capstone.digitaltherapy.aws.service;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class PollyService {

    @Autowired
    private AmazonPolly amazonPolly;

    @Autowired
    private AmazonS3 amazonS3;

    private static final String S3_BUCKET_NAME = "dta-root";
    private static final String S3_BUCKET_FOLDER = "dta-speech-translation-storage/";

    /**
     * Text-to-Speech service function
     *
     * @param s3Url The S3 URL where the text is stored
     * @param fileName File name to upload to S3. Should uniquely identify the file by user.
     * @return The synthesized S3 .mp3 file location
     */
    public String convertTextToSpeech(String s3Url, String fileName) {
        if (s3Url == null || s3Url.isEmpty()) {
            throw new IllegalArgumentException("S3 URL input is empty");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String text = null;
        try {
            // Download the text content from the provided S3 URL
            text = downloadTextFromS3(s3Url);
        } catch (IOException e) {
            System.err.println("Error downloading text from S3: " + e.getMessage());
            return null;  // Return null or handle the error as needed
        }

        try {
            // Synthesize speech from text
            SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
                    .withText(text)
                    .withVoiceId("Joanna")
                    .withOutputFormat("mp3");
    
            SynthesizeSpeechResult synthesizeSpeechResult = amazonPolly.synthesizeSpeech(synthesizeSpeechRequest);
    
            // Get the audio stream
            InputStream audioStream = synthesizeSpeechResult.getAudioStream();
    
            // Upload to S3
            String s3Key = S3_BUCKET_FOLDER + fileName + ".mp3";
            amazonS3.putObject(new PutObjectRequest(S3_BUCKET_NAME, s3Key, audioStream, null));
    
            // Return the S3 URL
            return "https://dta-root.s3.amazonaws.com/" + s3Key;
        } catch (RuntimeException e) {
            // Handle Polly failure (no S3 interactions should happen)
            throw new RuntimeException("Polly service failure", e); // Make sure to include the error message here
        }
    }

    /**
     * Downloads the text file from the specified S3 URL
     *
     * @param s3Url The S3 URL where the text is stored
     * @return The text content as a string
     * @throws IOException if there is an issue with reading the file
     */
    private String downloadTextFromS3(String s3Url) throws IOException {
        // Extract the bucket and key from the S3 URL
        String[] urlParts = s3Url.replace("s3://", "").split("/");
        String bucketName = urlParts[0];
        String key = String.join("/", urlParts).substring(bucketName.length() + 1); // Extract key

        // Retrieve the S3 object
        InputStream inputStream = amazonS3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent();

        // Read the input stream into a String
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString().trim(); // Return the content of the text file
    }
}
