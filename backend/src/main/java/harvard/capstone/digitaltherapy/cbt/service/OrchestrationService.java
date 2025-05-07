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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private String userId;

    public OrchestrationService(){
        this.textAnalysisWorker = new TextAnalysisWorker();
        this.messageWorker = new MessageWorker();
        this.vectorDatabaseService = new VectorDatabaseService();
        this.videoAnalysisWorker = new VideoAnalysisWorker();
        this.audioAnalysisWorker = new AudioAnalysisWorker();
    }

    public void setSessionContext(String sessionId, String userId) {
        this.userId = userId;
        messageWorker.setSessionContext(sessionId, userId);
    }

    public String processUserMessage(String sessionId, Map<String, String> modalities, String input_transcript) {
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
                    textInsights.put("wordsAnalysis", textAnalysis.getOrDefault("emotion","no analysis"));
                    workerResponse.put("textInsights", textInsights);
                }
                case "video" -> {
                    Map<String, Object> videoInsights = new HashMap<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> analysisMap = (Map<String, Object>) analysisResult;
                        JsonNode jsonNode = objectMapper.readTree(analysisMap.get("videoAnalysis").toString());
                        videoInsights.put("facialAnalysis", jsonNode.toString());
                    } catch (JsonProcessingException e) {
                        videoInsights.put("facialAnalysis", "Error analyzing facial expressions");
                    }
                    workerResponse.put("videoInsights", videoInsights);
                }

                case "audio" -> {
                    Map<String, Object> voiceInsights = new HashMap<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> analysisMap = (Map<String, Object>) analysisResult;
                        JsonNode jsonNode = objectMapper.readTree(analysisMap.get("audioAnalysis").toString());
                        voiceInsights.put("toneAnalysis", jsonNode.toString());
                    } catch (JsonProcessingException e) {
                        voiceInsights.put("toneAnalysis", "Error analyzing audio");
                    }
                    workerResponse.put("voiceInsights", voiceInsights);
                }
            };
            vectorDatabaseService.indexSessionMessage(sessionId, userId, convertTextAnalysisToString(workerResponse), false);
        });
        // Create AgentState with this initial map
        AgentState state = new AgentState(workerResponse);
        // Instantiate the node
        MultiModalSynthesizer node = new MultiModalSynthesizer();
        // Apply the node
        Map<String, Object> result = node.apply(state);
        // Retrieve and print the final Analysis
        AnalysisResult analysis = (AnalysisResult) result.get("multimodalAnalysis");
        // Send analysis and user response to the message worker
        String response = messageWorker.generateResponse(analysis.toString(), input_transcript);
        vectorDatabaseService.indexSessionMessage(sessionId, userId, response, false);
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
