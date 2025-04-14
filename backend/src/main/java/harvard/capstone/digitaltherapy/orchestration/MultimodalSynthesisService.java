package harvard.capstone.digitaltherapy.orchestration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MultimodalSynthesisService
 *
 * A simple service that currently focuses on aggregating text analysis results.
 * Future versions will incorporate voice and facial analysis.
 */
public class MultimodalSynthesisService {

    // Simple in-memory storage for text analysis results by session
    private Map<String, Map<String, Object>> textAnalysisStore = new HashMap<>();

    /**
     * Stores text analysis results for a session
     *
     * @param sessionId The unique identifier for the therapy session
     * @param textAnalysis The analysis results from the TextAnalysisWorker
     */
    public void addTextAnalysis(String sessionId, Map<String, Object> textAnalysis) {
        textAnalysisStore.put(sessionId, textAnalysis);
    }

    /**
     * Gets the latest text analysis for a session
     *
     * @param sessionId The session identifier
     * @return The text analysis results or an empty map if none exists
     */
    public Map<String, Object> getTextAnalysis(String sessionId) {
        return textAnalysisStore.getOrDefault(sessionId, new HashMap<>());
    }

    /**
     * Checks if cognitive distortions were detected in the latest analysis
     *
     * @param sessionId The session identifier
     * @return true if distortions were detected, false otherwise
     */
    public boolean hasDistortions(String sessionId) {
        Map<String, Object> analysis = getTextAnalysis(sessionId);
        if (analysis.containsKey("cognitiveDistortions")) {
            Object distortions = analysis.get("cognitiveDistortions");
            if (distortions instanceof List) {
                return !((List<?>) distortions).isEmpty();
            }
        }
        return false;
    }

    /**
     * Gets the most recently detected emotion for a session
     *
     * @param sessionId The session identifier
     * @return The detected emotion or "unknown" if none is available
     */
    public String getDetectedEmotion(String sessionId) {
        Map<String, Object> analysis = getTextAnalysis(sessionId);
        return (String) analysis.getOrDefault("emotion", "unknown");
    }

    /**
     * Gets the key themes from the latest analysis
     *
     * @param sessionId The session identifier
     * @return List of key themes or an empty list if none available
     */
    @SuppressWarnings("unchecked")
    public List<String> getKeyThemes(String sessionId) {
        Map<String, Object> analysis = getTextAnalysis(sessionId);
        if (analysis.containsKey("keyThemes")) {
            return (List<String>) analysis.get("keyThemes");
        }
        return List.of();
    }

    /**
     * Returns a simplified therapy context map for the LLM prompt
     *
     * @param sessionId The session identifier
     * @return A map with the essential context for therapy
     */
    public Map<String, Object> getTherapyContext(String sessionId) {
        Map<String, Object> context = new HashMap<>();

        Map<String, Object> analysis = getTextAnalysis(sessionId);
        if (!analysis.isEmpty()) {
            context.put("userEmotion", analysis.getOrDefault("emotion", "unknown"));

            if (analysis.containsKey("cognitiveDistortions")) {
                context.put("cognitiveDistortions", analysis.get("cognitiveDistortions"));
            }

            if (analysis.containsKey("keyThemes")) {
                context.put("keyThemes", analysis.get("keyThemes"));
            }
        }

        return context;
    }

    /**
     * Clears stored analysis for a session
     *
     * @param sessionId The session identifier
     */
    public void clearSessionData(String sessionId) {
        textAnalysisStore.remove(sessionId);
    }
}
