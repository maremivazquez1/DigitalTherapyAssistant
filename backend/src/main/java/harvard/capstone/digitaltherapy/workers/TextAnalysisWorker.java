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
        String emotion = detectEmotion(userInput);
        analysis.put("emotion", emotion);

        List<String> cognitiveDistortions = detectCognitiveDistortions(userInput);
        analysis.put("cognitiveDistortions", cognitiveDistortions);

        List<String> keyThemes = extractKeyThemes(userInput);
        analysis.put("keyThemes", keyThemes);

        if (userId != null && sessionId != null) {
            Map<String, Object> metadataFilters = new HashMap<>();
            metadataFilters.put("userId", userId);
            metadataFilters.put("contentType", "analysis");

            List<EmbeddingMatch<TextSegment>> similarAnalyses =
                    vectorDatabaseService.findSimilarContent(userInput, metadataFilters, 3);

            if (!similarAnalyses.isEmpty()) {
                analysis.put("recurringPatterns", extractRecurringPatterns(similarAnalyses, cognitiveDistortions));
            }
        }

        analysis.put("analysisTimestamp", System.currentTimeMillis());
        return analysis;
    }

    /**
     * Detects the primary emotion expressed in the text
     *
     * @param text The text to analyze
     * @return The detected primary emotion
     */
    private String detectEmotion(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are an emotion detection system. Analyze the text and identify the primary emotion expressed. Respond with only one word from this list: anger, fear, joy, sadness, surprise, disgust, neutral, anxiety, shame, guilt."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        return response.aiMessage().text().trim().toLowerCase();
    }

    /**
     * Detects cognitive distortions in the text according to CBT principles
     *
     * @param text The text to analyze
     * @return A list of detected cognitive distortions
     */
    private List<String> detectCognitiveDistortions(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are a CBT analysis system. Identify any cognitive distortions in the text according to CBT principles. Respond with only the names of the distortions separated by commas. If none are present, respond with 'none'. Common distortions: all-or-nothing thinking, overgeneralization, mental filter, disqualifying the positive, jumping to conclusions, magnification, emotional reasoning, should statements, labeling, personalization."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        String distortionsText = response.aiMessage().text().trim().toLowerCase();

        if (distortionsText.equals("none")) {
            return List.of();
        }

        return Arrays.asList(distortionsText.split(",\\s*"));
    }

    /**
     * Extracts key themes or concerns from the text
     *
     * @param text The text to analyze
     * @return A list of key themes
     */
    private List<String> extractKeyThemes(String text) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are a theme extraction system. Identify the main themes or concerns in the text. Respond with only the themes separated by commas. Limit to 3 most important themes."),
                UserMessage.from(text)
        );

        ChatResponse response = chatModel.chat(messages);
        String themesText = response.aiMessage().text().trim();

        return Arrays.asList(themesText.split(",\\s*"));
    }

    public void setSessionContext(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    private List<String> extractRecurringPatterns(List<EmbeddingMatch<TextSegment>> similarAnalyses,
                                                  List<String> currentDistortions) {
        Map<String, Integer> distortionCounts = new HashMap<>();
        for (String distortion : currentDistortions) {
            distortionCounts.put(distortion, 1);
        }

        for (EmbeddingMatch<TextSegment> match : similarAnalyses) {
            String text = match.embedded().text();
            for (String distortion : currentDistortions) {
                if (text.toLowerCase().contains(distortion.toLowerCase())) {
                    distortionCounts.put(distortion, distortionCounts.getOrDefault(distortion, 0) + 1);
                }
            }
        }

        return distortionCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}