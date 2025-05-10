package harvard.capstone.digitaltherapy.burnout.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BurnoutModelTests {

    private BurnoutQuestion question1, question2, question3;
    private List<BurnoutQuestion> questionList;
    private BurnoutAssessment assessment;
    private LocalDateTime now;

    @BeforeEach
    public void setUp() {
        now = LocalDateTime.now();

        // Create test questions
        question1 = new BurnoutQuestion("q1", "How often do you feel exhausted after work?",
                AssessmentDomain.WORK, false);
        question2 = new BurnoutQuestion("q2", "Describe your current stress level",
                AssessmentDomain.PERSONAL, true);
        question3 = new BurnoutQuestion("q3", "How many hours do you sleep each night?",
                AssessmentDomain.LIFESTYLE, false);

        questionList = Arrays.asList(question1, question2, question3);
        assessment = new BurnoutAssessment(questionList);
    }

    @Test
    public void testAssessmentDomain() {
        assertEquals("Work", AssessmentDomain.WORK.getDisplayName());
        assertEquals("Questions about job stressors and workplace dynamics",
                AssessmentDomain.WORK.getDescription());

        assertEquals("Personal", AssessmentDomain.PERSONAL.getDisplayName());
        assertEquals("Questions about interpersonal relationships with friends, family, and partners",
                AssessmentDomain.PERSONAL.getDescription());

        assertEquals("Lifestyle", AssessmentDomain.LIFESTYLE.getDisplayName());
        assertEquals("Questions about routines, sleep habits, and diet/nutrition",
                AssessmentDomain.LIFESTYLE.getDescription());
    }

    @Test
    public void testBurnoutQuestion() {
        assertEquals("q1", question1.getQuestionId());
        assertEquals("How often do you feel exhausted after work?", question1.getQuestion());
        assertEquals(AssessmentDomain.WORK, question1.getDomain());
        assertFalse(question1.isMultimodal());

        assertEquals("q2", question2.getQuestionId());
        assertEquals("Describe your current stress level", question2.getQuestion());
        assertEquals(AssessmentDomain.PERSONAL, question2.getDomain());
        assertTrue(question2.isMultimodal());

        // Test toString method
        assertEquals("How often do you feel exhausted after work?", question1.toString());
    }

    @Test
    public void testBurnoutAssessment() {
        // Test getQuestions
        assertEquals(3, assessment.getQuestions().size());
        assertTrue(assessment.getQuestions().contains(question1));
        assertTrue(assessment.getQuestions().contains(question2));
        assertTrue(assessment.getQuestions().contains(question3));

        // Test getQuestionsForDomain
        assertEquals(1, assessment.getQuestionsForDomain(AssessmentDomain.WORK).size());
        assertEquals(question1, assessment.getQuestionsForDomain(AssessmentDomain.WORK).get(0));

        // Test getStandardQuestions
        assertEquals(2, assessment.getStandardQuestions().size());
        assertTrue(assessment.getStandardQuestions().contains(question1));
        assertTrue(assessment.getStandardQuestions().contains(question3));
        assertFalse(assessment.getStandardQuestions().contains(question2));

        // Test getMultimodalQuestions
        assertEquals(1, assessment.getMultimodalQuestions().size());
        assertEquals(question2, assessment.getMultimodalQuestions().get(0));
    }

    @Test
    public void testBurnoutSessionCreationResponse() {
        BurnoutSessionCreationResponse response = new BurnoutSessionCreationResponse("session123", questionList);

        assertEquals("session123", response.getSessionId());
        assertEquals(3, response.getQuestions().size());
        assertTrue(response.getQuestions().contains(question1));

        // Test equals and hashCode
        BurnoutSessionCreationResponse responseSame = new BurnoutSessionCreationResponse("session123", questionList);
        BurnoutSessionCreationResponse responseDifferent = new BurnoutSessionCreationResponse("differentSession", questionList);

        assertEquals(response, responseSame);
        assertEquals(response.hashCode(), responseSame.hashCode());
        assertNotEquals(response, responseDifferent);
        assertNotEquals(response.hashCode(), responseDifferent.hashCode());

        // Test toString
        String toString = response.toString();
        assertTrue(toString.contains("session123"));
        assertTrue(toString.contains("questionsCount=3"));
    }

    @Test
    public void testBurnoutUserResponse() {
        Map<String, Object> insights = new HashMap<>();
        insights.put("video", "{\"emotions\": [\"stressed\", \"tired\"]}");
        insights.put("audio", "{\"tone\": \"anxious\"}");

        BurnoutUserResponse response = new BurnoutUserResponse("q1", "I feel very stressed", insights);

        assertEquals("q1", response.getQuestionId());
        assertEquals("I feel very stressed", response.getTextResponse());
        assertEquals(insights, response.getMultimodalInsights());
        assertEquals(2, response.getMultimodalInsights().size());
        assertEquals("{\"emotions\": [\"stressed\", \"tired\"]}", response.getMultimodalInsights().get("video"));
    }

    @Test
    public void testBurnoutScore() {
        // Test constructor with explanation
        BurnoutScore score = new BurnoutScore("session123", "user456", 7.5, "High burnout detected");

        assertEquals("session123", score.getSessionId());
        assertEquals("user456", score.getUserId());
        assertEquals(7.5, score.getOverallScore());
        assertEquals("High burnout detected", score.getExplanation());

        // Test constructor without explanation
        BurnoutScore scoreNoExplanation = new BurnoutScore("session123", "user456", 7.5);

        assertEquals("session123", scoreNoExplanation.getSessionId());
        assertEquals("user456", scoreNoExplanation.getUserId());
        assertEquals(7.5, scoreNoExplanation.getOverallScore());
        assertNull(scoreNoExplanation.getExplanation());
    }

    @Test
    public void testBurnoutSummary() {
        BurnoutSummary summary = new BurnoutSummary("session123", "You are showing signs of moderate burnout");

        assertEquals("session123", summary.getSessionId());
        assertEquals("You are showing signs of moderate burnout", summary.getOverallInsight());
    }

    @Test
    public void testBurnoutAssessmentSession() {
        BurnoutAssessmentSession session = new BurnoutAssessmentSession("session123", "user456", assessment, now);

        // Test initial state
        assertEquals("session123", session.getSessionId());
        assertEquals("user456", session.getUserId());
        assertEquals(assessment, session.getAssessment());
        assertTrue(session.getResponses().isEmpty());
        assertFalse(session.isCompleted());
        assertNull(session.getCompletedAt());
        assertNull(session.getScore());
        assertNull(session.getSummary());

        // Test setting completed
        session.setCompleted(true);
        assertTrue(session.isCompleted());

        // Test setting completedAt
        LocalDateTime completedTime = LocalDateTime.now();
        session.setCompletedAt(completedTime);
        assertEquals(completedTime, session.getCompletedAt());

        // Test setting score
        BurnoutScore score = new BurnoutScore("session123", "user456", 7.5, "High burnout detected");
        session.setScore(score);
        assertEquals(score, session.getScore());

        // Test setting summary
        BurnoutSummary summary = new BurnoutSummary("session123", "You are showing signs of moderate burnout");
        session.setSummary(summary);
        assertEquals(summary, session.getSummary());

        // Test adding responses
        Map<String, Object> insights = new HashMap<>();
        BurnoutUserResponse userResponse = new BurnoutUserResponse("q1", "I feel very stressed", insights);
        session.getResponses().put("q1", userResponse);
        assertEquals(1, session.getResponses().size());
        assertEquals(userResponse, session.getResponses().get("q1"));
    }

    @Test
    public void testBurnoutAssessmentResult() {
        // Create necessary objects
        BurnoutScore score = new BurnoutScore("session123", "user456", 7.5, "High burnout detected");
        Map<String, BurnoutUserResponse> responses = new HashMap<>();

        Map<String, Object> insights = new HashMap<>();
        insights.put("video", "{\"emotions\": [\"stressed\", \"tired\"]}");

        BurnoutUserResponse userResponse = new BurnoutUserResponse("q1", "I feel very stressed", insights);
        responses.put("q1", userResponse);

        LocalDateTime completedAt = LocalDateTime.now();

        // Create result object
        BurnoutAssessmentResult result = new BurnoutAssessmentResult(
                "session123", "user456", assessment, responses, score,
                "You are showing signs of moderate burnout", completedAt);

        // Test getters
        assertEquals("session123", result.getSessionId());
        assertEquals("user456", result.getUserId());
        assertEquals(assessment, result.getAssessment());
        assertEquals(responses, result.getResponses());
        assertEquals(score, result.getScore());
        assertEquals("You are showing signs of moderate burnout", result.getSummary());
        assertEquals(completedAt, result.getCompletedAt());

        // Test responses content
        assertEquals(1, result.getResponses().size());
        assertEquals(userResponse, result.getResponses().get("q1"));
    }
}
