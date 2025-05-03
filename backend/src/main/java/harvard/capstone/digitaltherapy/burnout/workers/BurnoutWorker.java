package harvard.capstone.digitaltherapy.burnout.workers;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import harvard.capstone.digitaltherapy.burnout.ai.BurnoutScoreCalculator;
import harvard.capstone.digitaltherapy.burnout.ai.BurnoutSummaryGenerator;
import harvard.capstone.digitaltherapy.burnout.ai.MultimodalQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.ai.StandardQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BurnoutWorker {

    private final StandardQuestionGenerator standardQuestionGenerator;
    private final MultimodalQuestionGenerator multimodalQuestionGenerator;
    private final BurnoutScoreCalculator scoreCalculator;
    private final BurnoutSummaryGenerator summaryGenerator;
    private final ChatLanguageModel chatModel;

    /**
     * Constructor that initializes the burnout assessment questions and generates a score.
     */
    public BurnoutWorker() {
        // Initialize the chat language model with Google Gemini
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-1.5-flash")
                .temperature(0.1) // Lower temperature for analysis tasks
                .topP(0.95)
                .maxOutputTokens(300)
                .build();

        this.standardQuestionGenerator = AiServices.builder(StandardQuestionGenerator.class)
                .chatLanguageModel(chatModel)
                .build();

        this.multimodalQuestionGenerator = AiServices.builder(MultimodalQuestionGenerator.class)
                .chatLanguageModel(chatModel)
                .build();

        this.scoreCalculator = AiServices.builder(BurnoutScoreCalculator.class)
                .chatLanguageModel(chatModel)
                .build();

        this.summaryGenerator = AiServices.builder(BurnoutSummaryGenerator.class)
                .chatLanguageModel(chatModel)
                .build();

    }

    /**
     * Generates a complete burnout assessment with questions for all domains
     * - Work: Questions about job stressors and workplace dynamics
     * - Personal: Questions about interpersonal relationships with friends, family, and partners
     * - Lifestyle: Questions about routines, sleep habits, and diet/nutrition
     *
     * For each domain:
     * - 3 standard questions for Likert scale response
     * - 1 multimodal question for video response
     *
     * @return A complete burnout assessment with all questions
     */
    public BurnoutAssessment generateBurnoutAssessment() {
        List<BurnoutQuestion> allQuestions = new ArrayList<>();

        // Generate questions for each domain
        for (AssessmentDomain domain : AssessmentDomain.values()) {
            // Generate 3 standard questions
            List<String> standardQuestions = standardQuestionGenerator.generateStandardQuestions(
                    3,
                    domain.getDisplayName(),
                    domain.getDescription()
            );

            // Create standard questions with unique IDs
            for (int i = 0; i < standardQuestions.size(); i++) {
                String questionId = domain.name().toLowerCase() + "_std_" + (i + 1);
                String question = standardQuestions.get(i);
                allQuestions.add(new BurnoutQuestion(questionId, question, domain, false));
            }

            // Generate 1 multimodal question
            String multimodalQuestion = multimodalQuestionGenerator.generateMultimodalQuestion(
                    domain.getDisplayName(),
                    domain.getDescription()
            );

            // Create multimodal question with unique ID
            String multimodalId = domain.name().toLowerCase() + "_multi_1";
            allQuestions.add(new BurnoutQuestion(multimodalId, multimodalQuestion, domain, true));
        }

        return new BurnoutAssessment(allQuestions);
    }


    public String generateBurnoutSummary(String formattedInput) {
        try {
            return summaryGenerator.generateSummary(formattedInput);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate a summary", e);
        }
    }


    public Map<String, Object> generateBurnoutScore(String formattedInput) {
        try {
            String jsonResponse = scoreCalculator.calculateBurnoutScores(formattedInput);

            // Remove Markdown formatting if present
            String cleanedJson = jsonResponse;
            if (jsonResponse.startsWith("```json")) {
                cleanedJson = jsonResponse.replace("```json", "")
                        .replace("```", "")
                        .trim();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(cleanedJson);

            double scoreValue = root.get("score").asDouble();
            String explanation = root.get("explanation").asText();

            Map<String, Object> result = new HashMap<>();
            result.put("score", scoreValue);
            result.put("explanation", explanation);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate burnout score", e);
        }
    }


}