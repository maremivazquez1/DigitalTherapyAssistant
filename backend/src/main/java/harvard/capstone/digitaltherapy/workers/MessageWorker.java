package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;

import java.util.*;

public class MessageWorker {

    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService = new VectorDatabaseService();
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
        Map<String, Double> sessionHistory=  vectorDatabaseService.findSimilarSessions(userId,lastUserMessage, 10);
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
        String formatted_prompt= buildPrompt(lastUserMessage,sessionHistory);
        ChatResponse response = chatModel.chat(UserMessage.from(formatted_prompt));

        if (userId != null && sessionId != null) {
            vectorDatabaseService.indexSessionMessage(
                sessionId, userId, response.aiMessage().text(), false);
        }

        return response.aiMessage().text();
    }

    public String buildPrompt(String synthesizerAnalysis, Map<String, Double> previousSessions) {
        StringBuilder contextBuilder = new StringBuilder();

        // Process previous sessions if available
        if (previousSessions != null && !previousSessions.isEmpty()) {
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3) // Limit to most relevant sessions
                    .forEach(entry -> {
                        contextBuilder.append("- Previous insight: ")
                                .append(entry.getKey())
                                .append("\n");
                    });
        }

        String prompt = String.format("""
        You are a warm and empathetic therapeutic companion, trained in cognitive behavioral therapy, who builds genuine connections with people. Your approach is gentle, understanding, and deeply respectful of each person's unique journey.

        I'll share with you a synthesis of our friend's current response, including their words, tone, and expressions: %s

        %s
        
        As you respond, please:
        
        💝 Connect with genuine warmth and understanding by:
        • Listening deeply to both spoken and unspoken feelings
        • Acknowledging their experiences with gentle validation
        • Creating a safe, judgment-free space for sharing
        
        🤝 Offer therapeutic support by:
        • Gently exploring thoughts and feelings together
        • Helping identify patterns with compassionate curiosity
        • Suggesting coping strategies in a collaborative way
        
        💫 Ensure your response:
        • Flows naturally and conversationally
        • Maintains a gentle, supportive presence
        • Builds upon our shared understanding
        
        Remember to be fully present with them, responding to their immediate needs while keeping in mind the context of their journey.
        """,
                synthesizerAnalysis,
                previousSessions != null && !previousSessions.isEmpty()
                        ? "\nDrawing from our previous conversations, while staying present with the current moment:\n" + contextBuilder.toString()
                        : ""
        );

        return prompt;
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
