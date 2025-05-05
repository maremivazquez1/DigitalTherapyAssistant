package harvard.capstone.digitaltherapy.burnout.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Interface for the AI service that generates multimodal assessment questions
 */
public interface MultimodalQuestionGenerator {
    @SystemMessage("You are an expert in mental health and burnout assessment. "
            + "Create professional prompts that ask users to provide a video response about their experiences related to a specific domain. "
            + "The prompts should encourage users to share their thoughts and feelings, providing a basis for multimodal analysis of their "
            + "facial expressions, voice tone, and emotional state. The questions should be open-ended but focused.")
    @UserMessage("Only generate a single sentence. Create a video response prompt for the {{domain}} domain ({{description}}). "
            + "The prompt should ask users to describe a specific experience or feeling related to this domain "
            + "that would reveal emotional and behavioral markers of potential burnout through video analysis.")
    String generateMultimodalQuestion(
            @V("domain") String domain,
            @V("description") String description);
}
