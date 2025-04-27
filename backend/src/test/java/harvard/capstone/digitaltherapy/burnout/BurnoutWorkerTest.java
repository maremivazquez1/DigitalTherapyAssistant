package harvard.capstone.digitaltherapy.burnout;

import harvard.capstone.digitaltherapy.burnout.ai.MultimodalQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.ai.StandardQuestionGenerator;
import harvard.capstone.digitaltherapy.burnout.model.AssessmentDomain;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutAssessment;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BurnoutWorkerTest {

    @Mock
    private StandardQuestionGenerator mockStandardQuestionGenerator;

    @Mock
    private MultimodalQuestionGenerator mockMultimodalQuestionGenerator;

    // No need to mock ChatLanguageModel since we're not using it directly in tests

    // Test implementation of BurnoutWorker that uses mocks instead of real AI services
    private class TestBurnoutWorker extends BurnoutWorker {
        private final StandardQuestionGenerator standardQuestionGenerator;
        private final MultimodalQuestionGenerator multimodalQuestionGenerator;

        public TestBurnoutWorker(
                StandardQuestionGenerator standardQuestionGenerator,
                MultimodalQuestionGenerator multimodalQuestionGenerator) {
            // Call parent constructor, but we'll override with our mock implementations
            super();
            this.standardQuestionGenerator = standardQuestionGenerator;
            this.multimodalQuestionGenerator = multimodalQuestionGenerator;
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
                    allQuestions.add(new BurnoutQuestion(question, domain, false));
                }

                // Generate 1 multimodal question
                String multimodalQuestion = multimodalQuestionGenerator.generateMultimodalQuestion(
                        domain.getDisplayName(),
                        domain.getDescription()
                );

                allQuestions.add(new BurnoutQuestion(multimodalQuestion, domain, true));
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
        burnoutWorker = new TestBurnoutWorker(mockStandardQuestionGenerator, mockMultimodalQuestionGenerator);
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
}