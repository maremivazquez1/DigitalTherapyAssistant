package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import harvard.capstone.digitaltherapy.cbt.model.AnalysisResult;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * MultiModalSynthesizer is a node in your LangGraph4J workflow that:
 * 1) Reads modality outputs (text/voice/video) from AgentState
 * 2) Builds a plain text prompt
 * 3) Calls a ChatLanguageModel (OpenAiChatModel) using a series of ChatMessage objects
 * 4) Parses the JSON response into an Analysis objec
 * 5) Returns a map of updates to be merged into AgentState
 */
public class MultiModalSynthesizer implements NodeAction<AgentState> {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public MultiModalSynthesizer() {
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.1) // Lower temperature for analysis tasks
                .topP(0.95)
                .maxOutputTokens(3000)
                .build();

        // Initialize Jackson's ObjectMapper for JSON parsing.
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        // Retrieve the underlying data map from AgentState (assumes AgentState provides data()).
        Map<String, Object> data = state.data();

        // Extract modality outputs from data (assuming they are stored as maps).
        @SuppressWarnings("unchecked")
        Map<String, Object> textInsights = (Map<String, Object>) data.get("textInsights");
        @SuppressWarnings("unchecked")
        Map<String, Object> voiceInsights = (Map<String, Object>) data.get("voiceInsights");
        @SuppressWarnings("unchecked")
        Map<String, Object> videoInsights = (Map<String, Object>) data.get("videoInsights");

        // Build the final prompt string.
        String finalPrompt = buildPrompt(textInsights, voiceInsights, videoInsights);

        // Create a list of ChatMessage objects. We'll have a system message and a user message.
        List<ChatMessage> messages = List.of(
                SystemMessage.from("You are an expert in psychological analysis and data interpretation. Follow the instructions below."),
                UserMessage.from(finalPrompt)
        );

        // Call the chat model using the messages list.
        ChatResponse response = chatModel.chat(messages);
        // Extract the AI response text.
        String llmOutput = response.aiMessage().text();

        // Parse the JSON response into an Analysis object.
        AnalysisResult analysis = parseLLMResponse(llmOutput);

        // Return a map that merges our updates into AgentState.
        return Map.of("multimodalAnalysis", analysis);
    }

    /**
     * Builds a textual prompt, referencing the modality scores.
     */
    private String buildPrompt(
            Map<String, Object> textInsights,
            Map<String, Object> voiceInsights,
            Map<String, Object> videoInsights
    ) {
        double textScore  = getScore(textInsights,  "wordScore");
        double voiceScore = getScore(voiceInsights, "audioScore");
        double videoScore = getScore(videoInsights, "videoScore");

        String prompt = ""
                + "You are an expert in psychological analysis and data interpretation.\n"
                + "We have three scores representing a person's emotional expression:\n\n"
                + " - Words: " + String.format("%.2f", textScore) + "\n"
                + " - Tone: " + String.format("%.2f", voiceScore) + "\n"
                + " - Facial Expression: " + String.format("%.2f", videoScore) + "\n\n"
                + "All scores range from 0 to 1.\n\n"
                + "Task:\n"
                + "  1. Compute a Congruence Score (0-1) by evaluating the alignment of these three scores.\n"
                + "  2. Determine the Dominant Emotion from these inputs, with justification.\n"
                + "  3. Identify any Cognitive Distortions (e.g., 'all-or-nothing thinking', 'catastrophizing').\n"
                + "  4. Provide a detailed Interpretation of these results.\n"
                + "  5. Generate a list of Follow-Up Prompts for further exploration.\n\n"
                + "Return the result in JSON format with these keys:\n"
                + "  - congruenceScore\n"
                + "  - dominantEmotion\n"
                + "  - cognitiveDistortions\n"
                + "  - interpretation\n"
                + "  - followUpPrompts\n";
        return prompt;
    }

    /**
     * Utility method to extract a numeric score from a map by key.
     */
    private double getScore(Map<String, Object> map, String key) {
        if (map != null && map.get(key) instanceof Number) {
            return ((Number) map.get(key)).doubleValue();
        }
        return 0.0;
    }
    /**
     * Parses the LLM's JSON response into an Analysis object.
     */
    private AnalysisResult parseLLMResponse(String jsonResponse) {
        AnalysisResult analysis = new AnalysisResult();
        try {
            // Trim whitespace
            jsonResponse = jsonResponse.trim();

            // Remove markdown markers if present
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7).trim();
            }
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3).trim();
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3).trim();
            }
            // Debug: print the cleaned JSON string.
            System.out.println("Cleaned JSON Output: " + jsonResponse);
            //String cleanedJson = jsonResponse.substring(0, jsonResponse.lastIndexOf('"')) + "\"}";
            JsonNode root = objectMapper.readTree(jsonResponse);
            analysis.setCongruenceScore(root.path("congruenceScore").asDouble());
            analysis.setDominantEmotion(root.path("dominantEmotion").asText());

            JsonNode distNode = root.path("cognitiveDistortions");
            if (distNode.isArray()) {
                String[] arr = new String[distNode.size()];
                for (int i = 0; i < distNode.size(); i++) {
                    arr[i] = distNode.get(i).asText();
                }
                analysis.setCognitiveDistortions(arr);
            }

            analysis.setInterpretation(root.path("interpretation").asText());

            JsonNode promptsNode = root.path("followUpPrompts");
            if (promptsNode.isArray()) {
                String[] arr = new String[promptsNode.size()];
                for (int i = 0; i < promptsNode.size(); i++) {
                    arr[i] = promptsNode.get(i).asText();
                }
                analysis.setFollowUpPrompts(arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return analysis;
    }
}
