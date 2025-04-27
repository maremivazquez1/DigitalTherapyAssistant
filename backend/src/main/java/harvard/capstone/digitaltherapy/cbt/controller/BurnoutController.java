package harvard.capstone.digitaltherapy.cbt.controller;

import harvard.capstone.digitaltherapy.cbt.service.OrchestrationService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class BurnoutController {

    private static final Logger logger = LoggerFactory.getLogger(BurnoutController.class);

    private final ObjectMapper objectMapper;
    private final OrchestrationService orchestrationService;

// region: QuestionResponse
    private final Map<String, List<QuestionResponse>> sessionResponses = new HashMap<>();
    public class QuestionResponse {
        private int id;
        private String type; // "likert" | "open_text" | "vlog" | "audio"
        private String content;
        private String subtitle;
        private String userResponse; // user input or LLM-evaluated media result
        private boolean answered;

        public void setId(int id){this.id = id;}
        public void setType(String type){this.type = type;}
        public void setContent(String content){this.content = content;}
        public void setSubtitle(String subtitle){this.subtitle = subtitle;}
        public void setUserResponse(String userResponse){this.userResponse = userResponse;}
        public void setAnswered(boolean answered){this.answered = answered;}
        public int getId(){return id;}
        public String getType(){return type;}
        public String getContent(){return content;}
        public String getSubtitle(){return subtitle;}
        public String getUserResponse(){return userResponse;}
        public boolean getAnswered(){return answered;}
    }

    private List<QuestionResponse> parseQuestions(String questionsJson) throws IOException {
        JsonNode questionsArray = objectMapper.readTree(questionsJson);
        List<QuestionResponse> questions = new ArrayList<>();

        for (JsonNode questionNode : questionsArray) {
            QuestionResponse qr = new QuestionResponse();
            qr.setId(questionNode.get("id").asInt());
            qr.setType(questionNode.get("type").asText());
            qr.setContent(questionNode.get("content").asText());
            if (questionNode.has("subtitle")) {
                qr.setSubtitle(questionNode.get("subtitle").asText());
            } else {
                qr.setSubtitle(""); // Safe default
            }
            qr.setAnswered(false);
            qr.setUserResponse(null);
            questions.add(qr);
        }
        return questions;
    }
// endregion: QuestionResponse

    @Autowired
    public BurnoutController(ObjectMapper objectMapper, OrchestrationService orchestrationService) {
        this.objectMapper = objectMapper;
        this.orchestrationService = orchestrationService;
    }

    public void handleMessage(WebSocketSession session, JsonNode requestJson, String messageType, String requestId) throws IOException {
        if ("start-burnout".equals(messageType)) {
            startBurnoutSession(session, requestId);
        } else if ("answer".equals(messageType)) {
            handleUserAnswer(session, requestJson, requestId);
        }else {
            // Future handling for other types of messages
        }
    }

    private void startBurnoutSession(WebSocketSession session, String requestId) throws IOException {
        String sessionId = session.getId();
        orchestrationService.associateBurnoutSession(sessionId);

        
        // (Later we will fetch real questions here, right now just send back a dummy)
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "burnout-questions");
        response.put("requestId", requestId);
        response.put("sessionId", sessionId); // include it if frontend wants to track
        String questionsJson = orchestrationService.getBurnoutQuestions();
        List<QuestionResponse> questions = parseQuestions(questionsJson); // we build this helper
        sessionResponses.put(sessionId, questions);
        response.set("questions", objectMapper.readTree(questionsJson));

        session.sendMessage(new TextMessage(response.toString()));
    }

    private void handleUserAnswer(WebSocketSession session, JsonNode requestJson, String requestId) throws IOException {
        String sessionId = session.getId();
        int questionId = requestJson.get("questionId").asInt();
        String userResponse = requestJson.get("response").asText(); // Usually the S3 path for media
        String responseType = requestJson.get("responseType").asText(); // "likert", "open_text", "vlog", "audio"
    
        List<QuestionResponse> responses = sessionResponses.get(sessionId);
    
        for (QuestionResponse qr : responses) {
            if (qr.getId() == questionId) {
                if ("likert".equalsIgnoreCase(qr.getType()) || "open_text".equalsIgnoreCase(qr.getType())) {
                    // Direct value input (number or text)
                    qr.setUserResponse(userResponse);
                    qr.setAnswered(true);
                    checkIfSessionComplete(session);
                } else if ("vlog".equalsIgnoreCase(qr.getType()) || "audio".equalsIgnoreCase(qr.getType())) {
                    // Media input - needs async processing
                    CompletableFuture.runAsync(() -> {
                        try {
                            Map<String, String> modalities = new HashMap<>();
                            modalities.put(responseType.toLowerCase(), userResponse); // "audio" or "vlog" mapped to S3 path
    
                            String evaluatedResult = orchestrationService.processBurnoutMedia(sessionId, modalities);
                            qr.setUserResponse(evaluatedResult);
                            qr.setAnswered(true);
    
                            checkIfSessionComplete(session);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Optionally set error response if needed
                            qr.setUserResponse("Error processing media: " + e.getMessage());
                            qr.setAnswered(true);
                        }
                    });
                }
                break;
            }
        }
    }    

    private void checkIfSessionComplete(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        List<QuestionResponse> responses = sessionResponses.get(sessionId);
    
        if (responses != null && responses.stream().allMatch(QuestionResponse::getAnswered)) {
            logger.info("All questions answered for session {}", sessionId);
    
            // You can call orchestrator to start final analysis here
            CompletableFuture.runAsync(() -> {
                try {
                    // TODO: Send completed responses to orchestrator->burnoutworker for final evaluation
                    String finalEvaluation = orchestrationService.finalizeBurnoutEvaluation(sessionId, responses);
                    handleFinalEvaluation(session, finalEvaluation);
                } catch (Exception e) {
                    logger.error("Error during final burnout evaluation for session {}: {}", sessionId, e.getMessage(), e);
                }
            });
        }
    }

    private void handleFinalEvaluation(WebSocketSession session, String finalEvaluationJson) throws IOException {
        ObjectNode finalMessage = objectMapper.createObjectNode();
        finalMessage.put("type", "final-evaluation");
        finalMessage.put("evaluation", finalEvaluationJson);
    
        session.sendMessage(new TextMessage(finalMessage.toString()));
    }
    
}

