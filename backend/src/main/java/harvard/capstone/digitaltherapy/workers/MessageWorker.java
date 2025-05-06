package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Arrays;

@Component
public class MessageWorker {

    private static final Logger logger = LoggerFactory.getLogger(MessageWorker.class);
    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService;
    private final Map<String, ChatMemory> sessionMemories;

    private String sessionId;
    private String userId;
    private static final Map<String, Integer> sessionMessageCounter = new HashMap<>();

    public MessageWorker() {
        logger.info("Initializing MessageWorker");
        this.vectorDatabaseService = new VectorDatabaseService();
        this.sessionMemories = new ConcurrentHashMap<>();
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.2)
                .topP(0.95)
                .maxOutputTokens(300)
                .build();
        logger.debug("MessageWorker initialized successfully");
    }

    public void setSessionContext(String sessionId, String userId) {
        logger.info("Setting session context for sessionId: {} and userId: {}", sessionId, userId);
        this.sessionId = sessionId;
        this.userId = userId;
        sessionMessageCounter.merge(sessionId, 1, Integer::sum);
        sessionMemories.computeIfAbsent(sessionId, k -> {
            logger.debug("Creating new chat memory for session {}", sessionId);
            return MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .build();
        });
    }

    public String generateResponse(String analysisMessage, String inputTranscript) {
        logger.info("Generating response for session: {}", sessionId);

        if (sessionId == null || userId == null) {
            logger.error("Session context not set before generating response");
            throw new IllegalStateException("Session context not set");
        }

        // Determine current CBT stage
        int messageCount = sessionMessageCounter.getOrDefault(sessionId, 0);
        String stage = determineStage(messageCount);

        // Extract recommended interventions based on analysis
        List<String> relevantInterventions = extractInterventions(analysisMessage);

        // Retrieve or initialize chat memory
        ChatMemory chatMemory = sessionMemories.get(sessionId);

        // Build prompt context with memory injection
        List<ChatMessage> context = new ArrayList<>();
        String systemPrompt = """
            You are a licensed professional CBT therapist.
            Before each client statement, you will receive a synthesized analysis containing:
            • congruenceScore (0.0-1.0 alignment across modalities)
            • interpretation (summary of the client's internal state)
            • cognitiveDistortions (list of distortions identified)
            • followUpPrompts (therapeutic questions to explore)
            Do NOT mention these fields or any specific modality in your reply.

            Guidelines—Style:
            • Speak with empathy and non-judgment.
            • Keep replies to 2-3 sentences unless more is requested.

            Guidelines—Content:
            • Use the followUpPrompts to guide a socratic question to explore the client's core thoughts and emotions.
            • Gently challenge strong, black-and-white emotions (e.g. “hate”).
            • If appropriate, suggest behavioral experiment or thought-recording exercise.
            • Tailor your question and intervention to the current therapy phase.
            """;
        context.add(SystemMessage.from(systemPrompt));

        // Inject previous turns from memory
        context.addAll(chatMemory.messages());

        // Add dynamic user messages for this turn
        context.add(UserMessage.from("Stage: " + stage));
        context.add(UserMessage.from("Analysis:\n" + analysisMessage));
        context.add(UserMessage.from("Client said:\n" + inputTranscript));

        // Debug log the context being sent
        logger.debug("=== THERAPIST CONTEXT MESSAGES ===");
        for (ChatMessage m : context) {
            String content;
            if (m instanceof UserMessage) {
                content = ((UserMessage) m).singleText();
            }
            else if (m instanceof SystemMessage) {
                content = ((SystemMessage) m).text();
            }
            else if (m instanceof AiMessage) {
                content = ((AiMessage) m).text();
            }
            else {
                // any other ChatMessage subtype
                content = m.toString();
            }
            logger.debug("  [{}] {}", m.getClass().getSimpleName(), content);
        }

        // Generate the therapist response
        ChatResponse response = chatModel.chat(context);

        // Update memory with this turn
        chatMemory.add(UserMessage.from(inputTranscript));
        chatMemory.add(response.aiMessage());

        // Index the response in your vector DB and return it
        String responseText = response.aiMessage().text();
        vectorDatabaseService.indexSessionMessage(sessionId, userId, responseText, false);
        logger.info("Response generated successfully for session: {}", sessionId);
        return responseText;
    }

    private String determineStage(int messageCount) {
        if (messageCount < 5) return "INTRODUCTION - Establish rapport and safety";
        if (messageCount < 15) return "CORE - Active CBT work with focus on cognitive distortions";
        if (messageCount < 20) return "CONCLUSION - Wind-down and consolidation";
        return "SUMMARY - Session recap and next steps";
    }

    private List<String> extractInterventions(String analysisMessage) {
        List<String> distortions = extractDistortions(analysisMessage);
        if (distortions.isEmpty()) {
            return Collections.emptyList();
        }
        return vectorDatabaseService.findRelevantInterventions(distortions, 2);
    }

    private List<String> extractDistortions(String analysisMessage) {
        // Look for “cognitive distortion(s):” followed by a comma-separated list, stopping at the first period
        Pattern pattern = Pattern.compile(
            "(?:cognitive distortions?[:]?\\s*)([A-Za-z ,]+?)(?:\\.|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(analysisMessage);

        if (matcher.find()) {
            String list = matcher.group(1).trim();
            // Split on commas, trim each item, filter out any empties
            return Arrays.stream(list.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
        }

        // nothing found
        return Collections.emptyList();
    }

}
