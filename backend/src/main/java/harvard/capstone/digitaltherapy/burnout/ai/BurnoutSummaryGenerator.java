package harvard.capstone.digitaltherapy.burnout.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interface for the AI service that summarizes burnout assessment sessions.
 */
public interface BurnoutSummaryGenerator {
    @SystemMessage("You are a mental health professional trained to interpret burnout assessments. "
            + "Your task is to generate a concise 4–5 sentence summary based on the user’s responses to a burnout assessment. "
            + "Responses are rated on a 0–6 scale (0 = Never, 6 = Every day), and may include multimodal inputs such as text, tone of voice, and facial expression. "
            + "Your summary should describe the types of questions asked, highlight areas where the user rated themselves positively or negatively, and include insights from the user's verbal and textual responses. "
            + "Use concise, user-directed language (e.g., 'Today your burnout assessment shows you...'). Do not provide advice or recommendations.")
    @UserMessage("Review the following burnout assessment session:\n"
            + "{{questionsAndResponses}}\n\n"
            + "Generate a 4–5 sentence summary that includes:\n"
            + "- What kinds of questions were asked,\n"
            + "- Where the user rated themselves positively or negatively,\n"
            + "- Any insights from their text and spoken responses.\n\n"
            + "Use direct, personal language aimed at the user. Do not include any advice or suggestions.")
    String generateSummary(
            @V("questionsAndResponses") String questionsAndResponses);
}
