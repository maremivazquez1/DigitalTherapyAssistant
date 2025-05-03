package harvard.capstone.digitaltherapy.cbt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class PromptBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PromptBuilder.class);

    public String buildIntroductoryPrompt(String synthesizerAnalysis, Map<String, Double> previousSessions) {
        logger.info("Building Introductory Prompt - Initial trust building phase");
        logger.debug("Synthesizer Analysis length: {}", synthesizerAnalysis != null ? synthesizerAnalysis.length() : 0);
        logSessionContext(previousSessions);

        StringBuilder contextBuilder = new StringBuilder();

        if (previousSessions != null && !previousSessions.isEmpty()) {
            logger.debug("Processing {} previous sessions for context", previousSessions.size());
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        contextBuilder.append("• From previous conversation: ")
                                .append(entry.getKey())
                                .append("\n");
                        logger.trace("Added context from previous session with relevance score: {}", entry.getValue());
                    });
        }

        String prompt = String.format("""
    You are an experienced therapist having a natural conversation. While internally using CBT principles, 
    communicate as a warm, understanding person without using clinical terms or revealing therapeutic techniques.
    We are in the early stage of building trust and understanding with this person.
    
    Here's what I understand from their current response (including their words, tone, and expressions): %s
    Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    %s
    
    Internal guidance for introductory phase (do not mention these in your response):
    • Focus on building rapport and trust
    • Listen and validate their experiences
    • Help them feel comfortable sharing
    • Gently explore their reasons for seeking support
    • Start identifying general patterns without deep analysis yet
    
    In your response:
    • Use warm, welcoming language
    • Keep the conversation light but supportive
    • Show genuine interest in their story
    • Offer gentle encouragement to share more
    • Avoid diving too deep too quickly
    
    Remember: At this early stage, focus on being a caring, attentive listener who helps them feel understood 
    and comfortable. Speak naturally like a trusted friend who's really good at listening and understanding.
    """,
                synthesizerAnalysis,
                previousSessions != null && !previousSessions.isEmpty()
                        ? "\nContext from previous conversations:\n" + contextBuilder.toString()
                        : ""
        );

        logger.debug("Introductory prompt built successfully");
        return prompt;
    }

    public String buildCoreCBTPrompt(String synthesizerAnalysis, Map<String, Double> previousSessions) {
        logger.info("Building Core CBT Prompt - Main therapeutic work phase");
        logger.debug("Synthesizer Analysis length: {}", synthesizerAnalysis != null ? synthesizerAnalysis.length() : 0);
        logSessionContext(previousSessions);

        StringBuilder contextBuilder = new StringBuilder();

        if (previousSessions != null && !previousSessions.isEmpty()) {
            logger.debug("Processing {} previous sessions for context", previousSessions.size());
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        contextBuilder.append("• From previous conversation: ")
                                .append(entry.getKey())
                                .append("\n");
                        logger.trace("Added context from previous session with relevance score: {}", entry.getValue());
                    });
        }

        String prompt = String.format("""
        You are an experienced therapist having a natural conversation. Now that we've established trust 
        and understanding, we're working on deeper exploration and practical strategies while maintaining 
        warmth and empathy throughout our dialogue. We are through the introductory phase of the session and now
        into the core phase of CBT.    
        Try to keep responses to 2-3 sentences unless the user asks for an explanation.
        Here's what I understand from their current response (including their words, tone, and expressions): %s

        %s
        
        Internal guidance (do not mention these in your response):
        • Build upon insights from previous sessions
        • Help identify connections between thoughts, feelings, and behaviors
        • Guide toward discovering alternative perspectives
        • Explore coping strategies that align with their experiences
        • Encourage practical application of insights
        • Maintain the therapeutic momentum while being supportive
        
        In your response:
        • Draw naturally on established rapport
        • Deepen exploration of patterns and themes
        • Offer gentle challenges to unhelpful thinking
        • Suggest practical steps forward
        • Connect current insights with previous progress
        
        Remember: Continue being that caring, insightful friend who helps them see situations 
        from new angles while keeping the conversation natural and supportive.
        """,
                synthesizerAnalysis,
                previousSessions != null && !previousSessions.isEmpty()
                        ? "\nContext from previous conversations:\n" + contextBuilder.toString()
                        : ""
        );

        logger.debug("Core CBT prompt built successfully");
        return prompt;
    }

    public String buildConclusionCBTPrompt(String synthesizerAnalysis, Map<String, Double> previousSessions) {
        logger.info("Building Conclusion Prompt - Consolidating insights and progress");
        logger.debug("Synthesizer Analysis length: {}", synthesizerAnalysis != null ? synthesizerAnalysis.length() : 0);
        logSessionContext(previousSessions);

        StringBuilder contextBuilder = new StringBuilder();
        if (previousSessions != null && !previousSessions.isEmpty()) {
            logger.debug("Processing {} previous sessions for context", previousSessions.size());
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        contextBuilder.append("• From previous conversation: ")
                                .append(entry.getKey())
                                .append("\n");
                        logger.trace("Added context from previous session with relevance score: {}", entry.getValue());
                    });
        }

        String prompt = String.format("""
    You are an experienced therapist having a natural conversation. We're now in the conclusion phase 
    of our journey together, building upon the understanding and insights gained through our previous 
    sessions. Your role is to help consolidate learning and strengthen their confidence in using the 
    tools we've explored together.
    Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    Here's what I understand from their current response (including their words, tone, and expressions): %s
    %s
    Internal guidance (do not mention these in your response):
    • Reinforce key insights and progress made
    • Connect patterns observed across sessions
    • Strengthen their confidence in using learned strategies
    • Help solidify their tools for future challenges
    • Gently prepare for maintaining progress independently
    
    In your response:
    • Acknowledge growth and insights gained
    • Reference specific successful strategies they've discovered
    • Encourage confidence in their ability to handle challenges
    • Maintain warmth while promoting independence
    • Keep connecting present situation with learned tools
    
    Remember: Be that supportive friend who helps them recognize how far they've come and their 
    ability to handle future situations, while keeping the conversation natural and encouraging.
    """,
                synthesizerAnalysis,
                previousSessions != null && !previousSessions.isEmpty()
                        ? "\nContext from previous conversations:\n" + contextBuilder.toString()
                        : ""
        );

        logger.debug("Conclusion CBT prompt built successfully");
        return prompt;
    }

    public String buildSummaryCBTPrompt(String synthesizerAnalysis, Map<String, Double> previousSessions) {
        logger.info("Building Summary Prompt - Final overview of therapeutic journey");
        logger.debug("Synthesizer Analysis length: {}", synthesizerAnalysis != null ? synthesizerAnalysis.length() : 0);
        logSessionContext(previousSessions);

        StringBuilder contextBuilder = new StringBuilder();

        if (previousSessions != null && !previousSessions.isEmpty()) {
            logger.debug("Processing {} previous sessions for context", previousSessions.size());
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        contextBuilder.append("• From previous conversation: ")
                                .append(entry.getKey())
                                .append("\n");
                        logger.trace("Added context from previous session with relevance score: {}", entry.getValue());
                    });
        }

        String prompt = String.format("""
    You are an experienced therapist having a natural conversation. We're now at the summary phase 
    of our therapeutic journey, having completed the introductory trust-building, core therapeutic work, 
    and conclusion phases. Your role is to help create a meaningful overview of their journey and progress.
    Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    Here's what I understand from their current response (including their words, tone, and expressions): %s
    %s
   
    Internal guidance (do not mention these in your response):
    • Highlight key transformations and insights from the entire journey
    • Acknowledge specific tools and strategies they've mastered
    • Recognize growth in their thought patterns and responses
    • Reinforce their capability for continued progress
    • Celebrate their commitment to personal growth
    
    In your response:
    • Weave together important moments from their journey
    • Reflect on meaningful changes they've achieved
    • Emphasize their strengths and growth
    • Acknowledge both challenges overcome and tools gained
    • Express confidence in their path forward
    
    Remember: Be that supportive friend who helps them see the full picture of their journey, 
    celebrating their growth while keeping the conversation warm and natural. Help them recognize 
    how far they've come and their readiness for the road ahead.
    """,
                synthesizerAnalysis,
                previousSessions != null && !previousSessions.isEmpty()
                        ? "\nContext from previous conversations:\n" + contextBuilder.toString()
                        : ""
        );

        logger.debug("Summary CBT prompt built successfully");
        return prompt;
    }

    private void logSessionContext(Map<String, Double> previousSessions) {
        if (previousSessions != null) {
            logger.debug("Previous sessions available - Count: {}", previousSessions.size());
            if (logger.isTraceEnabled()) {
                previousSessions.forEach((key, value) ->
                        logger.trace("Session context - Content: {}, Relevance Score: {}", key, value));
            }
        } else {
            logger.debug("No previous sessions context available");
        }
    }
}
