package harvard.capstone.digitaltherapy.workers;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import harvard.capstone.digitaltherapy.cbt.service.PromptBuilder;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWorker {

    private static final Logger logger = LoggerFactory.getLogger(MessageWorker.class);
    private final ChatLanguageModel chatModel;
    private final VectorDatabaseService vectorDatabaseService;
    private final Map<String, ChatMemory> sessionMemories;
    private final PromptBuilder promptBuilder;

    private String sessionId;
    private String userId;
    private static final Map<String, Integer> sessionMessageCounter = new HashMap<>();

    public MessageWorker() {
        logger.info("Initializing MessageWorker");
        this.promptBuilder = new PromptBuilder();
        this.vectorDatabaseService = new VectorDatabaseService();
        this.sessionMemories = new ConcurrentHashMap<>();
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.2)
                .topP(0.95)
                .maxOutputTokens(400)
                .build();
        logger.debug("MessageWorker initialized successfully");
    }

    public void setSessionContext(String sessionId, String userId) {
        logger.info("Setting session context for sessionId: {} and userId: {}", sessionId, userId);
        this.sessionId = sessionId;
        this.userId = userId;
        if (this.sessionMessageCounter.containsKey(sessionId)) {
            int newCount = this.sessionMessageCounter.get(sessionId) + 1;
            this.sessionMessageCounter.put(sessionId, newCount);
            logger.debug("Incremented message counter for session {} to {}", sessionId, newCount);
        } else {
            this.sessionMessageCounter.put(sessionId, 1);
            logger.debug("Initialized new message counter for session {}", sessionId);
        }
        // Initialize or get existing chat memory for this session
        sessionMemories.computeIfAbsent(sessionId, k -> {
            logger.debug("Creating new chat memory for session {}", sessionId);
            return MessageWindowChatMemory.builder()
                    .maxMessages(20) // Keep last 20 messages in memory
                    .build();
        });
    }

    public String generateResponse(String analysisMessage, String inputTranscript) {
        logger.info("Generating response for session: {}", sessionId);

        if (sessionId == null || userId == null) {
            logger.error("Session context not set before generating response");
            throw new IllegalStateException("Session context not set");
        }

        logger.debug("Finding similar sessions for userId: {}", userId);
        Map<String, String> sessionHistory = vectorDatabaseService.findSimilarSessions(userId, analysisMessage, 25);
        logger.debug("Found {} similar sessions", sessionHistory.size());
        
        List<String> relevantInterventions = List.of("No recommended approaches at this time");
        String relevantContext = "No relevant context found";
        if (userId != null && sessionId != null && !analysisMessage.isEmpty()) {
            logger.debug("Building context for prompt");
            String hasrelevantContext = vectorDatabaseService.buildContextForPrompt(
                    sessionId, userId, analysisMessage, 3);
            if (hasrelevantContext != null) {
                logger.debug("Found relevant context: {}", hasrelevantContext);
                relevantContext = hasrelevantContext;
            }

            List<String> cognitiveDistortions = extractDistortions(analysisMessage);
            if (!cognitiveDistortions.isEmpty()) {
                logger.debug("Found {} cognitive distortions", cognitiveDistortions.size());
                List<String> foundInterventions =
                        vectorDatabaseService.findRelevantInterventions(cognitiveDistortions, 2);

                if (!foundInterventions.isEmpty()) {
                    relevantInterventions = foundInterventions;
                    logger.debug("Adding {} therapeutic approaches", relevantInterventions.size());
                }
            }
        }

        List<ChatMessage> context = new ArrayList<>();
        String systemPrompt = String.format("""
            You are an experienced CBT therapist.
            • Keep replies to 2-3 sentences unless asked for more.
            • Use Socratic questions to explore the client's current thoughts and emotions.
            • Briefly connect to any relevant patterns from past sessions.           
            """);
        context.add(SystemMessage.from(systemPrompt));

        int messageCount = sessionMessageCounter.getOrDefault(sessionId, 0);
        logger.debug("Building prompt for message count: {}", messageCount);
        String stage;
        if (messageCount < 5) {
            logger.debug("Using introductory prompt");
            stage = "INTRODUCTION - Establish rapport and safety";
        } else if (messageCount >= 5 && messageCount < 15) {
            logger.debug("Using core CBT prompt");
            stage = "CORE - Active CBT work with focus on cognitive distortions";
        } else if (messageCount >= 15 && messageCount < 20) {
            logger.debug("Using conclusion CBT prompt");
            stage = "CONCLUSION - Wind-down and consolidation";
        } else {
            logger.debug("Using summary CBT prompt");
            stage = "SUMMARY - Session recap and next steps";
        }

        ChatMemory chatMemory = sessionMemories.get(sessionId);
        String chatMemoryStr;
        if (chatMemory == null) {
            logger.warn("No chat memory found for session: {}", sessionId);
            chatMemoryStr = "No Previous History";
        }
        else {
            chatMemoryStr = chatMemory.messages().toString();
        }

        String cbtPrompt = promptBuilder.buildCoreCBTPrompt(
                stage, analysisMessage, inputTranscript, relevantInterventions.toString(), chatMemoryStr);

        context.add(UserMessage.from(cbtPrompt));

        // **Dump it** before you call the model:
        logger.debug("=== THERAPIST CONTEXT MESSAGES ===");
        for (ChatMessage m : context) {
            String content;
            if (m instanceof UserMessage) {
                // only UserMessage has singleText()
                content = ((UserMessage) m).singleText();
            } else if (m instanceof SystemMessage) {
                // SystemMessage (and AiMessage) expose .text()
                content = ((SystemMessage) m).text();
            } else {
                // fallback for any other ChatMessage subtype
                content = m.toString();
            }
            logger.debug("  [{}] {}", m.getClass().getSimpleName(), content);
        }
        // **END DUMP**
        logger.debug("Generating chat response");
        ChatResponse response = chatModel.chat(context);

        // Store resulting messages in chat memory (mutated in-map so already storing in sessionMemories)
        chatMemory.add(UserMessage.from("Analysis of user response: " + analysisMessage));
        chatMemory.add(UserMessage.from("User Reponse: " + inputTranscript));
        chatMemory.add(response.aiMessage());
        String responseText = response.aiMessage().text();
        logger.debug("Indexing response in vector database");
        vectorDatabaseService.indexSessionMessage(sessionId, userId, responseText, false);
        logger.info("Response generated successfully for session: {}", sessionId);
        return responseText;
    }

    private List<String> extractDistortions(String cognitiveDistortions) {
        if (cognitiveDistortions.contains("cognitive distortions")) {
            int start = cognitiveDistortions.indexOf("cognitive distortions: ");
            if (start != -1) {
                start += "cognitive distortions: ".length();
                int end = cognitiveDistortions.indexOf(".", start);
                if (end != -1) {
                    List<String> distortions = Arrays.asList(cognitiveDistortions.substring(start, end).split(", "));
                    logger.debug("Found {} cognitive distortions", distortions.size());
                    return distortions;
                }
            }
        }
        logger.debug("No cognitive distortions found");
        return Collections.emptyList();
    }
}

