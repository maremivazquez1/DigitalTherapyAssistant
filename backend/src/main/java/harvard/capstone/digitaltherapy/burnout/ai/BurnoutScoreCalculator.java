package harvard.capstone.digitaltherapy.burnout.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interface for the AI service that calculates burnout scores.
 */
public interface BurnoutScoreCalculator {
    @SystemMessage("You are a mental health professional specializing in burnout assessment scoring. "
            + "Your job is to analyze the responses to burnout assessment questions and calculate numerical scores "
            + "across different burnout dimensions: Emotional Exhaustion, Depersonalization/Cynicism, "
            + "Reduced Personal Accomplishment, and Physical Symptoms. Consider the responses on a 0-6 scale where "
            + "higher scores indicate higher levels of burnout.")
    @UserMessage("Questions and responses for {{domain}} burnout assessment (0=Never, 6=Every day):\n"
            + "{{questionsAndResponses}}\n\n"
            + "Calculate numerical scores for each burnout dimension and an overall burnout score on a scale of 0-10. "
            + "Format your response as follows:\n"
            + "Emotional Exhaustion Score: [score]/10\n"
            + "Depersonalization/Cynicism Score: [score]/10\n"
            + "Reduced Personal Accomplishment Score: [score]/10\n"
            + "Physical Symptoms Score: [score]/10\n"
            + "Overall Burnout Score: [score]/10")
    String calculateBurnoutScores(
            @V("domain") String domain,
            @V("questionsAndResponses") String questionsAndResponses);
}
