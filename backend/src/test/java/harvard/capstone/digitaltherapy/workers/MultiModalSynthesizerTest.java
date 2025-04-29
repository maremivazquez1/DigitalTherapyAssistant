package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import harvard.capstone.digitaltherapy.cbt.model.AnalysisResult;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiModalSynthesizerTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private AgentState agentState;

    @Mock
    private ChatResponse chatResponse;

    private MultiModalSynthesizer synthesizer;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        synthesizer = new MultiModalSynthesizer();
        testData = new HashMap<>();
    }

    @Test
    void testApplyWithValidData() {
        // Prepare test data
        Map<String, Object> textInsights = new HashMap<>();
        textInsights.put("wordScore", 0.8);

        Map<String, Object> voiceInsights = new HashMap<>();
        voiceInsights.put("audioScore", 0.9);

        Map<String, Object> videoInsights = new HashMap<>();
        videoInsights.put("videoScore", 0.7);

        testData.put("textInsights", textInsights);
        testData.put("voiceInsights", voiceInsights);
        testData.put("videoInsights", videoInsights);

        // Mock behavior
        when(agentState.data()).thenReturn(testData);
        // Execute
        Map<String, Object> result = synthesizer.apply(agentState);

        // Verify
        assertNotNull(result);
        assertTrue(result.containsKey("multimodalAnalysis"));
        assertTrue(result.get("multimodalAnalysis") instanceof AnalysisResult);
    }

    @Test
    void testParseLLMResponseWithValidJson() {
        String validJson = validJsonResponse();
        AnalysisResult result = synthesizer.parseLLMResponse(validJson);

        assertNotNull(result);
        assertEquals(0.86, result.getCongruenceScore());
        assertEquals("Positive Emotion (likely joy or excitement)", result.getDominantEmotion());
        assertNotNull(result.getCognitiveDistortions());
        assertNotNull(result.getFollowUpPrompts());
    }

    @Test
    void testGetScoreWithNullMap() {
        double score = synthesizer.getScore(null, "anyKey");
        assertEquals(0.0, score);
    }

    @Test
    void testGetScoreWithMissingKey() {
        Map<String, Object> map = new HashMap<>();
        double score = synthesizer.getScore(map, "nonExistentKey");
        assertEquals(0.0, score);
    }

    @Test
    void testGetScoreWithValidData() {
        Map<String, Object> map = new HashMap<>();
        map.put("score", 0.75);
        double score = synthesizer.getScore(map, "score");
        assertEquals(0.75, score);
    }

    @Test
    void testBuildPromptWithNullMaps() {
        String prompt = synthesizer.buildPrompt(null, null, null);
        assertNotNull(prompt);
        assertTrue(prompt.contains("Words: 0.00"));
        assertTrue(prompt.contains("Tone: 0.00"));
        assertTrue(prompt.contains("Facial Expression: 0.00"));
    }

    @Test
    void testBuildPromptWithValidData() {
        Map<String, Object> textMap = new HashMap<>();
        textMap.put("wordScore", 0.8);

        Map<String, Object> voiceMap = new HashMap<>();
        voiceMap.put("audioScore", 0.9);

        Map<String, Object> videoMap = new HashMap<>();
        videoMap.put("videoScore", 0.7);

        String prompt = synthesizer.buildPrompt(textMap, voiceMap, videoMap);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Words: 0.80"));
        assertTrue(prompt.contains("Tone: 0.90"));
        assertTrue(prompt.contains("Facial Expression: 0.70"));
    }

    private String validJsonResponse() {
        return """
            {
                "congruenceScore": 0.86,
                "dominantEmotion": "Positive Emotion (likely joy or excitement)",
                "cognitiveDistortions": [],
                "interpretation": "The individual presents with a high degree of emotional congruence.",
                "followUpPrompts": [
                    "Can you describe the situation that elicited these feelings?",
                    "How comfortable are you expressing your emotions verbally?"
                ]
            }
            """;
    }
}

