package harvard.capstone.digitaltherapy.websocket;

import harvard.capstone.digitaltherapy.burnout.controller.BurnoutController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BurnoutWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final BurnoutController burnoutController;
    private static class PendingUploadContext {
        public String type;
        public String sessionId;
        public String questionId;
    }
    
    private final Map<String, PendingUploadContext> pendingUploads = new ConcurrentHashMap<>();
    
    @Autowired
    public BurnoutWebSocketHandler(ObjectMapper objectMapper, BurnoutController burnoutController) {
        this.objectMapper = objectMapper;
        this.burnoutController = burnoutController;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("WebSocket disconnected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode requestJson = objectMapper.readTree(message.getPayload());
        String type = requestJson.has("type") ? requestJson.get("type").asText() : "";

        if ("video_upload".equalsIgnoreCase(type) || "audio_upload".equalsIgnoreCase(type)) {
            PendingUploadContext context = new PendingUploadContext();
            context.type = type;
            context.sessionId = requestJson.get("sessionId").asText();
            context.questionId = requestJson.get("questionId").asText();
            pendingUploads.put(session.getId(), context);

            logger.info("Preparing to receive {} for session {} question {}", type, context.sessionId, context.questionId);
            return;
        }

        // fallback to standard message handling
        burnoutController.handleMessage(session, requestJson);
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionKey = session.getId();
        PendingUploadContext context = pendingUploads.remove(sessionKey);

        if (context == null) {
            logger.error("No pending upload context for session {}", sessionKey);
            return;
        }

        try {
            if ("video_upload".equalsIgnoreCase(context.type)) {
                burnoutController.handleVideoMessage(session, context.sessionId, context.questionId, message);
            } else if ("audio_upload".equalsIgnoreCase(context.type)) {
                burnoutController.handleAudioMessage(session, context.sessionId, context.questionId, message);
            }
        } catch (Exception e) {
            logger.error("Error handling binary message for session {}: {}", sessionKey, e.getMessage(), e);
        }
    }
}
