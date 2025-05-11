package harvard.capstone.digitaltherapy.cbt.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.aws.service.RekognitionService;
import harvard.capstone.digitaltherapy.cbt.service.OrchestrationService;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.llm.service.S3StorageService;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CBTController {
    private static final Logger logger = LoggerFactory.getLogger(CBTController.class);

    private final ObjectMapper objectMapper;
    private final S3Utils s3Service;
    private final S3StorageService s3StorageService;
    private final CBTHelper cbtHelper;
    private final TranscribeService transcribeService;
    private final LLMProcessingService llmProcessingService;
    private final PollyService pollyService;
    private final RekognitionService rekognitionService;
    String currentModality = "";
    private final OrchestrationService orchestrationService;
    Map<String, String> input = new HashMap<>();
    private String  audio_s3_path ="";
    @Value("${ffmpeg_path}")
    private String ffmpeg_path;
    @Autowired
    public CBTController(ObjectMapper objectMapper,
                         S3Utils s3Service,
                         CBTHelper cbtHelper,
                         TranscribeService transcribeService,
                         LLMProcessingService llmProcessingService,
                         PollyService pollyService,
                         RekognitionService rekognitionService,
                         OrchestrationService orchestrationService,
                         S3StorageService s3StorageService) {
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
        this.cbtHelper = cbtHelper;
        this.transcribeService = transcribeService;
        this.llmProcessingService = llmProcessingService;
        this.pollyService = pollyService;
        this.rekognitionService = rekognitionService;
        this.orchestrationService =  orchestrationService;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Builds a key of the form "userId/sessionId/originalKey"
     */
    private String getUserId(WebSocketSession session) {
        Object raw = session.getAttributes().get("username");
        return (raw instanceof String) ? (String) raw : "anonymous";
    }

    private String prefixKey(WebSocketSession session, String originalKey) {
        String userId    = getUserId(session);
        String sessionId = session.getId();
        return userId + "_" + sessionId + "_" + originalKey;
    }

    public void handleMessage(WebSocketSession session, JsonNode requestJson, String messageType, String requestId) throws IOException {
        if ("audio".equals(messageType)) {
            handleAudioMessage(session, requestJson, requestId);
        } else {
            String content = requestJson.has("text") ? requestJson.get("text").asText() : "";
             // Check if modality field exists
            if (requestJson.has("modality")) {
                this.currentModality = requestJson.get("modality").asText();
            } else {
                handleTextMessage(session, requestJson);
                return;
            }
        }
    }

    public void handleTextOnlyMessage(WebSocketSession session, String content, String requestId) throws IOException {
        if (content.isEmpty()) {
            sendErrorMessage(session, "Text content cannot be empty", 400, requestId);
            return;
        }
        try {
            // Create a temporary text file
            File tempFile = File.createTempFile("text_", ".txt");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(content);
            }
            // Generate unique filename
            String fileName = "text_" + requestId + ".txt";
            // Upload to S3 with prefix
            String keyName = prefixKey(session, fileName);
            String uploadResponse = s3Service.uploadFile(tempFile.getAbsolutePath(), keyName);
            // Get processed content
            String llmResponse = llmProcessingService.process(uploadResponse);
            llmResponse=llmResponse.replace("s3://dta-root/", "");
            ResponseEntity<StreamingResponseBody> processedResponse = cbtHelper.downloadTextFile(llmResponse);
            if (processedResponse.getStatusCode() == HttpStatus.OK && processedResponse.getBody() != null) {
                // Convert StreamingResponseBody to String
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                processedResponse.getBody().writeTo(baos);
                String processedContent = baos.toString(StandardCharsets.UTF_8);
                // Create response JSON
                ObjectNode responseJson = objectMapper.createObjectNode();
                responseJson.put("type", "text-processed");
                responseJson.put("requestId", requestId);
                responseJson.put("originalContent", content);
                responseJson.put("processedContent", processedContent);
                responseJson.put("fileName", fileName);
                // Send response
                session.sendMessage(new TextMessage(responseJson.toString()));
            } else {
                sendErrorMessage(session, "Failed to process text content", 500, requestId);
            }
            // Cleanup
            if (!tempFile.delete()) {
                logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Error processing text message: {}", e.getMessage(), e);
            sendErrorMessage(session, "Error processing text: " + e.getMessage(), 500, requestId);
        }
    }

    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        logger.info("Received binary message from session {}", sessionId);
        try {
            // Get the binary payload
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);
            File tempFile = null;
            String keyName ="";
            String input_transcript = "";
            if (currentModality.equalsIgnoreCase("video")) {
                File webmFile = null;
                File mp4File = null;
                keyName = "video_" + sessionId + ".mp4";
                String keyWithPrefix = prefixKey(session, keyName);
                try {
                    // Step 1: Save incoming WebM data to a temp file
                    webmFile = File.createTempFile("video_temp_", ".webm");
                    try (FileOutputStream fos = new FileOutputStream(webmFile)) {
                        fos.write(audioData);
                    }
                    // Step 2: Create target MP4 file
                    mp4File = new File(webmFile.getParent(), "converted_" + webmFile.getName().replace(".webm", ".mp4"));
                    // Configure audio attributes
                    AudioAttributes audio = new AudioAttributes();
                    audio.setCodec("aac");
                    audio.setBitRate(128000);
                    audio.setChannels(2);
                    audio.setSamplingRate(44100);

                    // Configure video attributes
                    VideoAttributes video = new VideoAttributes();
                    video.setCodec("h264");
                    video.setBitRate(800000);
                    video.setFrameRate(30);

                    // Configure encoding attributes
                    EncodingAttributes attrs = new EncodingAttributes();
                    attrs.setOutputFormat("mp4");
                    attrs.setAudioAttributes(audio);
                    attrs.setVideoAttributes(video);

                    // Use ProcessBuilder directly for more control
                    List<String> command = Arrays.asList(
                            ffmpeg_path,
                            "-i", webmFile.getAbsolutePath(),
                            "-c:v", "h264",
                            "-c:a", "aac",
                            "-b:a", "128k",
                            "-b:v", "800k",
                            "-r", "30",
                            mp4File.getAbsolutePath()
                    );

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new RuntimeException("FFmpeg process failed with exit code: " + exitCode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
                }
                long s3AudioFileUploadTime  = System.currentTimeMillis();
                String response = s3Service.uploadFile(mp4File.getAbsolutePath(), keyWithPrefix);
                input.put("video", response);
                logger.info("S3 Video File Upload took {} ms", System.currentTimeMillis() - s3AudioFileUploadTime);
                if(input.size()==2){
                    processFinalMessage(session,audio_s3_path);
                }
            }
            else if(currentModality.equalsIgnoreCase("audio")){
                tempFile = File.createTempFile("audio_", ".mp3");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(audioData);
                }
                 keyName = "audio_" + sessionId + ".mp3";
                 String keyWithPrefix = prefixKey(session, keyName);
                 audio_s3_path = s3Service.uploadFile(tempFile.getAbsolutePath(), keyWithPrefix);
                 String bucketName = "dta-root";
                 String presignedUrl = S3Utils.generatePresignedUrl(bucketName, keyWithPrefix, Duration.ofMinutes(15));
                 input.put("audio", presignedUrl);
                 tempFile.delete(); // Cleanup temp file
                if(input.size()==2){
                    processFinalMessage(session,audio_s3_path);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing binary message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Error processing audio: " + e.getMessage(), 500, "unknown");
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", sessionId, ex);
            }
        }
    }

    public void handleAudioMessage(WebSocketSession session, JsonNode requestJson, String requestId) throws IOException {
        String audioData = requestJson.get("audioData").asText();
        String fileName = requestJson.get("fileName").asText();
        String type = requestJson.has("type") ? requestJson.get("type").asText() : "";

        try {
            // Convert base64 audio to MultipartFile
            MultipartFile multipartFile = cbtHelper.createMultipartFileFromBase64(audioData, fileName);

            // Process the audio using existing functionality
            File convertedFile = cbtHelper.convertMultiPartToBinaryFile(multipartFile);
            String keyName = requestId + multipartFile.getOriginalFilename();
            String keyWithPrefix = prefixKey(session, keyName);

            // Upload to S3
            String response = s3Service.uploadFile(convertedFile.getAbsolutePath(), keyWithPrefix);

            convertedFile.delete(); // Cleanup temp file

            // Download processed file
            File responseFile = s3Service.downloadFileFromS3("dta-root", keyWithPrefix);

            // Convert processed file to base64 for WebSocket response
            String processedAudioBase64 = cbtHelper.convertFileToBase64(responseFile);

            // Create response JSON
            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("requestId", requestId);
            responseJson.put("type", "audio");
            responseJson.put("fileName", fileName);
            responseJson.put("processedAudio", processedAudioBase64);

            // Send the response
            session.sendMessage(new TextMessage(responseJson.toString()));

            // Cleanup
            responseFile.delete();

        } catch (Exception e) {
            logger.error("Error processing audio: {}", e.getMessage(), e);
            sendErrorMessage(session, "Error processing audio: " + e.getMessage(), 500, requestId);
        }
    }

    public void handleTextMessage(WebSocketSession session, JsonNode requestJson) {
        String sessionId = session.getId();
        logger.info("Received message from session {}: {}", sessionId, "Handle text chat input");

        try {
            String messageType = requestJson.has("type") ? requestJson.get("type").asText() : "text";
            String requestId = requestJson.has("requestId") ? requestJson.get("requestId").asText() : "unknown";
            String content = requestJson.has("text") ? requestJson.get("text").asText() : "";

            if ("audio".equals(messageType)) {
                handleAudioMessage(session, requestJson, requestId);
            } else {
                handleTextOnlyMessage(session, content, requestId);
            }

        } catch (Exception e) {
            logger.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Error processing message: " + e.getMessage(), 500, "unknown");
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", sessionId, ex);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String message, int code, String requestId) throws IOException {
        ObjectNode errorJson = objectMapper.createObjectNode();
        errorJson.put("error", message);
        errorJson.put("code", code);
        errorJson.put("requestId", requestId);
        session.sendMessage(new TextMessage(errorJson.toString()));
    }

    private String generateOutputPath(String inputPath) {
        String outputPath= inputPath.replace("s3://dta-root/", "");
        int dotIndex = outputPath.lastIndexOf('.');
        if (dotIndex > 0) {
            outputPath = outputPath.substring(0, dotIndex) + "-response.txt";
            return outputPath;
        } else {
            return outputPath + "-response.txt";
        }
    }

    public void processFinalMessage(WebSocketSession session, String s3Path) throws IOException {
        String sessionId = session.getId();
        String userId = getUserId(session);
        
        orchestrationService.setSessionContext(sessionId, userId);
        String transcribedText = "";

        String input_transcript_s3_url = transcribeService.startTranscriptionJob(s3Path,sessionId);
        try {
            File transcribedFile = s3Service.downloadFileFromS3("dta-root",
                    input_transcript_s3_url.replace("https://s3.amazonaws.com/dta-root/", ""));
            input.put("text", input_transcript_s3_url);
            transcribedText = new String(Files.readAllBytes(transcribedFile.toPath()), StandardCharsets.UTF_8);
            try {
                JsonNode rootNode = objectMapper.readTree(transcribedText);
                JsonNode transcriptsNode = rootNode.path("results").path("transcripts");

                if (!transcriptsNode.isEmpty() && transcriptsNode.get(0).has("transcript")) {
                    transcribedText = transcriptsNode.get(0).path("transcript").asText();
                } else {
                    logger.warn("Transcript not found in the expected JSON structure");
                    transcribedText = "Transcription failed";
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parsing transcription JSON: {}", e.getMessage());
                transcribedText = "Error processing transcription";
            }
            transcribedFile.delete(); // Cleanup
        } catch (Exception e) {
            logger.error("Error reading transcribed text: {}", e.getMessage(), e);
        }
        // Send transcribed text as a text message
        ObjectNode textResponse = objectMapper.createObjectNode();
        textResponse.put("type", "input-transcription");
        textResponse.put("text", transcribedText);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(textResponse)));
        String llmResponse = orchestrationService.processUserMessage(sessionId, input, transcribedText);
        ObjectNode textLLMResponse = objectMapper.createObjectNode();
        textLLMResponse.put("type", "output-transcription");
        textLLMResponse.put("text", llmResponse);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(textLLMResponse)));
        String outputPath = generateOutputPath(s3Path);
        s3StorageService.writeTextToS3(outputPath, llmResponse);
        String rootOutputPath ="s3://dta-root/"+ outputPath;
        long pollyServiceTime  = System.currentTimeMillis();
        String textToSpeechResponse = pollyService.convertTextToSpeech(rootOutputPath, sessionId);
        logger.info("Polly Service Response Time took {} ms", System.currentTimeMillis() - pollyServiceTime);
        textToSpeechResponse= textToSpeechResponse.replace("https://dta-root.s3.amazonaws.com/", "");
        long S3DownloadTime  = System.currentTimeMillis();
        File responseFile = s3Service.downloadFileFromS3("dta-root", textToSpeechResponse);
        logger.info("S3 response download Time took {} ms", System.currentTimeMillis() - S3DownloadTime);
        // Convert processed file to binary message
        byte[] processedAudio = Files.readAllBytes(responseFile.toPath());
        BinaryMessage responseMessage = new BinaryMessage(processedAudio);
        // Send the binary response
        session.sendMessage(responseMessage);
        input.clear();
        audio_s3_path = "";
    }

    @PostConstruct
    public void disableCertificateValidation() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            logger.error("Failed to disable certificate validation", e);
        }
    }


}
