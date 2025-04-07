package harvard.capstone.digitaltherapy.cbt.controller;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.aws.service.AudioStreamPublisher;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeStreamingService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class CBTController {
    private static final Logger logger = LoggerFactory.getLogger(CBTController.class);

    private final ObjectMapper objectMapper;
    private final S3Utils s3Service;
    private final CBTHelper cbtHelper;
    private final TranscribeService transcribeService;
    private final TranscribeStreamingService transcribeStreamingService;
    private final LLMProcessingService llmProcessingService;
    private final PollyService pollyService;

    @Autowired
    public CBTController(ObjectMapper objectMapper,
                         S3Utils s3Service,
                         CBTHelper cbtHelper,
                         TranscribeService transcribeService,
                         TranscribeStreamingService transcribeStreamingService,
                         LLMProcessingService llmProcessingService,
                         PollyService pollyService) {
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
        this.cbtHelper = cbtHelper;
        this.transcribeService = transcribeService;
        this.transcribeStreamingService = transcribeStreamingService;
        this.llmProcessingService = llmProcessingService;
        this.pollyService = pollyService;
    }


    public void handleMessage(WebSocketSession session, JsonNode requestJson, String messageType, String requestId) throws IOException {
        if ("audio".equals(messageType)) {
            handleAudioMessage(session, requestJson, requestId);
        } else {
            String content = requestJson.has("text") ? requestJson.get("text").asText() : "";
            handleTextMessage(session, requestJson);
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
            // Upload to S3
            String uploadResponse = s3Service.uploadFile(tempFile.getAbsolutePath(), fileName);
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

    // public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    //     String sessionId = session.getId();
    //     logger.info("Received binary message from session {}", sessionId);

    //     try {
    //         // Get the binary payload
    //         ByteBuffer buffer = message.getPayload();
    //         byte[] audioData = new byte[buffer.remaining()];
    //         buffer.get(audioData);

    //         // Create a temporary file to store the audio
    //         File tempFile = File.createTempFile("audio_", ".mp3");
    //         try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    //             fos.write(audioData);
    //         }

    //         // Process the audio using existing functionality
    //         String keyName = "audio_" + sessionId + ".mp3";
    //         // Upload to S3
    //         long s3AudioFileUploadTime  = System.currentTimeMillis();
    //         String response = s3Service.uploadFile(tempFile.getAbsolutePath(), keyName);
    //         logger.info("S3 Audio File Upload took {} ms", System.currentTimeMillis() - s3AudioFileUploadTime);
    //         long transcribeServiceTime  = System.currentTimeMillis();
    //         String transcribedS3Path = transcribeService.startTranscriptionJob(response, sessionId);
    //         logger.info("transcribe Service took {} ms", System.currentTimeMillis() - transcribeServiceTime);
    //         tempFile.delete(); // Cleanup temp file
    //         String transcribedText = "";
    //         String transcript = "";
    //         try {
    //             File transcribedFile = s3Service.downloadFileFromS3("dta-root",
    //                     transcribedS3Path.replace("https://s3.amazonaws.com/dta-root/", ""));
    //             transcribedText = new String(Files.readAllBytes(transcribedFile.toPath()), StandardCharsets.UTF_8);
    //             try {
    //                 JsonNode rootNode = objectMapper.readTree(transcribedText);
    //                 JsonNode transcriptsNode = rootNode.path("results").path("transcripts");

    //                 if (!transcriptsNode.isEmpty() && transcriptsNode.get(0).has("transcript")) {
    //                     transcript = transcriptsNode.get(0).path("transcript").asText();
    //                 } else {
    //                     logger.warn("Transcript not found in the expected JSON structure");
    //                     transcript = "Transcription failed";
    //                 }
    //             } catch (JsonProcessingException e) {
    //                 logger.error("Error parsing transcription JSON: {}", e.getMessage());
    //                 transcript = "Error processing transcription";
    //             }
    //             transcribedFile.delete(); // Cleanup
    //         } catch (Exception e) {
    //             logger.error("Error reading transcribed text: {}", e.getMessage(), e);
    //         }
    //         // Send transcribed text as a text message
    //         ObjectNode textResponse = objectMapper.createObjectNode();
    //         textResponse.put("type", "transcription");
    //         textResponse.put("text", transcript);
    //         session.sendMessage(new TextMessage(objectMapper.writeValueAsString(textResponse)));

    //         String s3Path = transcribedS3Path.replace("https://s3.amazonaws.com/", "s3://");
    //         long llmResponseTime  = System.currentTimeMillis();
    //         String llmResponse = llmProcessingService.process(s3Path);
    //         logger.info("llm Response Time took {} ms", System.currentTimeMillis() - llmResponseTime);
    //         long pollyServiceTime  = System.currentTimeMillis();
    //         String textToSpeechResponse = pollyService.convertTextToSpeech(llmResponse, sessionId);
    //         logger.info("Polly Service Response Time took {} ms", System.currentTimeMillis() - pollyServiceTime);
    //         textToSpeechResponse= textToSpeechResponse.replace("https://dta-root.s3.amazonaws.com/", "");
    //         long S3DownloadTime  = System.currentTimeMillis();
    //         File responseFile = s3Service.downloadFileFromS3("dta-root", textToSpeechResponse);
    //         logger.info("S3 response download Time took {} ms", System.currentTimeMillis() - S3DownloadTime);
    //         // Convert processed file to binary message
    //         byte[] processedAudio = Files.readAllBytes(responseFile.toPath());
    //         BinaryMessage responseMessage = new BinaryMessage(processedAudio);
    //         // Send the binary response
    //         session.sendMessage(responseMessage);
    //         // Cleanup
    //         responseFile.delete();

    //     } catch (Exception e) {
    //         logger.error("Error processing binary message from session {}: {}", sessionId, e.getMessage(), e);
    //         try {
    //             sendErrorMessage(session, "Error processing audio: " + e.getMessage(), 500, "unknown");
    //         } catch (IOException ex) {
    //             logger.error("Failed to send error message to session {}", sessionId, ex);
    //         }
    //     }
    // }

    public void handleBinaryMessageAsyc(WebSocketSession session, BinaryMessage message) {
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
            
            // Upload to S3 asynchronously
            long s3AudioFileUploadTime = System.currentTimeMillis();
            String response = s3Service.uploadFile(tempFile.getAbsolutePath(), keyName);
            logger.info("S3 Audio File Upload took {} ms", System.currentTimeMillis() - s3AudioFileUploadTime);
    
            // Start transcription asynchronously
            CompletableFuture<String> transcribeFuture = transcribeService.startTranscriptionJobAsync(response, sessionId);
    
            // Wait for transcription completion and get the result using join()
            String transcribedS3Path = transcribeFuture.join();
    
            // Fetch transcribed text
            String transcribedText = fetchTranscribedText(transcribedS3Path);

            logger.info("Transcribe fetch took {} ms", System.currentTimeMillis() - s3AudioFileUploadTime);

    
            // Send the transcription result back to the WebSocket session
            sendTranscriptionResult(session, transcribedText);
    
            // Proceed with LLM and Polly services
            String s3Path = transcribedS3Path.replace("https://s3.amazonaws.com/", "s3://");
            String llmResponse = llmProcessingService.process(s3Path);
            String textToSpeechResponse = pollyService.convertTextToSpeech(llmResponse, sessionId);
    
            // Download processed file and send as binary response
            byte[] processedAudio = processTextToSpeechResponse(textToSpeechResponse);
            sendBinaryResponse(session, processedAudio);
    
            // Cleanup temp file
            tempFile.delete();
    
        } catch (Exception e) {
            logger.error("Error processing binary message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Error processing audio: " + e.getMessage(), 500, "unknown");
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", sessionId, ex);
            }
        }
    }
    

    private String fetchTranscribedText(String transcribedS3Path) {
        try {
            logger.info("Fetch from path: " + transcribedS3Path);
            File transcribedFile = s3Service.downloadFileFromS3("dta-root",
                    transcribedS3Path.replace("https://s3.amazonaws.com/dta-root/", ""));
            String transcribedText = new String(Files.readAllBytes(transcribedFile.toPath()), StandardCharsets.UTF_8);
            transcribedFile.delete(); // Cleanup

            // Parse the transcription result
            JsonNode rootNode = objectMapper.readTree(transcribedText);
            JsonNode transcriptsNode = rootNode.path("results").path("transcripts");

            if (!transcriptsNode.isEmpty() && transcriptsNode.get(0).has("transcript")) {
                return transcriptsNode.get(0).path("transcript").asText();
            } else {
                logger.warn("Transcript not found in the expected JSON structure");
                return "Transcription failed";
            }

        } catch (Exception e) {
            logger.error("Error reading transcribed text: {}", e.getMessage(), e);
            return "Error processing transcription";
        }
    }

    private void sendTranscriptionResult(WebSocketSession session, String transcribedText) {
        try {
            ObjectNode textResponse = objectMapper.createObjectNode();
            textResponse.put("type", "transcription");
            textResponse.put("text", transcribedText);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(textResponse)));
        } catch (IOException e) {
            logger.error("Error sending transcription result: {}", e.getMessage(), e);
        }
    }

    private byte[] processTextToSpeechResponse(String textToSpeechResponse) throws IOException {
        long S3DownloadTime = System.currentTimeMillis();
        File responseFile = s3Service.downloadFileFromS3("dta-root", textToSpeechResponse.replace("https://dta-root.s3.amazonaws.com/", ""));
        logger.info("S3 response download Time took {} ms", System.currentTimeMillis() - S3DownloadTime);
        byte[] processedAudio = Files.readAllBytes(responseFile.toPath());
        responseFile.delete(); // Cleanup
        return processedAudio;
    }

    private void sendBinaryResponse(WebSocketSession session, byte[] processedAudio) {
        try {
            BinaryMessage responseMessage = new BinaryMessage(processedAudio);
            session.sendMessage(responseMessage);
        } catch (IOException e) {
            logger.error("Error sending binary response: {}", e.getMessage(), e);
        }
    }

    // High-Level Flow:
    // - Receive Audio: WebSocket message arrives with audio data.
    // - Extract Audio Data: The audio is extracted and sent to the AudioStreamPublisher.
    // - Start Transcription: If this is the first message for the session, transcription is started by calling startTranscription with the audio stream and transcription listener.
    // - Transcription Updates:
    // -- As transcription occurs, partial results are sent back to the client in real time.
    // -- When transcription is completed, the session is cleaned up, and resources are freed.
    // - Error Handling: Any errors in the process (either with WebSocket or transcription) are caught and reported back to the client.

    private final Map<String, AudioStreamPublisher> sessionAudioStreamMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionStartedMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> sessionAudioChunkCounter = new ConcurrentHashMap<>();
    private final Map<String, List<byte[]>> sessionAudioBuffer = new ConcurrentHashMap<>();

    public void handleStreamingBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        logger.info("Received streaming binary message from session {}", sessionId);
        
        try {
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);
            logger.info("Extracted {} bytes of audio data for session {}", audioData.length, sessionId);
    
            // Create or reuse the AudioStreamPublisher
            AudioStreamPublisher publisher = sessionAudioStreamMap.computeIfAbsent(sessionId, id -> {
                logger.info("Creating new AudioStreamPublisher for session {}", sessionId);
                return new AudioStreamPublisher();
            });
    
            // Start transcription if not already started
            if (!sessionStartedMap.containsKey(sessionId)) {
                logger.info("Buffering audio for session {}", sessionId);
                sessionAudioBuffer.computeIfAbsent(sessionId, id -> new ArrayList<>()).add(audioData);
    
                // Start transcription (or restart if previously failed)
                logger.info("Starting transcription for session {}", sessionId);
                sessionStartedMap.put(sessionId, true);
    
                // Start the transcription with the listener
                transcribeStreamingService.startTranscription(publisher, new TranscribeStreamingService.TranscriptionListener() {
                    @Override
                    public void onTranscript(String transcript) {
                        logger.info("Received partial transcription: {}", transcript);
    
                        // Send partial transcription to client
                        try {
                            ObjectNode response = objectMapper.createObjectNode();
                            response.put("type", "partial_transcription");
                            response.put("text", transcript);
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                            
                            // Pass transcribed text to other services
                            passTranscribedTextToOtherServices(sessionId, transcript);
                        } catch (IOException e) {
                            logger.error("Error sending transcription response to client: {}", e.getMessage(), e);
                        }
                    }
    
                    @Override
                    public void onComplete() {
                        logger.info("Transcription stream completed for session {}", sessionId);
                        publisher.complete();
                        sessionAudioStreamMap.remove(sessionId);
                        sessionStartedMap.remove(sessionId);
                        sessionAudioBuffer.remove(sessionId); // Cleanup
                    }
    
                    @Override
                    public void onError(Throwable error) {
                        logger.error("Error during transcription for session {}: {}", sessionId, error.getMessage(), error);
                        try {
                            sendErrorMessage(session, "Streaming transcription error: " + error.getMessage(), 500, sessionId);
                        } catch (IOException ex) {
                            logger.error("Failed to send error message", ex);
                        }
                    }
                });
    
                // Flush buffered audio chunks to Transcribe service
                List<byte[]> bufferedChunks = sessionAudioBuffer.getOrDefault(sessionId, Collections.emptyList());
                logger.info("Flushing {} buffered chunks for session {}", bufferedChunks.size(), sessionId);
                for (byte[] chunk : bufferedChunks) {
                    publisher.publish(chunk);
                }
    
            } else {
                // Already started transcription, continue publishing live audio
                logger.info("Publishing live audio chunk for session {}", sessionId);
                publisher.publish(audioData);
            }
    
        } catch (Exception e) {
            logger.error("Error during streaming transcription for session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Streaming transcription error: " + e.getMessage(), 500, sessionId);
            } catch (IOException ex) {
                logger.error("Failed to send error message", ex);
            }
        }
    }
     
    
    // Updated method to pass transcribed text to other services
    private void passTranscribedTextToOtherServices(String sessionId, String transcript) {
        // Example: You can store it, pass it to other methods, or trigger another process
        logger.info("Passing transcribed text from session {}: {}", sessionId, transcript);
    
        // // Example of passing to another service
        // otherService.processTranscription(sessionId, transcript);
    
        // // Or send it to a different endpoint
        // externalEndpoint.sendTranscription(sessionId, transcript);
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

            // Upload to S3
            String response = s3Service.uploadFile(convertedFile.getAbsolutePath(), keyName);

            convertedFile.delete(); // Cleanup temp file

            // Download processed file
            File responseFile = s3Service.downloadFileFromS3("dta-root", keyName);

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

}
