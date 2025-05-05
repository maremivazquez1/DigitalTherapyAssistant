package harvard.capstone.digitaltherapy.cbt.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromptBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PromptBuilder.class);

    // public List<ChatMessage> buildIntroductoryPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
    //     StringBuilder contextBuilder = buildContextString(previousSessions);

    //     String systemPrompt = String.format("""
    //         You are an experienced therapist in the INITIAL PHASE of therapy. Your primary goal is establishing rapport 
    //         and creating a safe space. DO NOT dive into problem-solving or CBT techniques yet.
    //         Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    //         STRICT GUIDELINES FOR THIS INITIAL PHASE:
    //             1. Focus primarily on the current message analysis
    //             2. Use SIMPLE and reflective listening
    //             3. Ask BASIC open-ended questions about their current situation
    //             4. AVOID referencing past sessions unless client brings them up
    //             5. Keep responses BRIEF (2-3 sentences maximum)
    
    //         REQUIRED RESPONSE STYLE:
    //             • Respond mainly to their current emotional state
    //             • Use phrases like "I'm here to listen" or "Tell me more about that"
    //             • Show interest but don't probe deeply
    //             • Mirror their current language without interpretation
    
    //             Remember: Focus on their current message and emotions. Minimal reference to past sessions.
    //         """);
    //     String userPrompt = String.format("""
    //         CURRENT MESSAGE ANALYSIS (Primary focus for this phase):
    //         Client's current emotional state, tone, and expressions:
    //             %s

    //         PREVIOUS SESSION ANALYSIS HISTORY (Use minimally in this phase):
    //         Past interactions and context:
    //         %s
    //         """,
    //             synthesizerAnalysis,
    //             contextBuilder.toString()
    //     );
    //     context.add(SystemMessage.from(systemPrompt));
    //     context.add(UserMessage.from(userPrompt));
    //     return context;
    // }

    public String buildCoreCBTPrompt(String stage, String synthesizerAnalysis, String userResponse, String approaches, String previousSessions) {
        String userPrompt = String.format("""
            STAGE: %s

            CLIENT MESSAGE ANALYSIS:
            %s

            CLIENT MESSAGE:
            %s

            ANALYSIS RECOMMENDED APPROACHES:
            %s

            PAST SESSIONS (for reference):
            %s
            """,
                stage,
                synthesizerAnalysis,
                userResponse,
                approaches,
                previousSessions
        );
        return userPrompt;
    }

    // public List<ChatMessage> buildConclusionCBTPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
    //     StringBuilder contextBuilder = buildContextString(previousSessions);

    //     String systemPrompt = String.format("""
    // You are an experienced therapist in the CONSOLIDATION PHASE. 
    // Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    // PREVIOUS SESSION HISTORY (Primary focus - 70%%):
    
    // RESPONSE REQUIREMENTS:
    // 1. START with relevant insights from previous sessions
    // 2. CONNECT past learning to current situation
    // 3. HIGHLIGHT specific techniques they've mastered (from session history)
    // 4. RELATE current challenges to past successes
    
    // KEY INSTRUCTIONS:
    // • Heavily reference specific examples from session history
    // • Use current message to reinforce learned skills
    // • Demonstrate progress by comparing past and present responses
    // • Guide towards independent application of learned techniques
    
    // Focus: Use session history to show progress while addressing current situation.
    // """
    //     );

    //     String userPrompt = String.format("""
    // PREVIOUS SESSION ANALYSIS HISTORY (Primary focus - 70%%):
    // Review of therapy journey and progress patterns:
    // %s
   
    // CURRENT MESSAGE ANALYSIS (Secondary focus - 30%%):
    // Client's current state and thoughts:
    // %s
    // """,
    //             contextBuilder.toString(),  // Note: Switched order to emphasize history
    //             synthesizerAnalysis
    //     );

    //     context.add(SystemMessage.from(systemPrompt));
    //     context.add(UserMessage.from(userPrompt));
    //     return context;
    // }

    // public List<ChatMessage> buildSummaryCBTPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
    //     StringBuilder contextBuilder = buildContextString(previousSessions);
    //     String systemPrompt = String.format("""
    // You are an experienced therapist in the FINAL SUMMARY PHASE. 
    // Try to keep responses to 2-3 sentences unless the user asks for an explanation.
    
    // CRITICAL INSTRUCTIONS:
    // 1. PRIORITIZE comprehensive review of session history
    // 2. HIGHLIGHT major breakthroughs from previous sessions
    // 3. DEMONSTRATE progress using specific examples from past sessions
    // 4. CONNECT early challenges to current capabilities
    
    // REQUIRED RESPONSE STRUCTURE:
    // 1. Begin with key insights from therapy journey (session history)
    // 2. Reference specific examples of growth from past sessions
    // 3. Compare early sessions to current capabilities
    // 4. Use current message only to reinforce overall progress
    
    // EXAMPLE FRAMEWORK:
    // • "Throughout our sessions, you've shown significant growth in [specific examples from history]..."
    // • "From our early discussions about [past challenge] to now handling [current situation]..."
    // • "You've developed these key skills: [list from session history]..."
    
    // Focus: Create a comprehensive review based primarily on session history, using current message only to reinforce progress.
    // """);

    //     String userPrompt = String.format("""
    //         PREVIOUS SESSION ANALYSIS HISTORY (Primary focus - 80%%):
    //         Complete therapy journey and progress patterns:
    //         %s
    //         CURRENT MESSAGE ANALYSIS (Minor focus - 20%%):
    //         Client's final session state:
    //         %s
    //         """,
    //             contextBuilder.toString(),  // Note: Switched order to emphasize history
    //             synthesizerAnalysis
    //     );

    //     context.add(SystemMessage.from(systemPrompt));
    //     context.add(UserMessage.from(userPrompt));
    //     return context;
    // }

    // private StringBuilder buildContextString(Map<String, String> previousSessions) {
    //     StringBuilder contextBuilder = new StringBuilder();
    //     System.out.println("PreviousSessions Size: " + previousSessions.size());
    //     if (previousSessions != null && !previousSessions.isEmpty()) {

    //         previousSessions.forEach((key, value) ->
    //             System.out.println("DEBUG previousSessions entry - key: " 
    //                 + key + ", value: " + value)
    //         );

    //         previousSessions.entrySet().stream()
    //                 .sorted(Map.Entry.<String, String>comparingByValue())  // Remove .reversed() if you want ascending order
    //                 .limit(25)
    //                 .forEach(entry -> contextBuilder.append("• Previous: ")
    //                         .append(entry.getKey())
    //                         .append(": ")        // Optional: add a separator between key and value
    //                         .append(entry.getValue())  // Add this line to include the text content
    //                         .append("\n"));
    //     }
    //     return contextBuilder;
    // }
}
