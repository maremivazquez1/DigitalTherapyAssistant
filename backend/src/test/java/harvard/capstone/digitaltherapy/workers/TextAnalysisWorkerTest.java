package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TextAnalysisWorkerTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private VectorDatabaseService vectorDatabaseService;

    private TextAnalysisWorker textAnalysisWorker;

    @BeforeEach
    public void setUp() {
        // Create a custom constructor for testing
        textAnalysisWorker = new TextAnalysisWorker() {
            @Override
            public Map<String, Object> analyzeText(String userInput) {
                // Access mocked dependencies
                return super.analyzeText(userInput);
            }
        };

        // Inject mocked dependencies using reflection
        try {
            java.lang.reflect.Field chatModelField = TextAnalysisWorker.class.getDeclaredField("chatModel");
            chatModelField.setAccessible(true);
            chatModelField.set(textAnalysisWorker, chatModel);

            java.lang.reflect.Field vectorDatabaseServiceField = TextAnalysisWorker.class.getDeclaredField("vectorDatabaseService");
            vectorDatabaseServiceField.setAccessible(true);
            vectorDatabaseServiceField.set(textAnalysisWorker, vectorDatabaseService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocked dependencies", e);
        }
    }

    @Test
    public void testAnalyzeText_BasicEmotionAndDistortions() {
        // Setup
        String userInput = "I'm feeling really anxious today. Everything is going wrong and I'll never get better.";

        // Mock emotion detection response
        ChatResponse emotionResponse = mock(ChatResponse.class);
        when(emotionResponse.aiMessage()).thenReturn(AiMessage.from("anxiety"));

        // Mock cognitive distortions response
        ChatResponse distortionsResponse = mock(ChatResponse.class);
        when(distortionsResponse.aiMessage()).thenReturn(AiMessage.from("catastrophizing, all-or-nothing thinking"));

        // Mock key themes response
        ChatResponse themesResponse = mock(ChatResponse.class);
        when(themesResponse.aiMessage()).thenReturn(AiMessage.from("anxiety, helplessness, negativity"));

        // Use consecutive calls to mock different responses
        // This addresses the ambiguity problem with the chat method
        when(chatModel.chat(any(List.class)))
                .thenReturn(emotionResponse)        // First call - emotion detection
                .thenReturn(distortionsResponse)    // Second call - cognitive distortions
                .thenReturn(themesResponse);        // Third call - key themes

        // Execute
        Map<String, Object> analysis = textAnalysisWorker.analyzeText(userInput);

        // Verify
        assertNotNull(analysis);
        assertEquals("anxiety", analysis.get("emotion"));

        @SuppressWarnings("unchecked")
        List<String> distortions = (List<String>) analysis.get("cognitiveDistortions");
        assertEquals(2, distortions.size());
        assertTrue(distortions.contains("catastrophizing"));
        assertTrue(distortions.contains("all-or-nothing thinking"));

        @SuppressWarnings("unchecked")
        List<String> themes = (List<String>) analysis.get("keyThemes");
        assertEquals(3, themes.size());
        assertTrue(themes.contains("anxiety"));
        assertTrue(themes.contains("helplessness"));
        assertTrue(themes.contains("negativity"));

        // Verify timestamp is present
        assertTrue(analysis.containsKey("analysisTimestamp"));

        // Verify that no vector database operations were attempted without session context
        verify(vectorDatabaseService, never()).findSimilarContent(anyString(), anyMap(), anyInt());
    }

    @Test
    public void testAnalyzeText_WithSessionContext() {
        // Setup
        String userInput = "I always mess things up.";
        String sessionId = "test-session-123";
        String userId = "test-user-456";

        // Set session context
        textAnalysisWorker.setSessionContext(sessionId, userId);

        // Mock emotion detection response
        ChatResponse emotionResponse = mock(ChatResponse.class);
        when(emotionResponse.aiMessage()).thenReturn(AiMessage.from("sadness"));

        // Mock cognitive distortions response
        ChatResponse distortionsResponse = mock(ChatResponse.class);
        when(distortionsResponse.aiMessage()).thenReturn(AiMessage.from("overgeneralization"));

        // Mock key themes response
        ChatResponse themesResponse = mock(ChatResponse.class);
        when(themesResponse.aiMessage()).thenReturn(AiMessage.from("self-doubt, perfectionism"));

        // Configure chatModel to return our mock responses - use the correct parameter type
        when(chatModel.chat(any(List.class)))
                .thenReturn(emotionResponse, distortionsResponse, themesResponse);

        // Mock vector database service finding similar content
        Map<String, Object> metadataFilters = new HashMap<>();
        metadataFilters.put("userId", userId);
        metadataFilters.put("contentType", "analysis");

        TextSegment segment = TextSegment.from("Previous analysis containing overgeneralization pattern");
        EmbeddingMatch<TextSegment> match = mock(EmbeddingMatch.class);
        when(match.embedded()).thenReturn(segment);

        when(vectorDatabaseService.findSimilarContent(eq(userInput), eq(metadataFilters), eq(3)))
                .thenReturn(Collections.singletonList(match));

        // Execute
        Map<String, Object> analysis = textAnalysisWorker.analyzeText(userInput);

        // Verify
        assertNotNull(analysis);
        assertEquals("sadness", analysis.get("emotion"));

        @SuppressWarnings("unchecked")
        List<String> distortions = (List<String>) analysis.get("cognitiveDistortions");
        assertEquals(1, distortions.size());
        assertEquals("overgeneralization", distortions.get(0));

        // Verify vector database was queried with session context
        verify(vectorDatabaseService).findSimilarContent(eq(userInput), eq(metadataFilters), eq(3));
    }

    @Test
    public void testAnalyzeText_NoDistortionsFound() {
        // Setup
        String userInput = "I had a productive day at work today.";

        // Mock emotion detection response
        ChatResponse emotionResponse = mock(ChatResponse.class);
        when(emotionResponse.aiMessage()).thenReturn(AiMessage.from("joy"));

        // Mock cognitive distortions response - none found
        ChatResponse distortionsResponse = mock(ChatResponse.class);
        when(distortionsResponse.aiMessage()).thenReturn(AiMessage.from("none"));

        // Mock key themes response
        ChatResponse themesResponse = mock(ChatResponse.class);
        when(themesResponse.aiMessage()).thenReturn(AiMessage.from("productivity, accomplishment, satisfaction"));

        // Configure chatModel to return our mock responses - use the correct parameter type
        when(chatModel.chat(any(List.class)))
                .thenReturn(emotionResponse, distortionsResponse, themesResponse);

        // Execute
        Map<String, Object> analysis = textAnalysisWorker.analyzeText(userInput);

        // Verify
        assertNotNull(analysis);
        assertEquals("joy", analysis.get("emotion"));

        @SuppressWarnings("unchecked")
        List<String> distortions = (List<String>) analysis.get("cognitiveDistortions");
        assertTrue(distortions.isEmpty(), "No distortions should be found");

        @SuppressWarnings("unchecked")
        List<String> themes = (List<String>) analysis.get("keyThemes");
        assertEquals(3, themes.size());
        assertTrue(themes.contains("productivity"));
    }

    @Test
    public void testDetectRecurringPatterns() {
        // Setup
        String userInput = "I keep making the same mistakes over and over.";
        String sessionId = "test-session-123";
        String userId = "test-user-456";

        // Set session context
        textAnalysisWorker.setSessionContext(sessionId, userId);

        // Mock emotion detection response
        ChatResponse emotionResponse = mock(ChatResponse.class);
        when(emotionResponse.aiMessage()).thenReturn(AiMessage.from("frustration"));

        // Mock cognitive distortions response
        ChatResponse distortionsResponse = mock(ChatResponse.class);
        when(distortionsResponse.aiMessage()).thenReturn(AiMessage.from("overgeneralization, labeling"));

        // Mock key themes response
        ChatResponse themesResponse = mock(ChatResponse.class);
        when(themesResponse.aiMessage()).thenReturn(AiMessage.from("self-criticism, mistakes, patterns"));

        // Configure chatModel to return our mock responses - use the correct parameter type
        when(chatModel.chat(any(List.class)))
                .thenReturn(emotionResponse, distortionsResponse, themesResponse);

        // Mock vector database service finding similar content with matching distortions
        TextSegment segment1 = TextSegment.from("Previous analysis showing overgeneralization pattern");
        TextSegment segment2 = TextSegment.from("Another previous analysis with overgeneralization and labeling");

        EmbeddingMatch<TextSegment> match1 = mock(EmbeddingMatch.class);
        when(match1.embedded()).thenReturn(segment1);

        EmbeddingMatch<TextSegment> match2 = mock(EmbeddingMatch.class);
        when(match2.embedded()).thenReturn(segment2);

        Map<String, Object> metadataFilters = new HashMap<>();
        metadataFilters.put("userId", userId);
        metadataFilters.put("contentType", "analysis");

        when(vectorDatabaseService.findSimilarContent(eq(userInput), eq(metadataFilters), eq(3)))
                .thenReturn(Arrays.asList(match1, match2));

        // Execute
        Map<String, Object> analysis = textAnalysisWorker.analyzeText(userInput);

        // Verify
        assertNotNull(analysis);

        @SuppressWarnings("unchecked")
        List<String> recurringPatterns = (List<String>) analysis.get("recurringPatterns");
        assertNotNull(recurringPatterns, "Recurring patterns should be identified");
        assertTrue(recurringPatterns.contains("overgeneralization"), "Overgeneralization should be identified as recurring");
    }
}