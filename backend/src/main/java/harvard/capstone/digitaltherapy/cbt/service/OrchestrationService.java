package harvard.capstone.digitaltherapy.cbt.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;
import harvard.capstone.digitaltherapy.orchestration.MultimodalSynthesisService;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import harvard.capstone.digitaltherapy.workers.MessageWorker;
import harvard.capstone.digitaltherapy.workers.TextAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
/**
 * Core orchestration component that manages the therapeutic conversation flow
 * and coordinates interactions between different system components.
 */
@Service
public class OrchestrationService {

    private final TextAnalysisWorker textAnalysisWorker;
    private final MessageWorker messageWorker;
    private final MultimodalSynthesisService synthesisService;
    private final VectorDatabaseService vectorDatabaseService;
    private final VideoAnalysisWorker videoAnalysisWorker;
    private final AudioAnalysisWorker audioAnalysisWorker;

    // Simple in-memory session tracking (would use Redis in production)
    private final Map<String, List<ChatMessage>> sessionMessages = new HashMap<>();

    public OrchestrationService() {
        this.textAnalysisWorker = new TextAnalysisWorker();
        this.messageWorker = new MessageWorker();
        this.synthesisService = new MultimodalSynthesisService();
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
        modalityToFuture.forEach((modalityType, future) -> {
            Object analysisResult = future.join();
            // Convert the analysis result to an appropriate message format
            String analysisContent = switch (modalityType.toLowerCase()) {
                case "text" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> textAnalysis = (Map<String, Object>) analysisResult;
                    yield convertTextAnalysisToString(textAnalysis);  // You'll need to implement this
                }
                case "video" -> {
                    // Convert video analysis result to string
                    yield String.valueOf(analysisResult);
                }
                case "audio" -> {
                    // Convert audio analysis result to string
                    yield String.valueOf(analysisResult);
                }
                default -> "Unsupported modality type: " + modalityType;
            };

            messages.add(UserMessage.from(analysisContent));
        });
        // 5. Generate the response using the MessageWorker
        String response = messageWorker.generateResponse(messages);
        // 6. Add the assistant's response to the conversation history
        messages.add(SystemMessage.from(response));
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
