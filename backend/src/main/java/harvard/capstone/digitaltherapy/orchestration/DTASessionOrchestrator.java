package harvard.capstone.digitaltherapy.orchestration;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import harvard.capstone.digitaltherapy.persistence.VectorDatabaseService;
import harvard.capstone.digitaltherapy.workers.MessageWorker;
import harvard.capstone.digitaltherapy.workers.TextAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;

import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTASessionOrchestrator
 *
 * Core orchestration component that manages the therapeutic conversation flow
 * and coordinates interactions between different system components.
 */
@Service
public class DTASessionOrchestrator implements TherapySessionService {

    private final TextAnalysisWorker textAnalysisWorker;
    private final MessageWorker messageWorker;
    private final MultimodalSynthesisService synthesisService;
    private final VectorDatabaseService vectorDatabaseService;
    private final VideoAnalysisWorker videoAnalysisWorker;
    private final AudioAnalysisWorker audioAnalysisWorker;

    // Simple in-memory session tracking (would use Redis in production)
    private final Map<String, List<ChatMessage>> sessionMessages = new HashMap<>();

    public DTASessionOrchestrator() {
        this.textAnalysisWorker = new TextAnalysisWorker();
        this.messageWorker = new MessageWorker();
        this.synthesisService = new MultimodalSynthesisService();
        this.vectorDatabaseService = new VectorDatabaseService();
        this.videoAnalysisWorker = new VideoAnalysisWorker();
        this.audioAnalysisWorker = new AudioAnalysisWorker();
    }

    /**
     * Creates a new therapy session
     *
     * @return The session ID for the new session
     */

    @Override
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        List<ChatMessage> messages = new ArrayList<>();

        // Add the initial system message for a CBT therapy context
        messages.add(SystemMessage.from(
                "You are a CBT therapist guiding a patient through a CBT session. " +
                        "Use concise and empathetic language. Focus on helping the patient " +
                        "identify and reframe negative thought patterns."
        ));

        sessionMessages.put(sessionId, messages);
        return sessionId;
    }

    public String associateSession(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();

        // Add the initial system message for a CBT therapy context
        messages.add(SystemMessage.from(
                "You are a CBT therapist guiding a patient through a CBT session. " +
                        "Use concise and empathetic language. Focus on helping the patient " +
                        "identify and reframe negative thought patterns."
        ));

        sessionMessages.put(sessionId, messages);
        return sessionId;
    }

    /**
     * Processes a user message and generates a response
     *
     * @param sessionId   The session identifier
     * @param userMessage The message from the user
     * @return The therapeutic response
     */

    @Override
    public String processUserMessage(String sessionId, String userMessage) {
        if (!sessionMessages.containsKey(sessionId)) {
            throw new IllegalArgumentException("Invalid session ID: " + sessionId);
        }
        List<ChatMessage> messages = sessionMessages.get(sessionId);

        // Add the user message to the conversation history
        messages.add(UserMessage.from(userMessage));

        // 1. Analyze the text with TextAnalysisWorker
        Map<String, Object> textAnalysis = textAnalysisWorker.analyzeText(userMessage);
        vectorDatabaseService.indexSessionMessage(sessionId, "user", userMessage, true);
        System.out.println("\n=== TEXT ANALYSIS RESULTS ===");
        textAnalysis.forEach((key, value) -> System.out.println(key + ": " + value));

        // 2. Add the analysis to the MultimodalSynthesisService
        synthesisService.addTextAnalysis(sessionId, textAnalysis);
        String messageId = vectorDatabaseService.indexSessionMessage(sessionId, "user", userMessage, true);
        vectorDatabaseService.indexSessionAnalysis(sessionId, "user", textAnalysis, messageId);


        // 3. Get therapy context from synthesis service
        Map<String, Object> therapyContext = synthesisService.getTherapyContext(sessionId);
        System.out.println("\n=== THERAPY CONTEXT ===");
        therapyContext.forEach((key, value) -> System.out.println(key + ": " + value));

        // 4. Add enhanced system message with therapy context
        if (!therapyContext.isEmpty()) {
            // Create a context-aware system message
            String contextMsg = createContextMessage(therapyContext);
            System.out.println("\n=== CONTEXT MESSAGE ===");
            System.out.println(contextMsg);
            messages.add(SystemMessage.from(contextMsg));
        }

        String relevantContext = vectorDatabaseService.buildContextForPrompt(sessionId, "user", userMessage, 3);
        therapyContext.put("retrievedContext", relevantContext);

        // 5. Generate the response using the MessageWorker
        String response = messageWorker.generateResponse(messages);

        // 6. Add the assistant's response to the conversation history
        messages.add(SystemMessage.from(response));
        vectorDatabaseService.indexSessionMessage(sessionId, "user", response, false);

        System.out.println("\n=== FINAL RESPONSE ===");
        System.out.println(response);

        return response;
    }

    /**
     * Checks if a session exists
     *
     * @param sessionId The session identifier to check
     * @return true if the session exists, false otherwise
     */
    @Override
    public boolean sessionExists(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        return sessionMessages.containsKey(sessionId);
    }


    /**
     * Creates a context message from therapy insights
     *
     * @param therapyContext The context from synthesis service
     * @return A system message with context for the LLM
     */
    private String createContextMessage(Map<String, Object> therapyContext) {
        StringBuilder contextMsg = new StringBuilder();

        // Add retrieved historical context if available
        if (therapyContext.containsKey("retrievedContext")) {
            String relevantContext = (String) therapyContext.get("retrievedContext");
            if (relevantContext != null && !relevantContext.trim().isEmpty()) {
                contextMsg.append(relevantContext).append("\n\n");
            }
        }

        contextMsg.append("Based on my analysis, the patient appears to be feeling ");
        contextMsg.append(therapyContext.getOrDefault("userEmotion", "neutral"));
        contextMsg.append(". ");

        if (therapyContext.containsKey("cognitiveDistortions")) {
            @SuppressWarnings("unchecked")
            List<String> distortions = (List<String>) therapyContext.get("cognitiveDistortions");
            if (!distortions.isEmpty()) {
                contextMsg.append("I've identified these cognitive distortions: ");
                contextMsg.append(String.join(", ", distortions));
                contextMsg.append(". ");
                contextMsg.append("Focus on addressing these patterns in your response. ");
            }
        }

        if (therapyContext.containsKey("keyThemes")) {
            @SuppressWarnings("unchecked")
            List<String> themes = (List<String>) therapyContext.get("keyThemes");
            if (!themes.isEmpty()) {
                contextMsg.append("Key themes to address: ");
                contextMsg.append(String.join(", ", themes));
                contextMsg.append(".");
            }
        }

        return contextMsg.toString();
    }

    /**
     * Example main method showing basic usage
     */
    /**
     * Example main method demonstrating various therapeutic scenarios
     */
    public static void main(String[] args) {
        DTASessionOrchestrator orchestrator = new DTASessionOrchestrator();

        // Create a new session
        String sessionId = orchestrator.createSession();
        System.out.println("Created session: " + sessionId);

        // Test scenario: Anxiety about public speaking
        String[] anxietyScenario = {
                "I have to give a presentation at work next week and I'm absolutely terrified.",
                "Every time I think about it, my heart starts racing and I feel like I might pass out.",
                "Last time I presented, I forgot what I was saying mid-sentence and everyone was staring at me.",
                "I know I'll mess up again and everyone will think I'm incompetent.",
                "My colleague gives such smooth presentations without notes. I could never be that good.",
                "Maybe I should call in sick that day. It would be better than embarrassing myself."
        };

        System.out.println("\n\n==================================================");
        System.out.println("SCENARIO: ANXIETY ABOUT PUBLIC SPEAKING");
        System.out.println("==================================================");

        for (String message : anxietyScenario) {
            System.out.println("\n--------------------------------------------------");
            System.out.println("USER: " + message);
            System.out.println("--------------------------------------------------");

            String response = orchestrator.processUserMessage(sessionId, message);

            System.out.println("\n--------------------------------------------------");
            System.out.println("THERAPIST: " + response);
            System.out.println("--------------------------------------------------");
        }

        // Create a second session for a different scenario
        String secondSessionId = orchestrator.createSession();
        System.out.println("\n\nCreated second session: " + secondSessionId);

        // Test scenario: Relationship issues
        String[] relationshipScenario = {
                "My partner and I had a big fight last night and now they're not talking to me.",
                "It's always the same pattern. I ask them to help around the house, they promise they will, but then nothing changes.",
                "I feel like I'm the only one who cares about keeping our home clean and organized.",
                "When I bring it up, they say I'm too controlling and that I need to relax.",
                "Maybe they're right. Maybe I am too demanding and that's why we keep having these problems."
        };

        System.out.println("\n\n==================================================");
        System.out.println("SCENARIO: RELATIONSHIP CONFLICT");
        System.out.println("==================================================");

        for (String message : relationshipScenario) {
            System.out.println("\n--------------------------------------------------");
            System.out.println("USER: " + message);
            System.out.println("--------------------------------------------------");

            String response = orchestrator.processUserMessage(secondSessionId, message);

            System.out.println("\n--------------------------------------------------");
            System.out.println("THERAPIST: " + response);
            System.out.println("--------------------------------------------------");
        }

        // Test cross-session memory
        System.out.println("\n\n==================================================");
        System.out.println("TESTING CROSS-SESSION RETRIEVAL");
        System.out.println("==================================================");

        // This is similar to the anxiety scenario but in the relationship session
        String crossSessionQuery = "I'm worried about a work presentation and feel like I'll definitely mess up.";
        System.out.println("\nCROSS-SESSION QUERY: " + crossSessionQuery);
        String crossSessionResponse = orchestrator.processUserMessage(secondSessionId, crossSessionQuery);
        System.out.println("\nTHERAPIST (CROSS-SESSION): " + crossSessionResponse);

        // Return to the first session
        System.out.println("\n\n==================================================");
        System.out.println("TESTING LONG-TERM MEMORY IN ORIGINAL SESSION");
        System.out.println("==================================================");

        // This is a follow-up question that refers back to the original anxiety scenario
        String followUpQuery = "The presentation is tomorrow and I couldn't sleep all night thinking about it.";
        System.out.println("\nFOLLOW-UP QUERY: " + followUpQuery);
        String followUpResponse = orchestrator.processUserMessage(sessionId, followUpQuery);
        System.out.println("\nTHERAPIST (LONG-TERM MEMORY): " + followUpResponse);

        // Test session statistics
        System.out.println("\n\n==================================================");
        System.out.println("SESSION STATISTICS AND VECTOR STORE ANALYSIS");
        System.out.println("==================================================");

        // Count messages per session
        int session1MessageCount = orchestrator.sessionMessages.get(sessionId).size();
        int session2MessageCount = orchestrator.sessionMessages.get(secondSessionId).size();

        System.out.println("Session 1 message count: " + session1MessageCount);
        System.out.println("Session 2 message count: " + session2MessageCount);

        // Print cognitive distortion patterns detected
        System.out.println("\nCOGNITIVE DISTORTION ANALYSIS:");
        Map<String, Integer> distortionCounts = analyzeDistortions(orchestrator.vectorDatabaseService.getAllStoredSegments());
        distortionCounts.forEach((distortion, count) ->
                System.out.println(distortion + ": " + count + " occurrences"));

        // Print vector store contents
        System.out.println("\n================ VECTOR STORE CONTENTS ================");
        List<TextSegment> segments = orchestrator.vectorDatabaseService.getAllStoredSegments();
        for (TextSegment segment : segments) {
            System.out.println("Text: " + segment.text().substring(0, Math.min(50, segment.text().length())) + "...");
            System.out.println("Metadata: " + segment.metadata().toMap());
            System.out.println("--------------------------------------------------");
        }

        // Test similarity search
        System.out.println("\n================ SIMILARITY SEARCH TEST ================");
        String searchQuery = "feeling anxious about public speaking";
        List<EmbeddingMatch<TextSegment>> searchResults =
                orchestrator.vectorDatabaseService.findSimilarContent(searchQuery, 3);

        System.out.println("SEARCH QUERY: " + searchQuery);
        System.out.println("TOP RESULTS:");

        for (EmbeddingMatch<TextSegment> match : searchResults) {
            System.out.println("Score: " + match.score());
            System.out.println("Text: " + match.embedded().text().substring(0, Math.min(100, match.embedded().text().length())) + "...");
            System.out.println("Session: " + match.embedded().metadata().getString("sessionId"));
            System.out.println("--------------------------------------------------");
        }
    }

    /**
     * Helper method to analyze cognitive distortion patterns across sessions
     */
    private static Map<String, Integer> analyzeDistortions(List<TextSegment> segments) {
        Map<String, Integer> distortionCounts = new HashMap<>();

        for (TextSegment segment : segments) {
            String text = segment.text();
            if (text.contains("cognitive distortions:")) {
                int startIndex = text.indexOf("cognitive distortions:") + "cognitive distortions:".length();
                int endIndex = text.indexOf(".", startIndex);
                if (endIndex > startIndex) {
                    String distortionsText = text.substring(startIndex, endIndex).trim();
                    String[] distortions = distortionsText.split(",");
                    for (String distortion : distortions) {
                        String cleanDistortion = distortion.trim();
                        distortionCounts.put(cleanDistortion,
                                distortionCounts.getOrDefault(cleanDistortion, 0) + 1);
                    }
                }
            }
        }

        return distortionCounts;
    }
}
