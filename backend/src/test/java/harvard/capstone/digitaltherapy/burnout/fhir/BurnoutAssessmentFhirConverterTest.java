package harvard.capstone.digitaltherapy.burnout.fhir;

import harvard.capstone.digitaltherapy.burnout.model.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BurnoutAssessmentFhirConverterTest {

    private BurnoutAssessmentFhirConverter converter;
    private BurnoutAssessmentResult sampleResult;
    private LocalDateTime completedAt;

    @BeforeEach
    public void setUp() {
        converter = new BurnoutAssessmentFhirConverter();
        completedAt = LocalDateTime.of(2025, 4, 15, 10, 30, 0);

        // Create a sample BurnoutAssessmentResult for testing
        sampleResult = createSampleBurnoutAssessmentResult();
    }

    @Test
    public void testBasicMetadataConversion() {
        // Act
        QuestionnaireResponse response = converter.convertToFhir(sampleResult);

        // Assert
        assertEquals("session123", response.getId());
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, response.getStatus());
        assertEquals(
                Date.from(completedAt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
                response.getAuthored().getTime()
        );
        assertEquals("Patient/user456", response.getSubject().getReference());
    }

    @Test
    public void testQuestionAnswerConversion() {
        // Act
        QuestionnaireResponse response = converter.convertToFhir(sampleResult);

        // Assert - Find questions in the response items
        boolean q1Found = false;
        boolean q2Found = false;

        for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem()) {
            if ("q1".equals(item.getLinkId())) {
                q1Found = true;
                assertEquals("How often do you feel exhausted?", item.getText());
                assertEquals(1, item.getAnswer().size());
                assertEquals("I feel exhausted frequently", ((StringType)item.getAnswer().get(0).getValue()).getValue());
            }
            else if ("q2".equals(item.getLinkId())) {
                q2Found = true;
                assertEquals("Describe your stress level", item.getText());
                assertEquals(1, item.getAnswer().size());
                assertEquals("My stress level is very high", ((StringType)item.getAnswer().get(0).getValue()).getValue());
            }
        }

        assertTrue(q1Found, "Question 1 not found in response items");
        assertTrue(q2Found, "Question 2 not found in response items");
    }

    @Test
    public void testScoreConversion() {
        // Act
        QuestionnaireResponse response = converter.convertToFhir(sampleResult);

        // Assert - Check for score item
        boolean scoreFound = false;

        for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem()) {
            if ("score".equals(item.getLinkId())) {
                scoreFound = true;
                assertEquals("Calculated Burnout Score", item.getText());
                assertEquals(1, item.getAnswer().size());
                assertEquals(7.5, ((DecimalType)item.getAnswer().get(0).getValue()).getValue().doubleValue());
            }
        }

        assertTrue(scoreFound, "Score not found in response items");
    }

    @Test
    public void testSummaryConversion() {
        // Act
        QuestionnaireResponse response = converter.convertToFhir(sampleResult);

        // Assert - Check for summary item
        boolean summaryFound = false;

        for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem()) {
            if ("summary".equals(item.getLinkId())) {
                summaryFound = true;
                assertEquals("Assessment Summary", item.getText());
                assertEquals(1, item.getAnswer().size());
                assertEquals("You are showing signs of moderate burnout", ((StringType)item.getAnswer().get(0).getValue()).getValue());
            }
        }

        assertTrue(summaryFound, "Summary not found in response items");
    }

    @Test
    public void testMissingResponses() {
        // Create a result with missing responses
        BurnoutAssessmentResult resultWithMissingResponses = createResultWithMissingResponses();

        // Act
        QuestionnaireResponse response = converter.convertToFhir(resultWithMissingResponses);

        // Assert - Only q1 should be in the response, not q2
        boolean q1Found = false;
        boolean q2Found = false;

        for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem()) {
            if ("q1".equals(item.getLinkId())) {
                q1Found = true;
            }
            else if ("q2".equals(item.getLinkId())) {
                q2Found = true;
            }
        }

        assertTrue(q1Found, "Question 1 not found in response items");
        assertFalse(q2Found, "Question 2 should not be in response items");
    }

    @Test
    public void testCompleteConversion() {
        // Act
        QuestionnaireResponse response = converter.convertToFhir(sampleResult);

        // Assert - Check overall structure
        // 2 questions + 1 score + 1 summary = 4 items
        assertEquals(4, response.getItem().size());
    }

    // Helper method to create a sample BurnoutAssessmentResult for testing
    private BurnoutAssessmentResult createSampleBurnoutAssessmentResult() {
        // Create questions
        BurnoutQuestion q1 = new BurnoutQuestion("q1", "How often do you feel exhausted?", AssessmentDomain.WORK, false);
        BurnoutQuestion q2 = new BurnoutQuestion("q2", "Describe your stress level", AssessmentDomain.PERSONAL, true);
        List<BurnoutQuestion> questions = Arrays.asList(q1, q2);

        // Create assessment
        BurnoutAssessment assessment = new BurnoutAssessment(questions);

        // Create user responses
        Map<String, BurnoutUserResponse> responses = new HashMap<>();

        // Response for q1
        Map<String, Object> insights1 = new HashMap<>();
        BurnoutUserResponse response1 = new BurnoutUserResponse("q1", "I feel exhausted frequently", insights1);
        responses.put("q1", response1);

        // Response for q2
        Map<String, Object> insights2 = new HashMap<>();
        insights2.put("video", "{\"emotions\": [\"stressed\", \"tired\"]}");
        insights2.put("audio", "{\"tone\": \"anxious\"}");
        BurnoutUserResponse response2 = new BurnoutUserResponse("q2", "My stress level is very high", insights2);
        responses.put("q2", response2);

        // Create score
        BurnoutScore score = new BurnoutScore("session123", "user456", 7.5, "High burnout detected");

        // Create assessment result
        return new BurnoutAssessmentResult(
                "session123",
                "user456",
                assessment,
                responses,
                score,
                "You are showing signs of moderate burnout",
                completedAt
        );
    }

    // Helper method to create a result with missing responses
    private BurnoutAssessmentResult createResultWithMissingResponses() {
        // Create questions
        BurnoutQuestion q1 = new BurnoutQuestion("q1", "How often do you feel exhausted?", AssessmentDomain.WORK, false);
        BurnoutQuestion q2 = new BurnoutQuestion("q2", "Describe your stress level", AssessmentDomain.PERSONAL, true);
        List<BurnoutQuestion> questions = Arrays.asList(q1, q2);

        // Create assessment
        BurnoutAssessment assessment = new BurnoutAssessment(questions);

        // Create user responses - only answering q1
        Map<String, BurnoutUserResponse> responses = new HashMap<>();

        // Response for q1 only
        Map<String, Object> insights1 = new HashMap<>();
        BurnoutUserResponse response1 = new BurnoutUserResponse("q1", "I feel exhausted frequently", insights1);
        responses.put("q1", response1);

        // Create score
        BurnoutScore score = new BurnoutScore("session123", "user456", 7.5, "High burnout detected");

        // Create assessment result
        return new BurnoutAssessmentResult(
                "session123",
                "user456",
                assessment,
                responses,
                score,
                "You are showing signs of moderate burnout",
                completedAt
        );
    }
}