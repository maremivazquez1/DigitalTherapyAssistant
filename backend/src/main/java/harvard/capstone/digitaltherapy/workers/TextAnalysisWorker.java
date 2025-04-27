package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * TextAnalysisWorker
 *
 * Specialized worker that analyzes text inputs to extract insights such as:
 * - Emotional states and sentiment
 * - Cognitive distortions (according to CBT principles)
 * - Key themes and concerns
 * - Linguistic patterns
 *
 * This worker uses LLM capabilities to perform analysis and returns structured
 * insights that can be used by the MultimodalSynthesisService.
 */
public class TextAnalysisWorker {

    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService;
    private String sessionId;
    private String userId;

    private static class AnalysisResult<T> {
        private final T result;
        private final double confidence;

        public AnalysisResult(T result, double confidence) {
            this.result = result;
            this.confidence = confidence;
        }

        public T getResult() {
            return result;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public TextAnalysisWorker() {
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.1) // Lower temperature for analysis tasks
                .topP(0.95)
                .maxOutputTokens(300)
                .build();
        this.vectorDatabaseService = new VectorDatabaseService();
    }

    public void setSessionContext(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }

    /**
     * Analyzes text input to extract emotional state, cognitive distortions,
     * and other relevant insights
     *
     * @param userInput The user's text input to analyze
     * @return A map containing the analysis results
     */
    public Map<String, Object> analyzeText(String userInput) {
        Map<String, Object> analysis = new HashMap<>();

        // Perform basic analysis
        AnalysisResult<String> emotionResult = detectEmotion(userInput);
        analysis.put("emotion", emotionResult.getResult());
        analysis.put("emotionConfidence", emotionResult.getConfidence());

        AnalysisResult<List<String>> distortionsResult = detectCognitiveDistortions(userInput);
        analysis.put("cognitiveDistortions", distortionsResult.getResult());
        analysis.put("cognitiveDistortionsConfidence", distortionsResult.getConfidence());

        AnalysisResult<List<String>> themesResult = extractKeyThemes(userInput);
        analysis.put("keyThemes", themesResult.getResult());
        analysis.put("keyThemesConfidence", themesResult.getConfidence());

        if (userId != null && sessionId != null) {
            Map<String, Object> metadataFilters = new HashMap<>();
            metadataFilters.put("userId", userId);
            metadataFilters.put("contentType", "analysis");

            List<EmbeddingMatch<TextSegment>> similarAnalyses =
                    vectorDatabaseService.findSimilarContent(userInput, metadataFilters, 3);

            if (!similarAnalyses.isEmpty()) {
                analysis.put("recurringPatterns", extractRecurringPatterns(similarAnalyses, distortionsResult.getResult()));
            }
        }

        analysis.put("analysisTimestamp", System.currentTimeMillis());
        return analysis;
    }

    /**
     * Detects the primary emotion expressed in the text
     *
     * @param text The text to analyze
     * @return AnalysisResult containing the detected primary emotion and confidence score
     */
    private AnalysisResult<String> detectEmotion(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are an emotion detection system. Analyze the text and identify the primary emotion expressed. Respond with the emotion and confidence score in format 'emotion|confidence' where emotion is one of: anger, fear, joy, sadness, surprise, disgust, neutral, anxiety, shame, guilt. Confidence should be between 0 and 1."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        String[] parts = response.aiMessage().text().trim().toLowerCase().split("\\|");
        return new AnalysisResult<>(parts[0], Double.parseDouble(parts[1]));
    }

    /**
     * Detects cognitive distortions in the text according to CBT principles
     *
     * @param text The text to analyze
     * @return AnalysisResult containing the list of cognitive distortions and confidence score
     */
    private AnalysisResult<List<String>> detectCognitiveDistortions(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are a CBT analysis system. Identify cognitive distortions in the text according to CBT principles. Respond with distortions and confidence score in format 'distortions|confidence' where distortions are comma-separated. If none present, respond with 'none|confidence'. Common distortions: all-or-nothing thinking, overgeneralization, mental filter, disqualifying the positive, jumping to conclusions, magnification, emotional reasoning, should statements, labeling, personalization."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        String[] parts = response.aiMessage().text().trim().toLowerCase().split("\\|");
        List<String> distortions = parts[0].equals("none") ?
                List.of() :
                Arrays.asList(parts[0].split("\\s*,\\s*"));
        return new AnalysisResult<>(distortions, Double.parseDouble(parts[1]));
    }

    /**
     * Extracts key themes from the text
     *
     * @param text The text to analyze
     * @return AnalysisResult containing the list of key themes and confidence score
     */
    private AnalysisResult<List<String>> extractKeyThemes(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are a theme extraction system. Identify the main themes or concerns in the text. Respond with themes and confidence score in format 'themes|confidence' where themes are comma-separated. Limit to 3 most important themes."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        String[] parts = response.aiMessage().text().trim().split("\\|");
        return new AnalysisResult<>(
                Arrays.asList(parts[0].split("\\s*,\\s*")),
                Double.parseDouble(parts[1])
        );
    }

    /**
     * Extracts recurring patterns from similar analyses
     *
     * @param similarAnalyses List of similar analysis results
     * @param currentDistortions Current cognitive distortions
     * @return List of recurring patterns
     */
    private List<String> extractRecurringPatterns(
            List<EmbeddingMatch<TextSegment>> similarAnalyses,
            List<String> currentDistortions) {
        // Implementation for pattern extraction
        // This could be enhanced to include confidence scores as well
        return similarAnalyses.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }
}
