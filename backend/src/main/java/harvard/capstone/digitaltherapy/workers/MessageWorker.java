package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

import java.util.List;

public class MessageWorker {

    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService = new VectorDatabaseService();
    // intro agent
    // middle agent
    // conclusion agent
    private String sessionId;
    private String userId;

    public MessageWorker() {
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
    }

    public String generateResponse(List<ChatMessage> messages) {
        String lastUserMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                lastUserMessage = ((UserMessage) messages.get(i)).singleText();
                break;
            }
        }

        if (userId != null && sessionId != null && !lastUserMessage.isEmpty()) {
            String relevantContext = vectorDatabaseService.buildContextForPrompt(
                sessionId, userId, lastUserMessage, 3);

            if (!relevantContext.isEmpty()) {
                messages.add(SystemMessage.from(
                    "Consider this relevant information from previous sessions: " + relevantContext));
            }

            List<String> cognitiveDistortions = extractDistortionsFromMessages(messages);
            if (!cognitiveDistortions.isEmpty()) {
                List<String> relevantInterventions =
                    vectorDatabaseService.findRelevantInterventions(cognitiveDistortions, 2);

                if (!relevantInterventions.isEmpty()) {
                    messages.add(SystemMessage.from(
                        "Consider these therapeutic approaches: " + String.join("; ", relevantInterventions)));
                }
            }
        }

        ChatResponse response = chatModel.chat(messages);

        if (userId != null && sessionId != null) {
            vectorDatabaseService.indexSessionMessage(
                sessionId, userId, response.aiMessage().text(), false);
        }

        return response.aiMessage().text();
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
