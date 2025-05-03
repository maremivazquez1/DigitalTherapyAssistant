// package harvard.capstone.digitaltherapy.workers;

// import dev.langchain4j.data.message.ChatMessage;
// import dev.langchain4j.data.message.SystemMessage;
// import dev.langchain4j.data.message.UserMessage;
// import dev.langchain4j.memory.ChatMemory;
// import dev.langchain4j.memory.chat.MessageWindowChatMemory;
// import dev.langchain4j.model.chat.ChatLanguageModel;
// import dev.langchain4j.model.openai.OpenAiChatModel;
// import dev.langchain4j.service.AiServices;
// import harvard.capstone.digitaltherapy.cbt.service.PromptBuilder;
// import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import static org.junit.jupiter.api.Assertions.*;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContext;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.context.SecurityContextImpl;

// public class MessageWorkerTest {

    // private MessageWorker messageWorker;
    // private ChatLanguageModel model;
    // private ChatMemory chatMemory;
    // private PromptBuilder promptBuilder;

//     @BeforeEach
//     public void setup() {
//         // Set up a mock security context with a test user
//         Authentication authentication = new UsernamePasswordAuthenticationToken(
//             "testuser",
//             "password",
//             List.of(new SimpleGrantedAuthority("ROLE_USER"))
//         );
//         SecurityContext securityContext = new SecurityContextImpl();
//         securityContext.setAuthentication(authentication);
//         SecurityContextHolder.setContext(securityContext);

//         // Initialize the chat model and memory
//         model = OpenAiChatModel.builder()
//             .apiKey("test-key")
//             .modelName("gpt-3.5-turbo")
//             .build();

//         chatMemory = MessageWindowChatMemory.builder()
//             .maxMessages(10)
//             .build();

//         // Initialize the message worker
//         messageWorker = new MessageWorker();
//     }

//     @Test
//     public void test_MessageWorker_Constructor() {
//         assertNotNull(messageWorker, "MessageWorker should be created");
//     }

//     @Test
//     public void test_setSessionContext_initializesNewSession() {
//         String sessionId = "test-session";
//         String userId = "testuser";

//         messageWorker.setSessionContext(sessionId, userId);

//         // Verify that the session was initialized with chat memory
//         try {
//             java.lang.reflect.Field sessionMemoriesField = MessageWorker.class.getDeclaredField("sessionMemories");
//             sessionMemoriesField.setAccessible(true);
//             @SuppressWarnings("unchecked")
//             Map<String, ChatMemory> sessionMemories = (Map<String, ChatMemory>) sessionMemoriesField.get(messageWorker);
            
//             assertTrue(sessionMemories.containsKey(sessionId), "Session should be created in sessionMemories");
//             ChatMemory memory = sessionMemories.get(sessionId);
//             assertNotNull(memory, "Chat memory should be initialized");
//         } catch (NoSuchFieldException | IllegalAccessException e) {
//             fail("Exception occurred while checking session initialization: " + e.getMessage());
//         }
//     }

//     @Test
//     public void test_generateResponse_handlesMultipleMessages() {
//         String sessionId = "test-session";
//         String userId = "testuser";

//         // Set session context
//         messageWorker.setSessionContext(sessionId, userId);

//         // Process first message
//         String response1 = messageWorker.generateResponse(List.of(UserMessage.from("Hello, I'm feeling anxious today.")));
//         assertNotNull(response1, "First response should not be null");

//         // Process second message
//         String response2 = messageWorker.generateResponse(List.of(UserMessage.from("I'm still feeling anxious.")));
//         assertNotNull(response2, "Second response should not be null");
//         assertNotEquals(response1, response2, "Responses should be different for different inputs");
//     }

//     @Test
//     public void test_generateResponse_handlesSystemMessages() {
//         String sessionId = "test-session";
//         String userId = "testuser";
        
        // Set session context
        messageWorker.setSessionContext(sessionId, userId);

        // Test with system message
        String response = messageWorker.generateResponse(List.of(
            UserMessage.from("I'm feeling stressed about work."),
            SystemMessage.from("Additional context: The user has shown signs of anxiety in previous sessions.")
        ));
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
    }

    @Test
    public void test_generateResponse_throwsExceptionForMissingSessionContext() {
        String message = "Hello, world!";

        // Try to generate response without setting session context
        assertThrows(IllegalStateException.class, () -> {
            messageWorker.generateResponse(List.of(UserMessage.from(message)));
        }, "Should throw exception when session context is not set");
    }
} 

