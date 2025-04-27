package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.cbt.controller.BurnoutController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BurnoutWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final BurnoutController burnoutController;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public BurnoutWebSocketHandler(ObjectMapper objectMapper, BurnoutController burnoutController) {
        this.objectMapper = objectMapper;
        this.burnoutController = burnoutController;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("Burnout WebSocket connected: session {}", sessionId);

        ObjectNode welcomeMessage = objectMapper.createObjectNode();
        welcomeMessage.put("type", "system");
        welcomeMessage.put("message", "Burnout session connected successfully");
        session.sendMessage(new TextMessage(welcomeMessage.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("Burnout WebSocket closed: session {}, reason: {}", sessionId, status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("Transport error for session {}: {}", sessionId, exception.getMessage(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(sessionId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String sessionId = session.getId();
        logger.info("Received burnout text message from session {}", sessionId);
        try {
            JsonNode requestJson = objectMapper.readTree(message.getPayload());
            String messageType = requestJson.has("type") ? requestJson.get("type").asText() : "text";
            String requestId = requestJson.has("requestId") ? requestJson.get("requestId").asText() : "unknown";
            burnoutController.handleMessage(session, requestJson, messageType, requestId);
        } catch (Exception e) {
            logger.error("Error processing burnout message from session {}: {}", sessionId, e.getMessage(), e);
            sendErrorMessage(session, "Error processing burnout message: " + e.getMessage(), 500, "unknown");
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
