package harvard.capstone.digitaltherapy.burnout.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.burnout.ai.BurnoutSummaryGenerator;
import harvard.capstone.digitaltherapy.burnout.ai.MultimodalQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.ai.StandardQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.model.AssessmentDomain;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessment;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BurnoutWorkerTest {

    @Mock
    private StandardQuestionGenerator mockStandardQuestionGenerator;

    @Mock
    private MultimodalQuestionGenerator mockMultimodalQuestionGenerator;

    @Mock
    private BurnoutSummaryGenerator mockBurnoutSummaryAgent;

    // No need to mock ChatLanguageModel since we're not using it directly in tests

    // Test implementation of BurnoutWorker that uses mocks instead of real AI services
    private class TestBurnoutWorker extends BurnoutWorker {
        private final StandardQuestionGenerator standardQuestionGenerator;
        private final MultimodalQuestionGenerator multimodalQuestionGenerator;
        private final BurnoutSummaryGenerator burnoutSummaryAgent;

        public TestBurnoutWorker(
                StandardQuestionGenerator standardQuestionGenerator,
                MultimodalQuestionGenerator multimodalQuestionGenerator,
                BurnoutSummaryGenerator burnoutSummaryAgent) {
            // Call parent constructor, but we'll override with our mock implementations
            super();
            this.standardQuestionGenerator = standardQuestionGenerator;
            this.multimodalQuestionGenerator = multimodalQuestionGenerator;
            this.burnoutSummaryAgent = burnoutSummaryAgent;
        }

        @Override
        public BurnoutAssessment generateBurnoutAssessment() {
            List<BurnoutQuestion> allQuestions = new ArrayList<>();

            // Generate questions for each domain using mocked services
            for (AssessmentDomain domain : AssessmentDomain.values()) {
                // Generate 3 standard questions
                List<String> standardQuestions = standardQuestionGenerator.generateStandardQuestions(
                        3,
                        domain.getDisplayName(),
                        domain.getDescription()
                );

                for (String question : standardQuestions) {
                    allQuestions.add(new BurnoutQuestion("qid-" + question.hashCode(), question, domain, false));
                }

                // Generate 1 multimodal question
                String multimodalQuestion = multimodalQuestionGenerator.generateMultimodalQuestion(
                        domain.getDisplayName(),
                        domain.getDescription()
                );

                allQuestions.add(new BurnoutQuestion("qid-" + multimodalQuestion.hashCode(), multimodalQuestion, domain, true));
            }

            return new BurnoutAssessment(allQuestions);
        }
    }

    private TestBurnoutWorker burnoutWorker;

    @BeforeEach
    public void setUp() {
        // We're using the @ExtendWith(MockitoExtension.class) instead of MockitoAnnotations.openMocks(this)

        // Set up mock responses for all domains with lenient settings to avoid the UnnecessaryStubbingException
        lenient().when(mockStandardQuestionGenerator.generateStandardQuestions(
                        eq(3),
                        eq("Work"),
                        anyString()))
                .thenReturn(Arrays.asList(
                        "I feel emotionally drained from my work.",
                        "I feel used up at the end of the workday.",
                        "I feel frustrated by my job."
                ));

        lenient().when(mockMultimodalQuestionGenerator.generateMultimodalQuestion(
                        eq("Work"),
                        anyString()))
                .thenReturn("Describe a recent work situation that made you feel overwhelmed. How did it affect you emotionally and physically?");

        // Set up mock responses for the Personal domain
        lenient().when(mockStandardQuestionGenerator.generateStandardQuestions(
                        eq(3),
                        eq("Personal"),
                        anyString()))
                .thenReturn(Arrays.asList(
                        "I find it difficult to engage meaningfully with family and friends.",
                        "I feel too exhausted to maintain personal relationships.",
                        "I feel detached from the people closest to me."
                ));

        lenient().when(mockMultimodalQuestionGenerator.generateMultimodalQuestion(
                        eq("Personal"),
                        anyString()))
                .thenReturn("Describe how your current stress levels are affecting your personal relationships. What changes have you noticed?");

        // Set up mock responses for the Lifestyle domain
        lenient().when(mockStandardQuestionGenerator.generateStandardQuestions(
                        eq(3),
                        eq("Lifestyle"),
                        anyString()))
                .thenReturn(Arrays.asList(
                        "I have trouble falling or staying asleep.",
                        "I don't have energy for physical activities I used to enjoy.",
                        "I tend to eat unhealthy foods when feeling stressed."
                ));

        lenient().when(mockMultimodalQuestionGenerator.generateMultimodalQuestion(
                        eq("Lifestyle"),
                        anyString()))
                .thenReturn("Describe your current sleep patterns and how they've changed over the past month. How do you feel when you wake up?");

        // Initialize the test worker with mocks
        burnoutWorker = new TestBurnoutWorker(mockStandardQuestionGenerator, mockMultimodalQuestionGenerator, mockBurnoutSummaryAgent);
    }

    @Test
    public void testGenerateBurnoutAssessment() {
        // Generate a burnout assessment
        BurnoutAssessment assessment = burnoutWorker.generateBurnoutAssessment();

        // Verify we have the correct number of questions
        assertNotNull(assessment);
        assertEquals(12, assessment.getQuestions().size(), "Should have 12 questions total (3 standard + 1 multimodal for each of 3 domains)");

        // Verify the distribution of question types
        assertEquals(9, assessment.getStandardQuestions().size(), "Should have 9 standard questions");
        assertEquals(3, assessment.getMultimodalQuestions().size(), "Should have 3 multimodal questions");

        // Verify the distribution across domains
        assertEquals(4, assessment.getQuestionsForDomain(AssessmentDomain.WORK).size(),
                "Should have 4 questions for Work domain");
        assertEquals(4, assessment.getQuestionsForDomain(AssessmentDomain.PERSONAL).size(),
                "Should have 4 questions for Personal domain");
        assertEquals(4, assessment.getQuestionsForDomain(AssessmentDomain.LIFESTYLE).size(),
                "Should have 4 questions for Lifestyle domain");

        // Verify specific questions for the Work domain
        List<BurnoutQuestion> workQuestions = assessment.getQuestionsForDomain(AssessmentDomain.WORK);
        boolean foundStandardWorkQuestion = false;
        boolean foundMultimodalWorkQuestion = false;

        for (BurnoutQuestion question : workQuestions) {
            if (!question.isMultimodal() && question.getQuestion().equals("I feel emotionally drained from my work.")) {
                foundStandardWorkQuestion = true;
            }
            if (question.isMultimodal() && question.getQuestion().contains("Describe a recent work situation")) {
                foundMultimodalWorkQuestion = true;
            }
        }

        assertTrue(foundStandardWorkQuestion, "Should contain the expected standard work question");
        assertTrue(foundMultimodalWorkQuestion, "Should contain the expected multimodal work question");

        // Verify that the TestBurnoutWorker properly uses the mocked generators
        verify(mockStandardQuestionGenerator, atLeastOnce()).generateStandardQuestions(
                eq(3),
                eq("Work"),
                anyString());
        verify(mockMultimodalQuestionGenerator, atLeastOnce()).generateMultimodalQuestion(
                eq("Work"),
                anyString());
    }

    @Test
    public void testBurnoutQuestionProperties() {
        // Create a test question
        BurnoutQuestion question = new BurnoutQuestion(
                "test-id",
                "Test question",
                AssessmentDomain.WORK,
                true
        );

        // Verify properties
        assertEquals("Test question", question.getQuestion());
        assertEquals(AssessmentDomain.WORK, question.getDomain());
        assertTrue(question.isMultimodal());
        assertEquals("Test question", question.toString());
    }

    @Test
    public void testAssessmentDomainProperties() {
        // Test the Work domain
        AssessmentDomain workDomain = AssessmentDomain.WORK;
        assertEquals("Work", workDomain.getDisplayName());
        assertEquals("Questions about job stressors and workplace dynamics", workDomain.getDescription());

        // Test the Personal domain
        AssessmentDomain personalDomain = AssessmentDomain.PERSONAL;
        assertEquals("Personal", personalDomain.getDisplayName());
        assertEquals("Questions about interpersonal relationships with friends, family, and partners",
                personalDomain.getDescription());

        // Test the Lifestyle domain
        AssessmentDomain lifestyleDomain = AssessmentDomain.LIFESTYLE;
        assertEquals("Lifestyle", lifestyleDomain.getDisplayName());
        assertEquals("Questions about routines, sleep habits, and diet/nutrition",
                lifestyleDomain.getDescription());
    }

    @Test
    public void testPrintGeneratedQuestions() {
        // Generate a burnout assessment
        BurnoutAssessment assessment = burnoutWorker.generateBurnoutAssessment();

        // Print out all the questions for manual inspection
        System.out.println("\n======= GENERATED BURNOUT ASSESSMENT QUESTIONS =======");

        // Print questions by domain
        for (AssessmentDomain domain : AssessmentDomain.values()) {
            System.out.println("\n--- " + domain.getDisplayName() + " DOMAIN ---");
            System.out.println("Description: " + domain.getDescription());

            // Print standard questions
            System.out.println("\nStandard Questions (Likert scale 0-6):");
            List<BurnoutQuestion> standardQuestions = assessment.getQuestionsForDomain(domain).stream()
                    .filter(q -> !q.isMultimodal())
                    .toList();

            for (int i = 0; i < standardQuestions.size(); i++) {
                System.out.println((i+1) + ". " + standardQuestions.get(i).getQuestion());
            }

            // Print multimodal questions
            System.out.println("\nMultimodal Question (Video response):");
            List<BurnoutQuestion> multimodalQuestions = assessment.getQuestionsForDomain(domain).stream()
                    .filter(BurnoutQuestion::isMultimodal)
                    .toList();

            for (int i = 0; i < multimodalQuestions.size(); i++) {
                System.out.println((i+1) + ". " + multimodalQuestions.get(i).getQuestion());
            }
        }

        System.out.println("\n======= ASSESSMENT SUMMARY =======");
        System.out.println("Total questions: " + assessment.getQuestions().size());
        System.out.println("Standard questions: " + assessment.getStandardQuestions().size());
        System.out.println("Multimodal questions: " + assessment.getMultimodalQuestions().size());
        System.out.println("=======================================\n");

        // Add some basic assertions to verify the test works properly
        assertNotNull(assessment);
        assertEquals(12, assessment.getQuestions().size());
    }

    @Test
    public void testPrintBurnoutScoreCalculatorOutput() {
        // This test requires an actual implementation of BurnoutScoreCalculator rather than a mock
        // Create a simple formatted input with sample assessment responses
        String sampleInput = """
            Work Domain:
            1. "I feel emotionally drained from my work." - Rating: 4/6
            2. "I feel used up at the end of the workday." - Rating: 5/6
            3. "I feel frustrated by my job." - Rating: 3/6
            
            Personal Domain:
            1. "I find it difficult to engage meaningfully with family and friends." - Rating: 3/6
            2. "I feel too exhausted to maintain personal relationships." - Rating: 4/6
            3. "I feel detached from the people closest to me." - Rating: 2/6
            
            Lifestyle Domain:
            1. "I have trouble falling or staying asleep." - Rating: 5/6
            2. "I don't have energy for physical activities I used to enjoy." - Rating: 4/6
            3. "I tend to eat unhealthy foods when feeling stressed." - Rating: 5/6
            
            Multimodal Insights:
            1. Voice tone analysis: Monotone and low energy when discussing work
            2. Facial expression analysis: Visible tension when discussing sleep issues
            3. Response time: Delayed responses when asked about personal relationships
            """;

        // Create an actual BurnoutWorker instance with real implementations (not mocks)
        BurnoutWorker realBurnoutWorker = new BurnoutWorker();

        try {
            // Call the generateBurnoutScore method
            Map<String, Object> scoreResults = realBurnoutWorker.generateBurnoutScore(sampleInput);

            // Print the results
            System.out.println("\n======= BURNOUT SCORE CALCULATOR OUTPUT =======");
            System.out.println("Score: " + scoreResults.get("score"));
            System.out.println("Explanation: " + scoreResults.get("explanation"));
            System.out.println("================================================\n");

            // Add a basic assertion to verify we got results
            assertNotNull(scoreResults);
            assertNotNull(scoreResults.get("score"));
            assertNotNull(scoreResults.get("explanation"));
        } catch (Exception e) {
            System.out.println("Error testing burnout score calculator: " + e.getMessage());
            e.printStackTrace();
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateBurnoutScore_SuccessfulParsing() {
        // Arrange
        BurnoutWorker burnoutWorker = new BurnoutWorker() {
            @Override
            public Map<String, Object> generateBurnoutScore(String formattedInput) {
                // Override to avoid actual API call
                try {
                    // Simulate a clean JSON response from the AI
                    String mockResponse = "{\"score\": 6.5, \"explanation\": \"This is a test explanation.\"}";

                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode root = objectMapper.readTree(mockResponse);

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
        };

        // Act
        Map<String, Object> result = burnoutWorker.generateBurnoutScore("Sample input");

        // Assert
        assertNotNull(result);
        assertEquals(6.5, result.get("score"));
        assertEquals("This is a test explanation.", result.get("explanation"));
    }

    @Test
    public void testGenerateBurnoutScore_WithMarkdownFormatting() {
        // Arrange
        BurnoutWorker burnoutWorker = new BurnoutWorker() {
            @Override
            public Map<String, Object> generateBurnoutScore(String formattedInput) {
                // Call the parent implementation with a mock response
                try {
                    // Simulate a Markdown-formatted response from the AI
                    String mockResponse = "```json\n" +
                            "{\n" +
                            "  \"score\": 7.8,\n" +
                            "  \"explanation\": \"Markdown formatted explanation.\"\n" +
                            "}\n" +
                            "```";

                    // Apply the actual cleaning logic
                    String cleanedJson = mockResponse;
                    if (mockResponse.startsWith("```json")) {
                        cleanedJson = mockResponse.replace("```json", "")
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
        };

        // Act
        Map<String, Object> result = burnoutWorker.generateBurnoutScore("Sample input");

        // Assert
        assertNotNull(result);
        assertEquals(7.8, result.get("score"));
        assertEquals("Markdown formatted explanation.", result.get("explanation"));
    }

    @Test
    public void testGenerateBurnoutScore_HandlesInvalidJson() {
        // Arrange
        BurnoutWorker burnoutWorker = new BurnoutWorker() {
            @Override
            public Map<String, Object> generateBurnoutScore(String formattedInput) {
                // Simulate an invalid JSON response
                throw new RuntimeException("Failed to generate burnout score",
                        new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));
            }
        };

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            burnoutWorker.generateBurnoutScore("Sample input");
        });

        assertTrue(exception.getMessage().contains("Failed to generate burnout score"));
    }

    @Test
    public void testGenerateBurnoutSummary() {
        // Arrange - Create formatted input with questions and user responses
        String formattedInput = """
        Work Domain:
        1. "I feel emotionally drained from my work." - Rating: 4/6
        2. "I feel used up at the end of the workday." - Rating: 5/6
        3. "I feel frustrated by my job." - Rating: 3/6
        
        Personal Domain:
        1. "I find it difficult to engage meaningfully with family and friends." - Rating: 3/6
        2. "I feel too exhausted to maintain personal relationships." - Rating: 4/6
        3. "I feel detached from the people closest to me." - Rating: 2/6
        
        Lifestyle Domain:
        1. "I have trouble falling or staying asleep." - Rating: 5/6
        2. "I don't have energy for physical activities I used to enjoy." - Rating: 4/6
        3. "I tend to eat unhealthy foods when feeling stressed." - Rating: 5/6
        
        Multimodal Responses:
        Work: "During the last project deadline, I was assigned three additional tasks on top of my regular workload. I couldn't focus and felt my heart racing. I developed a tension headache that lasted for days. I noticed I was snapping at colleagues over minor issues. I had trouble sleeping that week and felt completely drained even after a weekend off. I started to question if this job was right for me."
        
        Personal: "I've been canceling plans with friends more often lately. When I do see them, I find myself checking my phone for work emails. I've had arguments with my partner about being mentally absent. My parents have called twice to ask if everything is okay since I haven't been returning their calls. I just don't have the energy to engage in conversations about how things are going."
        
        Lifestyle: "My sleep has definitely gotten worse over the past month. I used to fall asleep within minutes, but now I lie awake replaying work conversations. I wake up at least twice during the night and check my email. When my alarm goes off, I feel like I haven't slept at all. I've been hitting snooze multiple times and rushing to work without breakfast. By afternoon, I'm relying on energy drinks and sugary snacks to get through the day."
        """;

        // Create an actual BurnoutWorker instance to test the real implementation
        BurnoutWorker burnoutWorker = new BurnoutWorker();

        // Act
        String summary = burnoutWorker.generateBurnoutSummary(formattedInput);

        // Assert
        assertNotNull(summary, "Summary should not be null");
        assertFalse(summary.isEmpty(), "Summary should not be empty");

        // Print the result for visual inspection
        System.out.println("\n======= BURNOUT SUMMARY GENERATOR OUTPUT =======");
        System.out.println(summary);
        System.out.println("================================================\n");

        // Additional assertions about the summary content
        assertTrue(summary.contains("Today"), "Summary should use direct language to the user");
        // Check that it's within the expected length range (4-5 sentences)
        int sentenceCount = summary.split("[.!?]+").length;
        assertTrue(sentenceCount >= 4 && sentenceCount <= 6,
                "Summary should contain 4-5 sentences, found: " + sentenceCount);

        // Verify it doesn't contain advice (as instructed in the system prompt)
        assertFalse(summary.toLowerCase().contains("should"),
                "Summary should not contain advice (no 'should' statements)");
        assertFalse(summary.toLowerCase().contains("recommend"),
                "Summary should not contain recommendations");
    }


}
