package harvard.capstone.digitaltherapy.burnout.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutSessionCreationResponse;
import harvard.capstone.digitaltherapy.burnout.orchestration.BurnoutAssessmentOrchestrator;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessmentResult;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.List;

@Controller
public class BurnoutController {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutController.class);

    private final ObjectMapper objectMapper;
    private final BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator;
    private final S3Utils s3Service;

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

        switch (messageType) {
            case "start-burnout" -> startBurnoutSession(session, requestJson);
            case "answer" -> handleUserAnswer(session, requestJson);
            case "assessment-complete" -> handleCompleteAssessment(session, requestJson);
            default -> logger.warn("Unhandled message type: {}", messageType);
        }
    }

    private void startBurnoutSession(WebSocketSession session, JsonNode requestJson) throws IOException {
        String userId = requestJson.has("userId") ? requestJson.get("userId").asText() : "unknown";
        BurnoutSessionCreationResponse responseData = burnoutAssessmentOrchestrator.createAssessmentSession(userId);

        // This will be used to track the session.
        String burnoutSessionId = responseData.getSessionId();

        // you now have all questions.
        List<BurnoutQuestion> questions = responseData.getQuestions();

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "burnout-questions");
        response.put("sessionId", burnoutSessionId);
        response.set("questions", objectMapper.valueToTree(questions));

        session.sendMessage(new TextMessage(response.toString()));
        logger.info("Started burnout session: {}", burnoutSessionId);
    }

    private void handleUserAnswer(WebSocketSession session, JsonNode requestJson) throws IOException {
        String burnoutSessionId = requestJson.get("sessionId").asText();
        String questionId = requestJson.get("questionId").asText();
        String response = requestJson.get("response").asText();

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

    private void handleCompleteAssessment(WebSocketSession session, JsonNode requestJson){
        try {
            String sessionId = requestJson.get("sessionId").asText();
            BurnoutAssessmentResult result = burnoutAssessmentOrchestrator.completeAssessment(sessionId);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "assessment-result");
            response.put("sessionId", sessionId);
            response.put("score", result.getScore().getOverallScore());
            response.put("summary", result.getSummary());

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            logger.error("Failed to complete assessment: {}", e.getMessage(), e);
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
