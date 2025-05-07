package harvard.capstone.digitaltherapy.burnout.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interface for the AI service that summarizes burnout assessment sessions.
 */
public interface BurnoutSummaryGenerator {
    @SystemMessage("You are a mental health professional trained to interpret burnout assessments. "
            + "Your task is to generate a synthesized, user-facing summary of the assessment session. "
            + "The session includes responses to questions rated on a 0–6 scale (0 = Never, 6 = Every day), and may include multimodal inputs such as text, tone of voice, and facial expression. "
            + "Your summary should be 4–5 concise sentences that interpret the user's state based on their responses, highlighting areas of strength or concern. "
            + "Do not repeat the input verbatim. Instead, identify patterns or emotional cues where present, and translate them into user-facing insights. "
            + "If multimodal insights are unavailable, omit them without mentioning their absence. "
            + "Use neutral, personal language (e.g., 'Today your burnout assessment shows you...') and do not offer advice or recommendations.")
    @UserMessage("Review the following burnout assessment session:\n"
            + "{{questionsAndResponses}}\n\n"
            + "Generate a summary (4–5 sentences) that:\n"
            + "- Describes the types of questions asked,\n"
            + "- Highlights where the user rated themselves positively or negatively,\n"
            + "- Synthesizes any observable emotional patterns from text or speech.\n\n"
            + "Avoid repeating the input. Instead, interpret the user's responses and convey meaningful insights in a natural, user-facing tone. "
            + "Do not reference the presence or absence of video/audio inputs. "
            + "Use direct, personal language. Do not give advice or recommendations.")
    String generateSummary(
            @V("questionsAndResponses") String questionsAndResponses);
}
