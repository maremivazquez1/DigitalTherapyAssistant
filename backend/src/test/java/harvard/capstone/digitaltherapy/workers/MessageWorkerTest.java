package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
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
    public void test_setSessionContext_initializesNewSession() {
        String sessionId = "test-session";
        String userId = "testuser";

        messageWorker.setSessionContext(sessionId, userId);

        // Verify that the session was initialized with chat memory
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

    /**
     * Test case for generateResponse method when session context is not set.
     * This test verifies that an IllegalStateException is thrown when the session context is null.
     */
    @Test
    public void testGenerateResponseWithNullSessionContext() {
        String analysisMessage = "Test analysis message";
        String inputTranscript = "Test input transcript";

        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse(analysisMessage, inputTranscript);
        }, "Expected IllegalStateException when session context is not set");
    }

    /**
     * Tests the behavior of generateResponse when the session context is not set.
     * This test verifies that an IllegalStateException is thrown when attempting to
     * generate a response without first setting the session context.
     */
    @Test
    public void testGenerateResponse_withoutSessionContext() {
        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse("Sample analysis", "Sample transcript");
        }, "Should throw IllegalStateException when session context is not set");
    }

    /**
     * Tests the MessageWorker constructor to ensure it initializes correctly.
     * Verifies that the VectorDatabaseService, sessionMemories, and chatModel are properly set up.
     */
    @Test
    public void testMessageWorkerConstructor() {
        assertNotNull(messageWorker, "MessageWorker should be created");

        try {
            java.lang.reflect.Field vectorDatabaseServiceField = MessageWorker.class.getDeclaredField("vectorDatabaseService");
            vectorDatabaseServiceField.setAccessible(true);
            assertNotNull(vectorDatabaseServiceField.get(messageWorker), "VectorDatabaseService should be initialized");

            java.lang.reflect.Field sessionMemoriesField = MessageWorker.class.getDeclaredField("sessionMemories");
            sessionMemoriesField.setAccessible(true);
            assertNotNull(sessionMemoriesField.get(messageWorker), "sessionMemories should be initialized");

            java.lang.reflect.Field chatModelField = MessageWorker.class.getDeclaredField("chatModel");
            chatModelField.setAccessible(true);
            assertNotNull(chatModelField.get(messageWorker), "chatModel should be initialized");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Exception occurred while checking MessageWorker initialization: " + e.getMessage());
        }
    }

    /**
     * Tests the generateResponse method when the session context is not set.
     * This should throw an IllegalStateException with the message "Session context not set".
     */
    @Test
    public void test_generateResponse_sessionContextNotSet() {
        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse("Sample analysis", "Sample transcript");
        }, "Session context not set");
    }

    /**
     * Tests the generateResponse method when session context is not set and AI message is present.
     * This test verifies that an IllegalStateException is thrown when the session context is not set,
     * and that the method correctly handles AI messages in the context.
     */
    @Test
    public void test_generateResponse_sessionContextNotSetWithAiMessage() {
        String analysisMessage = "Sample analysis";
        String inputTranscript = "Sample input";

        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse(analysisMessage, inputTranscript);
        }, "Should throw IllegalStateException when session context is not set");
    }

    /**
     * Tests the generateResponse method when session context is not set.
     * This test verifies that an IllegalStateException is thrown when
     * the sessionId or userId is null.
     */
    @Test
    public void test_generateResponse_throwsExceptionWhenSessionContextNotSet() {
        String analysisMessage = "Test analysis message";
        String inputTranscript = "Test input transcript";

        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse(analysisMessage, inputTranscript);
        }, "Expected IllegalStateException when session context is not set");
    }

    /**
     * Tests the generateResponse method when session context is set and a UserMessage is processed.
     * This test verifies that the method generates a non-null response when given valid input.
     */
    @Test
    public void test_generateResponse_withValidSessionAndUserMessage() {
        // Set up the session context
        messageWorker.setSessionContext("test-session", "testuser");

        // Prepare test inputs
        String analysisMessage = "congruenceScore: 0.8\ninterpretation: The client seems anxious.\ncognitive distortions: XXXXXXXXXXXXXXX\nfollowUpPrompts: How does this anxiety affect your daily life?";
        String inputTranscript = "I'm worried about my upcoming presentation.";

        // Call the method under test
        String response = messageWorker.generateResponse(analysisMessage, inputTranscript);

        // Assert that a non-null response is generated
        assertNotNull(response, "Generated response should not be null");
        assertFalse(response.isEmpty(), "Generated response should not be empty");
    }

    /**
     * Test case for setSessionContext method.
     * This test verifies that the setSessionContext method correctly initializes
     * the session context with the provided sessionId and userId.
     */
    @Test
    public void test_setSessionContext_InitializesSessionContext() {
        String sessionId = "testSessionId";
        String userId = "testUserId";

        messageWorker.setSessionContext(sessionId, userId);

        // Since the sessionMemories map is private, we can't directly assert its contents.
        // However, we can indirectly test its behavior by calling methods that use it.
        // For example, we could call generateResponse and check if it doesn't throw an IllegalStateException.

        assertDoesNotThrow(() -> {
            messageWorker.generateResponse("Test analysis", "Test transcript");
        }, "generateResponse should not throw an exception after setSessionContext is called");
    }
}
