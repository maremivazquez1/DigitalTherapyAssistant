package harvard.capstone.digitaltherapy.burnout.ai;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interface for the AI service that calculates burnout scores.
 */
public interface BurnoutScoreCalculator {
    @SystemMessage("You are a mental health professional specializing in burnout assessment scoring. "
            + "Your task is to analyze the responses to burnout assessment questions and calculate a single numerical burnout score. "
            + "Responses are rated on a 0-6 scale (0 = Never, 6 = Every day), and some may include multimodal insights such as text, voice tone, and facial expressions.")
    @UserMessage("Review the following burnout assessment responses rated on a 0–6 scale (0 = Never, 6 = Every day):\n"
            + "{{questionsAndResponses}}\n\n"
            + "Based on the overall patterns in the responses and multimodal insights, calculate a single overall burnout score on a scale of 0–10. "
            + "Then, provide a 3-sentence explanation for the score."
            + "The explanation should be concise."
            + "Return your response as a JSON object with two fields: 'score' (a numeric value) and 'explanation' (a string).")
    String calculateBurnoutScores(
            @V("questionsAndResponses") String questionsAndResponses);
}
