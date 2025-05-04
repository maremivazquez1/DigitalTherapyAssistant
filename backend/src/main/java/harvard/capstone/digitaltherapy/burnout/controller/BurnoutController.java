package harvard.capstone.digitaltherapy.burnout.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutSessionCreationResponse;
import harvard.capstone.digitaltherapy.burnout.orchestration.BurnoutAssessmentOrchestrator;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessmentResult;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.websocket.WebSocketSessionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Controller
public class BurnoutController {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutController.class);

    private final ObjectMapper objectMapper;
    private final BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator;
    private final S3Utils s3Service;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    public BurnoutController(ObjectMapper objectMapper,
                              BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator,
                              S3Utils s3Service) {
        this.objectMapper = objectMapper;
        this.burnoutAssessmentOrchestrator = burnoutAssessmentOrchestrator;
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
        BurnoutSessionCreationResponse responseData = burnoutAssessmentOrchestrator.createAssessmentSession(userId);

        // This will be used to track the session.
        String burnoutSessionId = responseData.getSessionId();

        // you now have all questions.
        List<BurnoutQuestion> questions = responseData.getQuestions();

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

        // TODO: Change to the sessionID
        boolean recorded = burnoutAssessmentOrchestrator.recordResponse(burnoutSessionId, questionId, response, null, null);

        if (recorded) {
            logger.info("Response recorded for session {}, question {}", burnoutSessionId, questionId);
        } else {
            logger.error("Failed to record response for session {}, question {}", burnoutSessionId, questionId);
        }
    }

    public void handleAudioMessage(WebSocketSession session, String sessionId, String questionId, BinaryMessage message) {
        try {
            String s3Key = "audio_" + sessionId + "_" + questionId + ".mp3";
            String audioUrl = s3Service.uploadAudioBinaryFile(message, s3Key);
            burnoutAssessmentOrchestrator.recordResponse(sessionId, questionId, "", null, audioUrl);
            logger.info("Audio uploaded and recorded for session {}, question {}", sessionId, questionId);
        } catch (Exception e) {
            logger.error("Failed to handle audio upload: {}", e.getMessage(), e);
        }
    }

    public void handleVideoMessage(WebSocketSession session, String sessionId, String questionId, BinaryMessage message) {
        try {
            String s3Key = "video_" + sessionId + "_" + questionId + ".mp4";
            String videoUrl = s3Service.uploadVideoBinaryFile(message, s3Key);
            burnoutAssessmentOrchestrator.recordResponse(sessionId, questionId, "", videoUrl, null);
            logger.info("Video uploaded and recorded for session {}, question {}", sessionId, questionId);
        } catch (Exception e) {
            logger.error("Failed to handle video upload: {}", e.getMessage(), e);
        }
    }


    // TODO: Change to call the Orch method.
    // Called by Orchestrator to send final results
    public void forwardFinalBurnoutResult(String burnoutSessionId, BurnoutAssessmentResult burnoutResult) {
        try {
            WebSocketSession session = sessionManager.getSession(burnoutSessionId);
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
