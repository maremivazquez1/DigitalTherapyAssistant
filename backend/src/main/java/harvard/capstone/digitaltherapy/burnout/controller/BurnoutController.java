package harvard.capstone.digitaltherapy.burnout.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.burnout.orchestration.BurnoutAssessmentOrchestrator;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutResult;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.websocket.BurnoutWebSocketHandler;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Controller
public class BurnoutController {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutController.class);

    private final ObjectMapper objectMapper;
    private final BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator;
    private final BurnoutWebSocketHandler burnoutWebSocketHandler;
    private final S3Utils s3Service;

    @Autowired
    public BurnoutController(ObjectMapper objectMapper,
                              BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator,
                              BurnoutWebSocketHandler burnoutWebSocketHandler,
                              S3Utils s3Service) {
        this.objectMapper = objectMapper;
        this.burnoutAssessmentOrchestrator = burnoutAssessmentOrchestrator;
        this.burnoutWebSocketHandler = burnoutWebSocketHandler;
        this.s3Service = s3Service;
    }

    public void handleMessage(WebSocketSession session, JsonNode requestJson) throws IOException {
        String messageType = requestJson.has("type") ? requestJson.get("type").asText() : "unknown";
        String requestId = requestJson.has("requestId") ? requestJson.get("requestId").asText() : "unknown";

        switch (messageType) {
            case "start-burnout" -> startBurnoutSession(session, requestId);
            case "answer" -> handleUserAnswer(session, requestJson);
            default -> logger.warn("Unhandled message type: {}", messageType);
        }
    }

    private void startBurnoutSession(WebSocketSession session, String requestId) throws IOException {
        String userId = "TODO"; // You can connect this to auth later
        String burnoutSessionId = burnoutAssessmentOrchestrator.createAssessmentSession(userId);

        List<BurnoutQuestion> questions = burnoutAssessmentOrchestrator.getSession(burnoutSessionId).getAssessment().getQuestions();

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "burnout-questions");
        response.put("requestId", requestId);
        response.put("sessionId", burnoutSessionId);
        response.set("questions", objectMapper.valueToTree(questions));

        session.sendMessage(new TextMessage(response.toString()));
        logger.info("Started burnout session: {}", burnoutSessionId);
    }

    private void handleUserAnswer(WebSocketSession session, JsonNode requestJson) throws IOException {
        String burnoutSessionId = requestJson.get("sessionId").asText();
        String questionId = requestJson.get("questionId").asText();
        String response = requestJson.get("response").asText();
        String responseType = requestJson.get("responseType").asText();

        String audioUrl = null;
        String videoUrl = null;

        if ("audio".equalsIgnoreCase(responseType)) {
            String audioFileName = "video_" + session.getId() + ".mp4";
            // audioUrl = s3Service.uploadVideoBinaryFile(response, audioFileName);
        } else if ("vlog".equalsIgnoreCase(responseType)) {
            // TODO: Upload and get Video URL
            String videoFileName = "video_" + session.getId() + ".mp4";
            // videoUrl = s3Service.uploadVideoBinaryFile(response, videoFileName);
        }

        boolean recorded = burnoutAssessmentOrchestrator.recordResponse(burnoutSessionId, questionId, response, videoUrl, audioUrl);

        if (recorded) {
            logger.info("Response recorded for session {}, question {}", burnoutSessionId, questionId);
        } else {
            logger.error("Failed to record response for session {}, question {}", burnoutSessionId, questionId);
        }
    }

    // public void handleBinaryMessage(WebSocketSession session, BinaryMessage message)

    // Called by Orchestrator to send final results
    public void forwardFinalBurnoutResult(String burnoutSessionId, BurnoutResult burnoutResult) {
        try {
            WebSocketSession session = burnoutWebSocketHandler.getSession(burnoutSessionId);
            if (session == null || !session.isOpen()) {
                logger.error("No active WebSocket session for sessionId: {}", burnoutSessionId);
                return;
            }

            ObjectNode finalMessage = objectMapper.createObjectNode();
            finalMessage.put("type", "final-assessment-result");
            finalMessage.put("sessionId", burnoutSessionId);
            finalMessage.set("result", objectMapper.valueToTree(burnoutResult));

            session.sendMessage(new TextMessage(finalMessage.toString()));
            logger.info("Sent final burnout result to frontend for session {}", burnoutSessionId);
        } catch (Exception e) {
            logger.error("Error sending final burnout result: {}", e.getMessage(), e);
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
