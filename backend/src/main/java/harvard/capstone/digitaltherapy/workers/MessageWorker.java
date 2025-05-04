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

    public String generateResponse(List<ChatMessage> messages) {
        logger.info("Generating response for session: {}", sessionId);

        if (sessionId == null || userId == null) {
            logger.error("Session context not set before generating response");
            throw new IllegalStateException("Session context not set");
        }

        ChatMemory chatMemory = sessionMemories.get(sessionId);
        if (chatMemory == null) {
            logger.warn("No chat memory found for session: {}", sessionId);
        }

        logger.debug("Adding {} messages to chat memory", messages.size());
        for (ChatMessage message : messages) {
            chatMemory.add(message);
        }

        String lastUserMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                lastUserMessage = ((UserMessage) messages.get(i)).singleText();
                logger.debug("Found last user message, length: {}", lastUserMessage.length());
                break;
            }
        }

        logger.debug("Finding similar sessions for userId: {}", userId);
        Map<String, Double> sessionHistory = vectorDatabaseService.findSimilarSessions(userId, lastUserMessage, 10);
        logger.debug("Found {} similar sessions", sessionHistory.size());

        if (userId != null && sessionId != null && !lastUserMessage.isEmpty()) {
            logger.debug("Building context for prompt");
            String relevantContext = vectorDatabaseService.buildContextForPrompt(
                    sessionId, userId, lastUserMessage, 3);
            if (!relevantContext.isEmpty()) {
                logger.debug("Adding relevant context to chat memory");
                chatMemory.add(SystemMessage.from(
                        "Consider this relevant information from previous sessions: " + relevantContext));
            }

            List<String> cognitiveDistortions = extractDistortionsFromMessages(messages);
            if (!cognitiveDistortions.isEmpty()) {
                logger.debug("Found {} cognitive distortions", cognitiveDistortions.size());
                List<String> relevantInterventions =
                        vectorDatabaseService.findRelevantInterventions(cognitiveDistortions, 2);

                if (!relevantInterventions.isEmpty()) {
                    logger.debug("Adding {} therapeutic approaches", relevantInterventions.size());
                    chatMemory.add(SystemMessage.from(
                            "Consider these therapeutic approaches: " + String.join("; ", relevantInterventions)));
                }
            }
        }

        List<ChatMessage> context = new ArrayList<>();
        int messageCount = sessionMessageCounter.getOrDefault(sessionId, 0);
        logger.debug("Building prompt for message count: {}", messageCount);
        if (messageCount < 5) {
            logger.debug("Using introductory prompt");
            context = promptBuilder.buildIntroductoryPrompt(lastUserMessage, sessionHistory, context);
        } else if (messageCount >= 5 && messageCount < 15) {
            logger.debug("Using core CBT prompt");
            context = promptBuilder.buildCoreCBTPrompt(lastUserMessage, sessionHistory,context);
        } else if (messageCount >= 15 && messageCount < 20) {
            logger.debug("Using conclusion CBT prompt");
            context = promptBuilder.buildConclusionCBTPrompt(lastUserMessage, sessionHistory,context);
        } else {
            logger.debug("Using summary CBT prompt");
            context = promptBuilder.buildSummaryCBTPrompt(lastUserMessage, sessionHistory,context);
        }

        context.addAll(chatMemory.messages());

        logger.debug("Generating chat response");
        ChatResponse response = chatModel.chat(context);
        String responseText = response.aiMessage().text();
        logger.debug("Adding response to chat memory");
        chatMemory.add(response.aiMessage());
        logger.debug("Indexing response in vector database");
        vectorDatabaseService.indexSessionMessage(sessionId, userId, responseText, false);
        logger.info("Response generated successfully for session: {}", sessionId);
        return responseText;
    }

    private List<String> extractDistortionsFromMessages(List<ChatMessage> messages) {
        logger.debug("Extracting cognitive distortions from {} messages", messages.size());
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                String text = ((SystemMessage) message).text();
                if (text.contains("cognitive distortions")) {
                    int start = text.indexOf("cognitive distortions: ");
                    if (start != -1) {
                        start += "cognitive distortions: ".length();
                        int end = text.indexOf(".", start);
                        if (end != -1) {
                            List<String> distortions = Arrays.asList(text.substring(start, end).split(", "));
                            logger.debug("Found {} cognitive distortions", distortions.size());
                            return distortions;
                        }
                    }
                }
            }
        }
        logger.debug("No cognitive distortions found");
        return Collections.emptyList();
    }
}

