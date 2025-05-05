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

    public List<ChatMessage> buildIntroductoryPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
        StringBuilder contextBuilder = buildContextString(previousSessions);

        String systemPrompt = String.format("""
            You are an experienced therapist in the Introduction Phase of the therapy session. Your main goal is to establish rapport and create a safe space.
            Keep responses brief (2-3 sentences) unless the user asks for more detail.

            Guidelines:
            • Focus on the current message analysis
            • Use simple, reflective listening
            • Ask basic open-ended questions about their current situation
            • Avoid referencing past sessions unless the client brings them up
            • Mirror the client’s language without interpretation
            • Show interest without probing deeply
            • Incorporate insights from all available data sources to understand the client's state more fully, but do not directly reference any analysis modality (e.g., audio, text, facial).
            """);
        String userPrompt = String.format("""
            CURRENT MESSAGE ANALYSIS:
            Client's current emotional state, tone, and expressions:
                %s

            PREVIOUS SESSION ANALYSIS HISTORY (use minimally):
            Past interactions and context:
            %s
            """,
                synthesizerAnalysis,
                contextBuilder.toString()
        );
        context.add(SystemMessage.from(systemPrompt));
        context.add(UserMessage.from(userPrompt));
        return context;
    }

    public List<ChatMessage> buildCoreCBTPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
        StringBuilder contextBuilder = buildContextString(previousSessions);

        String systemPrompt = String.format("""
            You are an experienced CBT therapist in the Core Therapy Phase.
            Keep responses concise (2-3 sentences) unless the user requests more explanation.

            Response Guidelines:
            • Address current thoughts and emotions from the message analysis
            • Connect to relevant patterns from session history
            • Implement CBT techniques based on current content
            • Reference past sessions when highlighting patterns
            • Incorporate insights from all available data sources to understand the client's state more fully, but do not directly reference any analysis modality (e.g., audio, text, facial).
            
            Therapeutic Techniques:
            • Use Socratic questioning for current thoughts
            • Connect present patterns to past discussions
            • Introduce new CBT techniques or build on previously used ones
            • Guide through structured problem-solving
            """);
        String userPrompt = String.format("""
            CURRENT MESSAGE ANALYSIS (70%% focus):
            Client's immediate emotional state and thought patterns:
                %s

            PREVIOUS SESSION ANALYSIS HISTORY (30%% focus):
            Relevant past discussions and patterns:
                %s
            """,
                synthesizerAnalysis,
                contextBuilder.toString()
        );

        context.add(SystemMessage.from(systemPrompt));
        context.add(UserMessage.from(userPrompt));
        return context;
    }

    public List<ChatMessage> buildConclusionCBTPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
        StringBuilder contextBuilder = buildContextString(previousSessions);

        String systemPrompt = String.format("""
            In the Progress Phase, focus on reviewing past sessions to highlight the client’s growth and mastery of techniques.
            Keep responses concise (2-3 sentences) unless more explanation is requested.

            Guidelines:
            • Start with relevant insights from previous sessions
            • Connect past learning to the current situation
            • Highlight specific techniques the client has mastered
            • Relate current challenges to past successes
            • Use examples from session history to demonstrate progress
            • Encourage independent application of learned techniques
            • Incorporate insights from all available data sources to understand the client's state more fully, but do not directly reference any analysis modality (e.g., audio, text, facial).
            """);

        String userPrompt = String.format("""
            PREVIOUS SESSION ANALYSIS HISTORY (70%% focus):
            Review of therapy journey and progress patterns:
            %s

            CURRENT MESSAGE ANALYSIS (30%% focus):
            Client's current state and thoughts:
            %s
            """,
                contextBuilder.toString(),
                synthesizerAnalysis
        );

        context.add(SystemMessage.from(systemPrompt));
        context.add(UserMessage.from(userPrompt));
        return context;
    }

    public List<ChatMessage> buildSummaryCBTPrompt(String synthesizerAnalysis, Map<String, String> previousSessions, List<ChatMessage> context) {
        StringBuilder contextBuilder = buildContextString(previousSessions);
        String systemPrompt = String.format("""
            You are an experienced therapist in the Summary Phase.
            Keep responses brief (2-3 sentences) unless the user asks for more detail.

            Focus on final reflections:
            • Provide a comprehensive review of the therapy journey
            • Highlight major breakthroughs and progress
            • Compare early challenges with current capabilities
            • Use current message to reinforce overall growth and achievements
            • Incorporate insights from all available data sources to understand the client's state more fully, but do not directly reference any analysis modality (e.g., audio, text, facial).
            """);

        String userPrompt = String.format("""
            PREVIOUS SESSION ANALYSIS HISTORY (80%% focus):
            Complete therapy journey and progress patterns:
            %s

            CURRENT MESSAGE ANALYSIS (20%% focus):
            Client's final session state:
            %s
            """,
                contextBuilder.toString(),
                synthesizerAnalysis
        );

        context.add(SystemMessage.from(systemPrompt));
        context.add(UserMessage.from(userPrompt));
        return context;
    }

    private StringBuilder buildContextString(Map<String, String> previousSessions) {
        StringBuilder contextBuilder = new StringBuilder();
        if (previousSessions != null && !previousSessions.isEmpty()) {
            previousSessions.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByValue())  // Remove .reversed() if you want ascending order
                    .limit(25)
                    .forEach(entry -> contextBuilder.append("• Previous: ")
                            .append(entry.getKey())
                            .append(": ")        // Optional: add a separator between key and value
                            .append(entry.getValue())  // Add this line to include the text content
                            .append("\n"));
        }
        return contextBuilder;
    }


}
