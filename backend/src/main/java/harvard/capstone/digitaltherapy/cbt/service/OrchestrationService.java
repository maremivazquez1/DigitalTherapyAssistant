package harvard.capstone.digitaltherapy.cbt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import harvard.capstone.digitaltherapy.cbt.model.AnalysisResult;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import harvard.capstone.digitaltherapy.workers.*;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Core orchestration component that manages the therapeutic conversation flow
 * and coordinates interactions between different system components.
 */
@Service
public class OrchestrationService {

    private final TextAnalysisWorker textAnalysisWorker;
    private final MessageWorker messageWorker;
    private final VectorDatabaseService vectorDatabaseService;
    private final VideoAnalysisWorker videoAnalysisWorker;
    private final AudioAnalysisWorker audioAnalysisWorker;

    // Simple in-memory session tracking (would use Redis in production)
    private final Map<String, List<ChatMessage>> sessionMessages = new HashMap<>();

    public OrchestrationService(){
        this.textAnalysisWorker = new TextAnalysisWorker();
        this.messageWorker = new MessageWorker();
        this.vectorDatabaseService = new VectorDatabaseService();
        this.videoAnalysisWorker = new VideoAnalysisWorker();
        this.audioAnalysisWorker = new AudioAnalysisWorker();
    }

    public String associateSession(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();

        // Add the initial system message for a CBT therapy context
        messages.add(SystemMessage.from(
                "You are a CBT therapist guiding a patient through a CBT session. " +
                        "Use concise and empathetic language. Focus on helping the patient " +
                        "identify and reframe negative thought patterns."
        ));

        sessionMessages.put(sessionId, messages);
        return sessionId;
    }

    public String processUserMessage(String sessionId, Map<String, String> modalities, String input_transcript) {
        if (!sessionMessages.containsKey(sessionId)) {
            throw new IllegalArgumentException("Invalid session ID: " + sessionId);
        }
        List<ChatMessage> messages = sessionMessages.get(sessionId);
        List<CompletableFuture<Object>> analysisFutures = new ArrayList<>();
        Map<String, CompletableFuture<Object>> modalityToFuture = new HashMap<>();  // To track which result belongs to which modality

        modalities.forEach((modalityType, content) -> {
            CompletableFuture<Object> analysisFuture = switch (modalityType.toLowerCase()) {
                case "text" -> {
                    Map<String, Object> textAnalysis = textAnalysisWorker.analyzeText(input_transcript);
                    yield CompletableFuture.completedFuture(textAnalysis);
                }
                case "video" -> videoAnalysisWorker.detectFacesFromVideoAsync(modalities.get(modalityType))
                        .thenCompose(result -> {
                            Map<String, Object> videoResult = new HashMap<>();
                            videoResult.put("videoAnalysis", result);
                            return CompletableFuture.completedFuture(videoResult);
                        });

                case "audio" -> audioAnalysisWorker.analyzeAudioAsync(modalities.get(modalityType))
                        .thenCompose(result -> {
                            Map<String, Object> audioResult = new HashMap<>();
                            audioResult.put("audioAnalysis", result);
                            return CompletableFuture.completedFuture(audioResult);
                        });

                default -> CompletableFuture.failedFuture(
                        new IllegalArgumentException("Unsupported modality type: " + modalityType)
                );
            };
                analysisFutures.add(analysisFuture);
                modalityToFuture.put(modalityType, analysisFuture);
        });
// Wait for all analysis to complete
        CompletableFuture.allOf(analysisFutures.toArray(new CompletableFuture[0])).join();

// Add analysis results to conversation history
        Map<String, Object> workerResponse = new HashMap<>();
        modalityToFuture.forEach((modalityType, future) -> {
            Object analysisResult = future.join();
            // Convert the analysis result to an appropriate message format
            switch (modalityType.toLowerCase()) {
                case "text" -> {
                    Map<String, Object> textInsights= new HashMap<>();
                    Map<String, Object> textAnalysis = (Map<String, Object>) analysisResult;
                    textInsights.put("wordScore", textAnalysis.getOrDefault("emotionConfidence",0.80));
                    textInsights.put("wordsAnalysis", textAnalysis.getOrDefault("emotion","no analysis"));
                    workerResponse.put("textInsights", textInsights);
                    //yield convertTextAnalysisToString(workerResponse);  // You'll need to implement this
                }
                case "video" -> {
                    Map<String, Object> videoInsights = new HashMap<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> analysisMap = (Map<String, Object>) analysisResult;
                        JsonNode jsonNode = objectMapper.readTree(analysisMap.get("videoAnalysis").toString());
                        // Calculate average confidence score
                        double totalConfidence = 0.0;
                        int count = 0;
                        // Get the faces array
                        JsonNode facesNode = jsonNode.get("faces");
                        if (facesNode != null && facesNode.isArray()) {
                            for (JsonNode faceFrame : facesNode) {
                                JsonNode face = faceFrame.get("face");
                                if (face != null) {
                                    // Add confidence score
                                    totalConfidence += face.get("confidence").asDouble();
                                    count++;
                                }
                            }
                        }
                        // Calculate average score
                        double averageScore = count > 0 ? totalConfidence / count : 0.0;
                        // Set the video score (normalized to 0-1 range)
                        videoInsights.put("videoScore", averageScore / 100.0);
                        // Set the entire analysis as facial analysis
                        videoInsights.put("facialAnalysis", jsonNode.toString());
                    } catch (JsonProcessingException e) {
                        videoInsights.put("videoScore", 0.0);
                        videoInsights.put("facialAnalysis", "Error analyzing facial expressions");
                    }
                    workerResponse.put("videoInsights", videoInsights);
                    //yield convertTextAnalysisToString(videoInsights);
                }

                case "audio" -> {
                    Map<String, Object> voiceInsights = new HashMap<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> analysisMap = (Map<String, Object>) analysisResult;
                        JsonNode jsonNode = objectMapper.readTree(analysisMap.get("audioAnalysis").toString());
                        // Extract confidence scores
                        double totalConfidence = 0.0;
                        int count = 0;
                        // Since the response is an array, get the first element
                        if (jsonNode.isArray() && jsonNode.size() > 0) {
                            JsonNode firstAnalysis = jsonNode.get(0);
                            // Navigate to prosody confidence
                            if (firstAnalysis.has("results") &&
                                    firstAnalysis.get("results").has("predictions") &&
                                    firstAnalysis.get("results").get("predictions").isArray()) {

                                JsonNode prediction = firstAnalysis.get("results").get("predictions").get(0);
                                if (prediction.has("models") && prediction.get("models").has("prosody")) {
                                    JsonNode prosody = prediction.get("models").get("prosody");

                                    // Get overall confidence from metadata
                                    if (prosody.has("metadata") && prosody.get("metadata").has("confidence")) {
                                        totalConfidence += prosody.get("metadata").get("confidence").asDouble();
                                        count++;
                                    }
                                    // Get confidence from individual predictions
                                    if (prosody.has("grouped_predictions") && prosody.get("grouped_predictions").isArray()) {
                                        for (JsonNode group : prosody.get("grouped_predictions")) {
                                            if (group.has("predictions") && group.get("predictions").isArray()) {
                                                for (JsonNode pred : group.get("predictions")) {
                                                    if (pred.has("confidence")) {
                                                        totalConfidence += pred.get("confidence").asDouble();
                                                        count++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Calculate average confidence score
                        double averageScore = count > 0 ? totalConfidence / count : 0.0;
                        voiceInsights.put("audioScore", averageScore);
                        voiceInsights.put("toneAnalysis", jsonNode.toString());

                    } catch (JsonProcessingException e) {
                        voiceInsights.put("audioScore", 0.0);
                        voiceInsights.put("toneAnalysis", "Error analyzing audio");
                    }
                    workerResponse.put("voiceInsights", voiceInsights);
                }
            };
            vectorDatabaseService.indexSessionMessage(sessionId, "user", convertTextAnalysisToString(workerResponse), false);
        });
        // Create AgentState with this initial map
        AgentState state = new AgentState(workerResponse);
        // Instantiate the node
        MultiModalSynthesizer node = new MultiModalSynthesizer();
        // Apply the node
        Map<String, Object> result = node.apply(state);
        // Retrieve and print the final Analysis
        AnalysisResult analysis = (AnalysisResult) result.get("multimodalAnalysis");
        System.out.println("Multimodal Analysis: " + analysis);
        // 5. Generate the response using the MessageWorker
        messages.add(UserMessage.from(convertTextAnalysisToString(workerResponse)));
        messages.add(UserMessage.from(analysis.toString()));
        String response = messageWorker.generateResponse(messages);
        vectorDatabaseService.indexSessionMessage(sessionId, "user", response, false);
        return response;
    }

    private String convertTextAnalysisToString(Map<String, Object> textAnalysis) {
        // Implement the conversion logic based on your text analysis structure
        StringBuilder result = new StringBuilder();
        textAnalysis.forEach((key, value) ->
                result.append(key).append(": ").append(value).append("\n")
        );
        return result.toString();
    }

}
