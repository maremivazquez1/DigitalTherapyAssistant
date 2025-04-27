package harvard.capstone.digitaltherapy.cbt.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import harvard.capstone.digitaltherapy.orchestration.MultimodalSynthesisService;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.MessageWorker;
import harvard.capstone.digitaltherapy.workers.TextAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class OrchestrationServiceTest {

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
    }

    @Test
    public void test_OrchestrationService_Constructor() {
        OrchestrationService orchestrationService = new OrchestrationService();

        assertNotNull(orchestrationService, "OrchestrationService should be created");

        // Use reflection to check if private fields are initialized
        try {
            java.lang.reflect.Field textAnalysisWorkerField = OrchestrationService.class.getDeclaredField("textAnalysisWorker");
            textAnalysisWorkerField.setAccessible(true);
            assertNotNull(textAnalysisWorkerField.get(orchestrationService), "textAnalysisWorker should be initialized");

            java.lang.reflect.Field messageWorkerField = OrchestrationService.class.getDeclaredField("messageWorker");
            messageWorkerField.setAccessible(true);
            assertNotNull(messageWorkerField.get(orchestrationService), "messageWorker should be initialized");

            java.lang.reflect.Field synthesisServiceField = OrchestrationService.class.getDeclaredField("synthesisService");
            synthesisServiceField.setAccessible(true);
            assertNotNull(synthesisServiceField.get(orchestrationService), "synthesisService should be initialized");

            java.lang.reflect.Field vectorDatabaseServiceField = OrchestrationService.class.getDeclaredField("vectorDatabaseService");
            vectorDatabaseServiceField.setAccessible(true);
            assertNotNull(vectorDatabaseServiceField.get(orchestrationService), "vectorDatabaseService should be initialized");

            java.lang.reflect.Field videoAnalysisWorkerField = OrchestrationService.class.getDeclaredField("videoAnalysisWorker");
            videoAnalysisWorkerField.setAccessible(true);
            assertNotNull(videoAnalysisWorkerField.get(orchestrationService), "videoAnalysisWorker should be initialized");

            java.lang.reflect.Field audioAnalysisWorkerField = OrchestrationService.class.getDeclaredField("audioAnalysisWorker");
            audioAnalysisWorkerField.setAccessible(true);
            assertNotNull(audioAnalysisWorkerField.get(orchestrationService), "audioAnalysisWorker should be initialized");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Exception occurred while checking field initialization: " + e.getMessage());
        }
    }

    @Test
    public void test_associateSession_initializesNewSession() {
        OrchestrationService orchestrationService = new OrchestrationService();
        String sessionId = "test-session";

        String result = orchestrationService.associateSession(sessionId);

        assertEquals(sessionId, result, "The method should return the provided sessionId");
        
        // Verify that the session was initialized with a system message
        try {
            java.lang.reflect.Field sessionMessagesField = OrchestrationService.class.getDeclaredField("sessionMessages");
            sessionMessagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, List<ChatMessage>> sessionMessages = (Map<String, List<ChatMessage>>) sessionMessagesField.get(orchestrationService);
            
            assertTrue(sessionMessages.containsKey(sessionId), "Session should be created in sessionMessages");
            List<ChatMessage> messages = sessionMessages.get(sessionId);
            assertFalse(messages.isEmpty(), "Session should have messages");
            assertTrue(messages.get(0) instanceof SystemMessage, "First message should be a system message");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Exception occurred while checking session initialization: " + e.getMessage());
        }
    }

    @Test
    public void test_processUserMessage_handlesMultipleMessages() {
        OrchestrationService orchestrationService = new OrchestrationService();
        String sessionId = "test-session";
        orchestrationService.associateSession(sessionId);

        Map<String, String> modalities = new HashMap<>();
        modalities.put("text", "test input");
        String inputTranscript = "Hello, I'm feeling anxious today.";

        // Process first message
        String response1 = orchestrationService.processUserMessage(sessionId, modalities, inputTranscript);
        assertNotNull(response1, "First response should not be null");

        // Process second message
        String response2 = orchestrationService.processUserMessage(sessionId, modalities, "I'm still feeling anxious.");
        assertNotNull(response2, "Second response should not be null");
        assertNotEquals(response1, response2, "Responses should be different for different inputs");
    }

    @Test
    public void test_processUserMessage_throwsExceptionForInvalidSessionId() {
        OrchestrationService orchestrationService = new OrchestrationService();
        String invalidSessionId = "invalid_session_id";
        Map<String, String> modalities = new HashMap<>();
        String inputTranscript = "Test input";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orchestrationService.processUserMessage(invalidSessionId, modalities, inputTranscript);
        });

        assertEquals("Invalid session ID: " + invalidSessionId, exception.getMessage());
    }
}
