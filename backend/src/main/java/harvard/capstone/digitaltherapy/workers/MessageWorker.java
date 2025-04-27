package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWorker {

    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService;
    private final Map<String, ChatMemory> sessionMemories;
    private String sessionId;
    private String userId;

    public MessageWorker() {
        this.vectorDatabaseService = new VectorDatabaseService();
        this.sessionMemories = new ConcurrentHashMap<>();
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.2)
                .topP(0.95)
                .maxOutputTokens(300)
                .build();

        /*OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                //.apiKey("demo") comment this line out
                .modelName("gpt-4o-mini")
                .build();
*/
    }

    public void setSessionContext(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        
        // Initialize or get existing chat memory for this session
        sessionMemories.computeIfAbsent(sessionId, k -> 
            MessageWindowChatMemory.builder()
                .maxMessages(20) // Keep last 20 messages in memory
                .build()
        );
    }

    public String generateResponse(List<ChatMessage> messages) {
        if (sessionId == null || userId == null) {
            throw new IllegalStateException("Session context not set");
        }

        ChatMemory chatMemory = sessionMemories.get(sessionId);
        if (chatMemory == null) {
            throw new IllegalStateException("No chat memory found for session: " + sessionId);
        }

        // Add messages to chat memory
        for (ChatMessage message : messages) {
            chatMemory.add(message);
        }

        String lastUserMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                lastUserMessage = ((UserMessage) messages.get(i)).singleText();
                break;
            }
        }

        if (!lastUserMessage.isEmpty()) {
            String relevantContext = vectorDatabaseService.buildContextForPrompt(
                sessionId, userId, lastUserMessage, 3);

            if (!relevantContext.isEmpty()) {
                chatMemory.add(SystemMessage.from(
                    "Consider this relevant information from previous sessions: " + relevantContext));
            }

            List<String> cognitiveDistortions = extractDistortionsFromMessages(messages);
            if (!cognitiveDistortions.isEmpty()) {
                List<String> relevantInterventions =
                    vectorDatabaseService.findRelevantInterventions(cognitiveDistortions, 2);

                if (!relevantInterventions.isEmpty()) {
                    chatMemory.add(SystemMessage.from(
                        "Consider these therapeutic approaches: " + String.join("; ", relevantInterventions)));
                }
            }
        }

        // Get messages from chat memory and generate response
        List<ChatMessage> memoryMessages = chatMemory.messages();
        ChatResponse response = chatModel.chat(memoryMessages);
        String responseText = response.aiMessage().text();

        // Add the response to chat memory
        chatMemory.add(response.aiMessage());

        // Index the response in vector database
        vectorDatabaseService.indexSessionMessage(sessionId, userId, responseText, false);

        return responseText;
    }

    private List<String> extractDistortionsFromMessages(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                String text = ((SystemMessage) message).text();
                if (text.contains("cognitive distortions")) {
                    int start = text.indexOf("cognitive distortions: ");
                    if (start != -1) {
                        start += "cognitive distortions: ".length();
                        int end = text.indexOf(".", start);
                        if (end != -1) {
                            return Arrays.asList(text.substring(start, end).split(", "));
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}
