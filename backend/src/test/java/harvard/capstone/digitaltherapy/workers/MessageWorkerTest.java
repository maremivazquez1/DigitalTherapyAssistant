package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import harvard.capstone.digitaltherapy.orchestration.MultimodalSynthesisService;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class MessageWorkerTest {

    private MessageWorker messageWorker;
    private ChatLanguageModel model;
    private ChatMemory chatMemory;

    @BeforeEach
    public void setup() {
        // Set up a mock security context with a test user
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Initialize the chat model and memory
        model = OpenAiChatModel.builder()
            .apiKey("test-key")
            .modelName("gpt-3.5-turbo")
            .build();

        chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

        // Initialize the message worker
        messageWorker = new MessageWorker();
    }

    @Test
    public void test_MessageWorker_Constructor() {
        assertNotNull(messageWorker, "MessageWorker should be created");
    }

    @Test
    public void test_processMessage_initializesNewSession() {
        String sessionId = "test-session";
        Map<String, String> modalities = new HashMap<>();
        modalities.put("text", "Hello, I'm feeling anxious today.");
        String inputTranscript = "Hello, I'm feeling anxious today.";

        String response = messageWorker.processMessage(sessionId, modalities, inputTranscript);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");

        // Verify that the session was initialized with a system message
        try {
            java.lang.reflect.Field sessionMemoriesField = MessageWorker.class.getDeclaredField("sessionMemories");
            sessionMemoriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ChatMemory> sessionMemories = (Map<String, ChatMemory>) sessionMemoriesField.get(messageWorker);
            
            assertTrue(sessionMemories.containsKey(sessionId), "Session should be created in sessionMemories");
            ChatMemory memory = sessionMemories.get(sessionId);
            assertNotNull(memory, "Chat memory should be initialized");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Exception occurred while checking session initialization: " + e.getMessage());
        }
    }

    @Test
    public void test_processMessage_handlesMultipleMessages() {
        String sessionId = "test-session";
        Map<String, String> modalities = new HashMap<>();
        modalities.put("text", "test input");

        // Process first message
        String response1 = messageWorker.processMessage(sessionId, modalities, "Hello, I'm feeling anxious today.");
        assertNotNull(response1, "First response should not be null");

        // Process second message
        String response2 = messageWorker.processMessage(sessionId, modalities, "I'm still feeling anxious.");
        assertNotNull(response2, "Second response should not be null");
        assertNotEquals(response1, response2, "Responses should be different for different inputs");
    }

    @Test
    public void test_processMessage_handlesDifferentModalities() {
        String sessionId = "test-session";
        
        // Test with text modality
        Map<String, String> textModalities = new HashMap<>();
        textModalities.put("text", "I'm feeling stressed about work.");
        String textResponse = messageWorker.processMessage(sessionId, textModalities, "I'm feeling stressed about work.");
        assertNotNull(textResponse, "Text response should not be null");

        // Test with audio modality
        Map<String, String> audioModalities = new HashMap<>();
        audioModalities.put("audio", "base64_encoded_audio");
        String audioResponse = messageWorker.processMessage(sessionId, audioModalities, "I sounded anxious in my voice.");
        assertNotNull(audioResponse, "Audio response should not be null");

        // Test with video modality
        Map<String, String> videoModalities = new HashMap<>();
        videoModalities.put("video", "base64_encoded_video");
        String videoResponse = messageWorker.processMessage(sessionId, videoModalities, "I looked nervous in the video.");
        assertNotNull(videoResponse, "Video response should not be null");
    }

    @Test
    public void test_processMessage_handlesEmptyModalities() {
        String sessionId = "test-session";
        Map<String, String> emptyModalities = new HashMap<>();
        String inputTranscript = "I'm not sure how I feel.";

        String response = messageWorker.processMessage(sessionId, emptyModalities, inputTranscript);
        assertNotNull(response, "Response should not be null for empty modalities");
        assertFalse(response.isEmpty(), "Response should not be empty for empty modalities");
    }
} 