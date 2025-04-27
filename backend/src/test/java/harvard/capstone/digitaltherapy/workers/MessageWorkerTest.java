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
import java.util.ArrayList;
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
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
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
        messageWorker.setSessionContext("test-session", "testuser");
    }

    @Test
    public void test_MessageWorker_Constructor() {
        assertNotNull(messageWorker, "MessageWorker should be created");
    }

    @Test
    public void test_generateResponse_initializesNewSession() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("Hello, I'm feeling anxious today."));

        String response = messageWorker.generateResponse(messages);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");

        // Verify that the session was initialized with a system message
        try {
            java.lang.reflect.Field sessionMemoriesField = MessageWorker.class.getDeclaredField("sessionMemories");
            sessionMemoriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ChatMemory> sessionMemories = (Map<String, ChatMemory>) sessionMemoriesField.get(messageWorker);
            
            assertTrue(sessionMemories.containsKey("test-session"), "Session should be created in sessionMemories");
            ChatMemory memory = sessionMemories.get("test-session");
            assertNotNull(memory, "Chat memory should be initialized");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Exception occurred while checking session initialization: " + e.getMessage());
        }
    }

    @Test
    public void test_generateResponse_handlesMultipleMessages() {
        List<ChatMessage> messages1 = new ArrayList<>();
        messages1.add(UserMessage.from("Hello, I'm feeling anxious today."));

        // Process first message
        String response1 = messageWorker.generateResponse(messages1);
        assertNotNull(response1, "First response should not be null");

        List<ChatMessage> messages2 = new ArrayList<>();
        messages2.add(UserMessage.from("I'm still feeling anxious."));

        // Process second message
        String response2 = messageWorker.generateResponse(messages2);
        assertNotNull(response2, "Second response should not be null");
        assertNotEquals(response1, response2, "Responses should be different for different inputs");
    }

    @Test
    public void test_generateResponse_handlesSystemMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("You are a helpful assistant."));
        messages.add(UserMessage.from("I need help with something."));

        String response = messageWorker.generateResponse(messages);
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
    }

    @Test
    public void test_generateResponse_throwsExceptionForInvalidSession() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("Hello"));

        // Reset session context
        messageWorker.setSessionContext(null, null);

        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse(messages);
        }, "Should throw exception when session context is not set");
    }

    @Test
    public void test_generateResponse_validatesUserAssociation() {
        String sessionId = "test-session";
        String userId = "testuser";
        messageWorker.setSessionContext(sessionId, userId);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("Hello, world!"));

        // Process message
        String response = messageWorker.generateResponse(messages);
        assertNotNull(response, "Response should not be null");

        // Create new session with same user
        String newSessionId = "new-session";
        messageWorker.setSessionContext(newSessionId, userId);
        
        List<ChatMessage> newMessages = new ArrayList<>();
        newMessages.add(UserMessage.from("Hello again"));
        
        String newResponse = messageWorker.generateResponse(newMessages);
        assertNotNull(newResponse, "New response should not be null");
    }
} 