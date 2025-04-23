package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.cbt.controller.CBTController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CBTWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CBTWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final CBTController cbtController;

    // Store active sessions
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public CBTWebSocketHandler(ObjectMapper objectMapper, CBTController cbtController) {
        this.objectMapper = objectMapper;
        this.cbtController = cbtController;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        // Retrieve the username from handshake attributes (set by JwtHandshakeInterceptor)
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            logger.info("New WebSocket connection established for user {} with session: {}", username, sessionId);
        } else {
            logger.info("New WebSocket connection established with session: {}", sessionId);
        }

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
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(sessionId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String sessionId = session.getId();
        logger.info("Received message from session {}", sessionId);
        try {
            JsonNode requestJson = objectMapper.readTree(message.getPayload());
            String messageType = requestJson.has("type") ? requestJson.get("type").asText() : "text";
            String requestId = requestJson.has("requestId") ? requestJson.get("requestId").asText() : "unknown";
            cbtController.handleMessage(session, requestJson, messageType, requestId);
        } catch (Exception e) {
            logger.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage(), 500, "unknown");
        }
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        logger.info("Received binary message from session {}", sessionId);
        cbtController.handleBinaryMessage(session, message);
    }

    private void sendErrorMessage(WebSocketSession session, String message, int code, String requestId) throws IOException {
        ObjectNode errorJson = objectMapper.createObjectNode();
        errorJson.put("error", message);
        errorJson.put("code", code);
        errorJson.put("requestId", requestId);
        session.sendMessage(new TextMessage(errorJson.toString()));
    }
}
