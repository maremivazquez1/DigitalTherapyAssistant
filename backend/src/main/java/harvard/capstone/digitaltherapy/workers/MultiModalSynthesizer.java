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
        Map<String, Object> textInsights = (Map<String, Object>) data.get("textInsights");
        String textAnalysis= (String) textInsights.get("wordsAnalysis");
        Map<String, Object> voiceInsights = (Map<String, Object>) data.get("voiceInsights");
        String toneAnalysis= (String) voiceInsights.get("toneAnalysis");
        Map<String, Object> videoInsights = (Map<String, Object>) data.get("videoInsights");
        String facialAnalysis= (String) videoInsights.get("facialAnalysis");
        // Build the final prompt string.
        String finalPrompt = buildPrompt(textAnalysis, toneAnalysis, facialAnalysis);
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
    public String buildPrompt(String textInsights, String voiceInsights, String videoInsights) {
        String prompt = ""
                + "You are an expert in multimodal psychological analysis, specializing in synthesizing insights from verbal, vocal, and visual emotional expressions.\n\n"
                + "Analyze these three modalities of emotional expression:\n"
                + "1. TEXT ANALYSIS (verbal content):\n" + textInsights + "\n"
                + "2. VOICE ANALYSIS (prosodic features):\n" + voiceInsights + "\n"
                + "3. FACIAL ANALYSIS (visual expressions):\n" + videoInsights + "\n\n"
                + "Tasks for Holistic Analysis:\n\n"
                + "1. Congruence Evaluation:\n"
                + "   - Calculate a congruence score (0-1) measuring alignment across modalities\n"
                + "   - 0: completely misaligned expressions\n"
                + "   - 1: perfect alignment across all modalities\n"
                + "   - Consider intensity, valence, and temporal patterns\n\n"
                + "2. Emotional Assessment:\n"
                + "   - Identify the dominant emotion with confidence level\n"
                + "   - Support with evidence from each modality\n"
                + "   - Note any significant emotional conflicts\n\n"
                + "3. Cognitive Pattern Analysis:\n"
                + "   - Identify cognitive distortions present in verbal content\n"
                + "   - Cross-reference with emotional markers in voice and face\n"
                + "   - List specific examples supporting each distortion\n\n"
                + "4. Synthesized Interpretation:\n"
                + "   - Provide comprehensive analysis of emotional state\n"
                + "   - Highlight patterns of consistency or discrepancy\n"
                + "   - Note any potential masked or suppressed emotions\n\n"
                + "5. Clinical Implications:\n"
                + "   - Generate relevant follow-up areas for exploration\n"
                + "   - Focus on areas of emotional incongruence\n"
                + "   - Consider therapeutic priorities\n\n"
                + "Return a JSON response with:\n"
                + "{\n"
                + "  \"congruenceScore\": <float 0-1>,\n"
                + "  \"dominantEmotion\": {\n"
                + "    \"emotion\": <string>,\n"
                + "    \"confidence\": <float 0-1>,\n"
                + "    \"evidence\": <array of supporting points>\n"
                + "  },\n"
                + "  \"cognitiveDistortions\": [\n"
                + "    {\n"
                + "      \"type\": <string>,\n"
                + "      \"evidence\": <string>,\n"
                + "      \"modalitySource\": <array of modalities>\n"
                + "    }\n"
                + "  ],\n"
                + "  \"interpretation\": <detailed analysis string>,\n"
                + "  \"followUpPrompts\": <array of strings>\n"
                + "}\n";
        return prompt;
    }


    /**
     * Utility method to extract a numeric score from a map by key.
     */
    public double getScore(Map<String, Object> map, String key) {
        if (map != null && map.get(key) instanceof Number) {
            return ((Number) map.get(key)).doubleValue();
        }
        return 0.0;
    }
    /**
     * Parses the LLM's JSON response into an Analysis object.
     */
    public AnalysisResult parseLLMResponse(String jsonResponse) {
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
