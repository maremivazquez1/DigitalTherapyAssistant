package harvard.capstone.digitaltherapy.burnout.ai;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * Interface for the AI service that generates standard assessment questions
 */
public interface StandardQuestionGenerator {
    @SystemMessage("You are an expert in mental health and burnout assessment. "
            + "Create professional, evidence-based burnout assessment questions that allow users to self-reflect on their experiences. "
            + "The questions should be framed as statements that a person would rate on a 7-point Likert scale from 0 (Never) to 6 (Every day). "
            + "The questions should be clear, direct, and focused on the specific domain.")
    @UserMessage("Generate {{count}} burnout assessment statements for the {{domain}} domain ({{description}}). "
            + "These should be statements that users would rate on a scale of 0-6. "
            + "Format each question on its own line with no numbering or prefixes.")
    List<String> generateStandardQuestions(
            @V("count") int count,
            @V("domain") String domain,
            @V("description") String description);
}
