package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the BurnoutAssessmentOrchestrator's remaining methods,
 * specifically focusing on completeAssessment and related private methods.
 */
@ExtendWith(MockitoExtension.class)
class BurnoutAssessmentOrchestratorCompleteAssessmentTest {

    @Mock
    private BurnoutWorker burnoutWorker;

    private BurnoutAssessmentOrchestrator orchestrator;
    private String sessionId;
    private String userId;

    @BeforeEach
    void setUp() {
        // Create a spy of the orchestrator
        orchestrator = spy(new BurnoutAssessmentOrchestrator());

        // Set up a session for testing
        userId = "testUser123";
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        sessionId = response.getSessionId();

        // Use reflection to replace the burnoutWorker with mock
        try {
            java.lang.reflect.Field workerField = BurnoutAssessmentOrchestrator.class.getDeclaredField("burnoutWorker");
            workerField.setAccessible(true);
            workerField.set(orchestrator, burnoutWorker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up mock", e);
        }
    }

    @Test
    void completeAssessment_shouldCalculateScoreAndGenerateSummary() {
        // Arrange
        // Answer all questions
        BurnoutAssessmentSession session = getSession(sessionId);
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Mock burnoutWorker methods
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(
                Map.of("score", 3.5, "explanation", "Moderate burnout detected")
        );
        when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn(
                "User shows signs of moderate burnout with emotional exhaustion."
        );

        // Act
        BurnoutAssessmentResult result = orchestrator.completeAssessment(sessionId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getScore().getOverallScore()).isEqualTo(3.5);
        assertThat(result.getSummary()).contains("moderate burnout");

        // Verify session was marked as completed
        session = getSession(sessionId);
        assertThat(session.isCompleted()).isTrue();
        assertThat(session.getCompletedAt()).isNotNull();

        // Verify worker methods were called
        verify(burnoutWorker).generateBurnoutScore(anyString());
        verify(burnoutWorker).generateBurnoutSummary(anyString());
    }

    @Test
    void completeAssessment_shouldThrowException_whenSessionDoesNotExist() {
        // Arrange
        String nonExistentSessionId = "nonExistentSession";

        // Act & Assert
        assertThatThrownBy(() -> orchestrator.completeAssessment(nonExistentSessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No session found");
    }

    @Test
    void completeAssessment_shouldReuseExistingScoreAndSummary_whenAlreadyCalculated() {
        // Arrange
        // Answer all questions
        BurnoutAssessmentSession session = getSession(sessionId);
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Mock burnoutWorker methods for first call
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(
                Map.of("score", 3.5, "explanation", "Moderate burnout detected")
        );
        when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn(
                "User shows signs of moderate burnout with emotional exhaustion."
        );

        // Act - Complete assessment first time
        BurnoutAssessmentResult result1 = orchestrator.completeAssessment(sessionId);

        // Reset mocks to verify they aren't called again
        reset(burnoutWorker);

        // Act - Complete assessment second time
        BurnoutAssessmentResult result2 = orchestrator.completeAssessment(sessionId);

        // Assert
        assertThat(result2).isNotNull();
        assertThat(result2.getScore().getOverallScore()).isEqualTo(result1.getScore().getOverallScore());
        assertThat(result2.getSummary()).isEqualTo(result1.getSummary());

        // Verify worker methods were not called again
        verify(burnoutWorker, never()).generateBurnoutScore(anyString());
        verify(burnoutWorker, never()).generateBurnoutSummary(anyString());
    }

    @Test
    void calculateScore_shouldReturnCorrectScore() throws Exception {
        // Arrange
        // Answer all questions
        BurnoutAssessmentSession session = getSession(sessionId);
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Mock burnoutWorker.generateBurnoutScore method
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(
                Map.of("score", 3.5, "explanation", "Moderate burnout detected")
        );

        // Get the calculateScore method via reflection
        Method calculateScoreMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "calculateScore", String.class);
        calculateScoreMethod.setAccessible(true);

        // Act
        BurnoutScore score = (BurnoutScore) calculateScoreMethod.invoke(orchestrator, sessionId);

        // Assert
        assertThat(score).isNotNull();
        assertThat(score.getOverallScore()).isEqualTo(3.5);
        assertThat(score.getExplanation()).isEqualTo("Moderate burnout detected");

        // Verify the score was stored in the session
        session = getSession(sessionId);
        assertThat(session.getScore()).isEqualTo(score);

        // Verify worker method was called with formatted input
        verify(burnoutWorker).generateBurnoutScore(anyString());
    }

    @Test
    void calculateScore_shouldThrowException_whenNoResponsesRecorded() throws Exception {
        // Arrange
        // Don't record any responses

        // Get the calculateScore method via reflection
        Method calculateScoreMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "calculateScore", String.class);
        calculateScoreMethod.setAccessible(true);

        // Act & Assert
        assertThatThrownBy(() -> calculateScoreMethod.invoke(orchestrator, sessionId))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("No session found or no responses recorded");
    }

    @Test
    void generateSummary_shouldReturnCorrectSummary() throws Exception {
        // Arrange
        // Answer all questions
        BurnoutAssessmentSession session = getSession(sessionId);
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Mock burnoutWorker methods
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(
                Map.of("score", 3.5, "explanation", "Moderate burnout detected")
        );
        when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn(
                "User shows signs of moderate burnout with emotional exhaustion."
        );

        // First calculate the score (required for generateSummary)
        Method calculateScoreMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "calculateScore", String.class);
        calculateScoreMethod.setAccessible(true);
        calculateScoreMethod.invoke(orchestrator, sessionId);

        // Get the generateSummary method via reflection
        Method generateSummaryMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "generateSummary", String.class);
        generateSummaryMethod.setAccessible(true);

        // Act
        BurnoutSummary summary = (BurnoutSummary) generateSummaryMethod.invoke(orchestrator, sessionId);

        // Assert
        assertThat(summary).isNotNull();
        assertThat(summary.getOverallInsight()).isEqualTo("User shows signs of moderate burnout with emotional exhaustion.");

        // Verify the summary was stored in the session
        session = getSession(sessionId);
        assertThat(session.getSummary()).isEqualTo(summary);

        // Verify worker method was called with formatted input
        verify(burnoutWorker).generateBurnoutSummary(anyString());
    }

    @Test
    void generateSummary_shouldThrowException_whenScoreNotCalculated() throws Exception {
        // Arrange
        // Answer all questions but don't calculate score
        BurnoutAssessmentSession session = getSession(sessionId);
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Get the generateSummary method via reflection
        Method generateSummaryMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "generateSummary", String.class);
        generateSummaryMethod.setAccessible(true);

        // Act & Assert
        assertThatThrownBy(() -> generateSummaryMethod.invoke(orchestrator, sessionId))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("No session found or score not calculated");
    }

    @Test
    void formatUserResponsesForWorker_shouldCorrectlyFormatResponses() throws Exception {
        // Arrange
        // Add some responses with different types of content
        BurnoutAssessmentSession session = getSession(sessionId);
        BurnoutQuestion question1 = session.getAssessment().getQuestions().get(0);
        BurnoutQuestion question2 = session.getAssessment().getQuestions().get(1);

        // Record a response with text only
        orchestrator.recordResponse(sessionId, question1.getQuestionId(), "Simple text response", null, null);

        // Record a response with multimodal insights
        orchestrator.recordResponse(sessionId, question2.getQuestionId(), "Multimodal response", null, null);

        // Add multimodal insights manually
        BurnoutUserResponse response2 = session.getResponses().get(question2.getQuestionId());
        response2.getMultimodalInsights().put("audio", "{\"emotion\":\"neutral\"}");
        response2.getMultimodalInsights().put("video", "{\"face\":\"detected\"}");

        // Get the formatUserResponsesForWorker method via reflection
        Method formatMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "formatUserResponsesForWorker", BurnoutAssessmentSession.class);
        formatMethod.setAccessible(true);

        // Act
        String formattedInput = (String) formatMethod.invoke(orchestrator, session);

        // Assert
        assertThat(formattedInput).isNotNull();
        assertThat(formattedInput).contains("Q: " + question1.getQuestion());
        assertThat(formattedInput).contains("A: Simple text response");
        assertThat(formattedInput).contains("Q: " + question2.getQuestion());
        assertThat(formattedInput).contains("A: Multimodal response");
        assertThat(formattedInput).contains("Multimodal: {audio={\"emotion\":\"neutral\"}, video={\"face\":\"detected\"}}");

        // Should contain separators
        assertThat(formattedInput.split("---").length).isGreaterThanOrEqualTo(3); // At least 2 questions + potential trailing
    }

    @Test
    void formatUserResponsesForWorker_shouldHandleMissingResponses() throws Exception {
        // Arrange
        // Don't add any responses
        BurnoutAssessmentSession session = getSession(sessionId);

        // Get the formatUserResponsesForWorker method via reflection
        Method formatMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "formatUserResponsesForWorker", BurnoutAssessmentSession.class);
        formatMethod.setAccessible(true);

        // Act
        String formattedInput = (String) formatMethod.invoke(orchestrator, session);

        // Assert
        assertThat(formattedInput).isNotNull();
        // Should indicate no responses for each question
        for (BurnoutQuestion question : session.getAssessment().getQuestions()) {
            assertThat(formattedInput).contains("Q: " + question.getQuestion());
            assertThat(formattedInput).contains("A: [No response]");
        }
    }

    // Helper method to get the session through reflection
    private BurnoutAssessmentSession getSession(String sessionId) {
        try {
            java.lang.reflect.Field activeSessionsField = BurnoutAssessmentOrchestrator.class.getDeclaredField("activeSessions");
            activeSessionsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, BurnoutAssessmentSession> activeSessions =
                    (Map<String, BurnoutAssessmentSession>) activeSessionsField.get(orchestrator);

            return activeSessions.get(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get session", e);
        }
    }
}
