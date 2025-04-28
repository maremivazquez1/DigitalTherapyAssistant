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

    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final BurnoutController burnoutController;

    @Autowired
    public BurnoutWebSocketHandler(ObjectMapper objectMapper, BurnoutController burnoutController) {
        this.objectMapper = objectMapper;
        this.burnoutController = burnoutController;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSessions.put(session.getId(), session);
        logger.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session.getId());
        logger.info("WebSocket disconnected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode requestJson = objectMapper.readTree(message.getPayload());
        burnoutController.handleMessage(session, requestJson);
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        logger.info("Received binary message from session {}", sessionId);
        // burnoutController.handleBinaryMessage(session, message);
    }

    public WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
}
