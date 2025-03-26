/**
 * WebSocket handler for processing text-based interactions between the frontend and the AWS Bedrock service.
 * Maintains conversation history per session, formats incoming JSON messages, calls the BedrockService,
 * and returns the generated responses back to the client via WebSocket.
 *
 * Note: This implementation is currently text-based and will need to be adapted to support audio-based
 * interactions (e.g., streaming audio from the client, handling STT/TTS processing) for the real application.
 */
package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import harvard.capstone.digitaltherapy.aws.service.BedrockService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BedrockWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BedrockWebSocketHandler.class);
    private final BedrockService bedrockService;
    private final ObjectMapper objectMapper;

    // Store active sessions
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Store conversation history per session
    private final ConcurrentHashMap<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    public BedrockWebSocketHandler(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Store the session
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        conversationHistory.put(sessionId, new ArrayList<>());
        logger.info("WebSocket connection established: {}", sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Remove the session and its conversation history
        String sessionId = session.getId();
        sessions.remove(sessionId);
        conversationHistory.remove(sessionId);
        logger.info("WebSocket connection closed: {}, status: {}", sessionId, status);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        logger.info("Received message from session {}: {}", sessionId, message.getPayload());

        try {
            // Parse the JSON message
            JsonNode requestJson = objectMapper.readTree(message.getPayload());

            // Extract text/prompt from the message
            String text = requestJson.has("text") ? requestJson.get("text").asText() : "";
            String requestId = requestJson.has("requestId") ? requestJson.get("requestId").asText() : "unknown";

            logger.debug("Extracted text: '{}', requestId: '{}'", text, requestId);

            if (text.trim().isEmpty()) {
                sendErrorMessage(session, "Text/prompt cannot be empty", 400, requestId);
                return;
            }

            // Get conversation history for this session
            List<Map<String, String>> history = conversationHistory.get(sessionId);
            if (history == null) {
                history = new ArrayList<>();
                conversationHistory.put(sessionId, history);
            }

            // Add user message to conversation history
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            history.add(userMessage);

            logger.debug("Conversation history before processing: {}", objectMapper.writeValueAsString(history));

            // Process the message with full conversation history
            String response;
            try {
                response = bedrockService.processMessageWithHistory(history);
                logger.debug("Raw response from Bedrock: {}", response);

                if (response == null || response.trim().isEmpty()) {
                    logger.warn("Received empty response from Bedrock service");
                    response = "I'm sorry, I couldn't generate a response at this time.";
                }
            } catch (Exception e) {
                logger.error("Error from Bedrock service: {}", e.getMessage(), e);
                sendErrorMessage(session, "Error processing your request: " + e.getMessage(), 500, requestId);
                return;
            }

            // Add assistant response to conversation history
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", response);
            history.add(assistantMessage);

            logger.debug("Updated conversation history: {}", objectMapper.writeValueAsString(history));

            // Limit history size to prevent token issues (optional)
            if (history.size() > 10) {
                history = history.subList(history.size() - 10, history.size());
                conversationHistory.put(sessionId, history);
            }

            // Create response JSON
            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("requestId", requestId);
            responseJson.put("responseText", response);

            String responseStr = responseJson.toString();
            logger.debug("Sending response: {}", responseStr);

            // Send the response
            session.sendMessage(new TextMessage(responseStr));
            logger.info("Sent response to session {}", sessionId);

        } catch (Exception e) {
            logger.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendErrorMessage(session, "Error processing message: " + e.getMessage(), 500, "unknown");
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", sessionId, ex);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage, int code, String requestId) throws IOException {
        ObjectNode errorJson = objectMapper.createObjectNode();
        errorJson.put("error", errorMessage);
        errorJson.put("code", code);
        errorJson.put("requestId", requestId);

        session.sendMessage(new TextMessage(errorJson.toString()));
    }
}
