
package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CBTWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CBTWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final S3Utils s3Service;
    private final CBTHelper cbtHelper;
    // Store active sessions
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Store conversation history per session
    private final ConcurrentHashMap<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    @Autowired
    private TranscribeService transcribeService;

    @Autowired
    private LLMProcessingService llmProcessingService;

    @Autowired
    private PollyService pollyService;

    @Autowired
    public CBTWebSocketHandler(ObjectMapper objectMapper, S3Utils s3Service, CBTHelper cbtHelper) {
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
        this.cbtHelper = cbtHelper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("New WebSocket connection established: {}", sessionId);

        // Send welcome message
        ObjectNode welcomeMessage = objectMapper.createObjectNode();
        welcomeMessage.put("type", "system");
        welcomeMessage.put("message", "Connected successfully");
        session.sendMessage(new TextMessage(welcomeMessage.toString()));
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("WebSocket connection closed for session {}: {} - {}",
                sessionId, status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("Transport error for session {}: {}", sessionId, exception.getMessage(), exception);

        // Optionally close the session on transport error
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(sessionId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        logger.info("Received message from session {}: {}", sessionId, message.getPayload());

        try {
            JsonNode requestJson = objectMapper.readTree(message.getPayload());
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

    private void handleTextOnlyMessage(WebSocketSession session, String content, String requestId) throws IOException {
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
            // Upload to S3
            String uploadResponse = s3Service.uploadFile(tempFile.getAbsolutePath(), fileName);
            // Get processed content
            ResponseEntity<StreamingResponseBody> processedResponse = cbtHelper.downloadTextFile(fileName);
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

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        logger.info("Received binary message from session {}", sessionId);

        try {
            // Get the binary payload
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);

            // Create a temporary file to store the audio
            File tempFile = File.createTempFile("audio_", ".mp3");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audioData);
            }

            // Process the audio using existing functionality
            String keyName = "audio_" + sessionId + ".mp3";

            // Upload to S3
            String response = s3Service.uploadFile(tempFile.getAbsolutePath(), keyName);
            String transcribedS3Path = transcribeService.startTranscriptionJob(response, sessionId);
            tempFile.delete(); // Cleanup temp file
            String s3Path = transcribedS3Path.replace("https://s3.amazonaws.com/", "s3://");
            String llmResponse = llmProcessingService.process(s3Path);
            String textToSpeechResponse = pollyService.convertTextToSpeech(llmResponse, sessionId);
            textToSpeechResponse= textToSpeechResponse.replace("https://dta-root.s3.amazonaws.com/", "");
            //https://dta-root.s3.amazonaws.com/dta-speech-translation-storage/d1007497-53ab-3816-ad8d-f265db662517.mp3
            // Download processed file
            File responseFile = s3Service.downloadFileFromS3("dta-root", textToSpeechResponse);

            // Convert processed file to binary message
            byte[] processedAudio = Files.readAllBytes(responseFile.toPath());
            BinaryMessage responseMessage = new BinaryMessage(processedAudio);

            // Send the binary response
            session.sendMessage(responseMessage);
            // Cleanup
            responseFile.delete();

        } catch (Exception e) {
            logger.error("Error processing binary message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Error processing audio: " + e.getMessage(), 500, "unknown");
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", sessionId, ex);
            }
        }
    }

    private void handleAudioMessage(WebSocketSession session, JsonNode requestJson, String requestId) throws IOException {
        String audioData = requestJson.get("audioData").asText();
        String fileName = requestJson.get("fileName").asText();
        String type = requestJson.has("type") ? requestJson.get("type").asText() : "";

        try {
            // Convert base64 audio to MultipartFile
            MultipartFile multipartFile = createMultipartFileFromBase64(audioData, fileName);

            // Process the audio using existing functionality
            File convertedFile = cbtHelper.convertMultiPartToBinaryFile(multipartFile);
            String keyName = requestId + multipartFile.getOriginalFilename();

            // Upload to S3
            String response = s3Service.uploadFile(convertedFile.getAbsolutePath(), keyName);

            convertedFile.delete(); // Cleanup temp file

            // Download processed file
            File responseFile = s3Service.downloadFileFromS3("dta-root", keyName);

            // Convert processed file to base64 for WebSocket response
            String processedAudioBase64 = convertFileToBase64(responseFile);

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

    private MultipartFile createMultipartFileFromBase64(String base64Audio, String fileName) {
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

    private String convertFileToBase64(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private void sendErrorMessage(WebSocketSession session, String message, int code, String requestId) throws IOException {
        ObjectNode errorJson = objectMapper.createObjectNode();
        errorJson.put("error", message);
        errorJson.put("code", code);
        errorJson.put("requestId", requestId);
        session.sendMessage(new TextMessage(errorJson.toString()));
    }
}