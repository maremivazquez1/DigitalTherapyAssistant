package harvard.capstone.digitaltherapy.burnout.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.burnout.service.BurnoutFhirService;
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
import java.util.List;

@Controller
public class BurnoutController {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutController.class);

    private final ObjectMapper objectMapper;
    private final BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator;
    private final S3Utils s3Service;
    private final BurnoutFhirService burnoutFhirService; // Keeping the service in the class

    @Autowired
    public BurnoutController(ObjectMapper objectMapper,
                             BurnoutAssessmentOrchestrator burnoutAssessmentOrchestrator,
                             S3Utils s3Service,
                             BurnoutFhirService burnoutFhirService) {
        this.objectMapper = objectMapper;
        this.burnoutAssessmentOrchestrator = burnoutAssessmentOrchestrator;
        this.s3Service = s3Service;
        this.burnoutFhirService = burnoutFhirService; // Still injecting it for future use
    }

    /**
     * Routes incoming WebSocket JSON messages to the appropriate handler
     * based on the "type" field in the message.
     *
     * @param session the WebSocket session origin
     * @param requestJson the parsed incoming JSON message
     * @throws IOException if message sending fails
     */
    public void handleMessage(WebSocketSession session, JsonNode requestJson) throws IOException {
        String messageType = requestJson.has("type") ? requestJson.get("type").asText() : "unknown";

        switch (messageType) {
            case "start-burnout" -> startBurnoutSession(session, requestJson);
            case "answer" -> handleUserAnswer(session, requestJson);
            case "assessment-complete" -> handleCompleteAssessment(session, requestJson);
            default -> logger.warn("Unhandled message type: {}", messageType);
        }
    }

    /**
     * Begins a new burnout assessment session, fetches all questions,
     * and sends them back to the frontend client.
     *
     * @param session the WebSocket session origin
     * @param requestJson the request payload containing user ID
     * @throws IOException if response message sending fails
     */
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

    /**
     * Records user input responses with the burnout assessment orchestrator
     *
     * @param session the WebSocket session oroign
     * @param requestJson the request payload containing burnout session ID, question ID, and user response
     */
    private void handleUserAnswer(WebSocketSession session, JsonNode requestJson) throws IOException {
        String burnoutSessionId = requestJson.get("sessionId").asText();
        String questionId = requestJson.get("questionId").asText();
        String response = requestJson.get("response").asText();

        boolean recorded = burnoutAssessmentOrchestrator.recordResponse(burnoutSessionId, questionId, response, null, null);

        if (recorded) {
            logger.info("Response recorded for session {}, question {}", burnoutSessionId, questionId);
        } else {
            sendErrorMessage(session, "handleUserAnswer_recordResponse", null);
        }
    }

    /**
     * Handles a binary audio message, uploads the file to S3,
     * and records the audio URL in the assessment session.
     *
     * @param session the WebSocket session origin
     * @param sessionId the assessment session ID
     * @param questionId the question ID this audio relates to
     * @param message the binary audio data
     */
    public void handleAudioMessage(WebSocketSession session, String sessionId, String questionId, BinaryMessage message) {
        try {
            String s3Key = "audio_" + sessionId + "_" + questionId + ".mp3";
            String audioUrl = s3Service.uploadAudioBinaryFile(message, s3Key);
            burnoutAssessmentOrchestrator.recordResponse(sessionId, questionId, "", null, audioUrl);
            logger.info("Audio uploaded and recorded for session {}, question {}", sessionId, questionId);
        } catch (Exception e) {
            sendErrorMessage(session, "handleAudioMessage", e);
        }
    }

    /**
     * Handles a binary video message, uploads the file to S3,
     * and records the video URL in the assessment session.
     *
     * @param session the WebSocket session origin
     * @param sessionId the assessment session ID
     * @param questionId the question ID this video relates to
     * @param message the binary video data
     */
    public void handleVideoMessage(WebSocketSession session, String sessionId, String questionId, BinaryMessage message) {
        try {
            String s3Key = "video_" + sessionId + "_" + questionId + ".mp4";
            String videoUrl = s3Service.uploadVideoBinaryFile(message, s3Key);
            burnoutAssessmentOrchestrator.recordResponse(sessionId, questionId, "", videoUrl, null);
            logger.info("Video uploaded and recorded for session {}, question {}", sessionId, questionId);
        } catch (Exception e) {
            sendErrorMessage(session, "handleVideoMessage", e);
        }
    }

    /**
     * Finalizes the burnout assessment, calculates the results,
     * and sends the final score and summary back to the frontend client.
     *
     * @param session the WebSocket session origin
     * @param requestJson the request payload containing sessionId
     */
    private void handleCompleteAssessment(WebSocketSession session, JsonNode requestJson) {
        try {
            String sessionId = requestJson.get("sessionId").asText();
            BurnoutAssessmentResult result = burnoutAssessmentOrchestrator.completeAssessment(sessionId);

            // Comment out FHIR service usage but keep the code for future reference
            // String fhirDocumentUrl = burnoutFhirService.processAndStoreAssessment(result);

            // Use a placeholder instead
            String fhirDocumentUrl = "fhir-document-not-stored"; // Placeholder value
            logger.info("FHIR document storage skipped as requested");

            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "assessment-result");
            response.put("sessionId", sessionId);
            response.put("score", result.getScore().getOverallScore());
            response.put("summary", result.getSummary());

            // uncomment to include in the response.
//            response.put("fhirDocumentUrl", fhirDocumentUrl);

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            logger.info("Assessment completed. FHIR document storage skipped.");
        } catch (Exception e) {
            sendErrorMessage(session, "handleCompleteAssessment", e);
        }
    }

    /**
     * Sends a structured error message to the client over WebSocket,
     * including a reference to the method of origin and exception details.
     *
     * @param session the WebSocket session origin
     * @param origin the name of the method where the error occurred
     * @param exception the exception thrown (can be null)
     */
    private void sendErrorMessage(WebSocketSession session, String origin, Exception exception) {
        String exceptionMsg = (exception == null) ? "unknown" : exception.getMessage();
        logger.error("BurnoutController_{} Error: {}", origin, exceptionMsg);
        try {
            ObjectNode errorJson = objectMapper.createObjectNode();
            errorJson.put("requestId", "BurnoutController_" + origin);
            errorJson.put("error", exceptionMsg);
            session.sendMessage(new TextMessage(errorJson.toString()));
        } catch (Exception e) {
            logger.error("BurnoutController sendErrorMessage failed: {}", e.getMessage(), e);
        }
    }
}