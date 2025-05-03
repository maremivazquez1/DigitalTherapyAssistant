package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BurnoutAssessmentOrchestrator
 */
@ExtendWith(MockitoExtension.class)
class BurnoutAssessmentOrchestratorTest {

    @Mock
    private BurnoutWorker burnoutWorker;

    @Mock
    private VideoAnalysisWorker videoAnalysisWorker;

    @Mock
    private AudioAnalysisWorker audioAnalysisWorker;


    private BurnoutAssessmentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // Any common setup for all tests can go here
        orchestrator = new BurnoutAssessmentOrchestrator();
    }

    @Test
    void createAssessmentSession_shouldCreateNewSessionWithUniqueSessionId() {
        // Arrange
        String userId = "user123";

        // Act
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotBlank();
        assertThat(response.getSessionId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); // UUID format
    }

    @Test
    void createAssessmentSession_shouldReturnResponseWithSessionIdAndQuestions() {
        // Arrange
        String userId = "user123";

        // Act
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotBlank();
        assertThat(response.getQuestions()).isNotNull();
        assertThat(response.getQuestions()).isNotEmpty();
    }

    @Test
    void createAssessmentSession_shouldStoreSessionForSubsequentUse() {
        // Arrange
        String userId = "user123";

        // Act - Create a session
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Get the first question ID from the response to use for recording a response
        String questionId = response.getQuestions().get(0).getQuestionId();

        // Assert - Try to record a response for this session, which should work if the session was stored
        boolean recordResult = orchestrator.recordResponse(sessionId, questionId, "Test response", null, null);

        assertThat(recordResult).isTrue();
    }

    @Test
    void createAssessmentSession_shouldInitializeSessionWithCorrectUserId() {
        // Arrange
        String userId = "user123";

        // Act - Create a session
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Record a response for all questions to allow completing the assessment
        for (BurnoutQuestion question : response.getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Complete the assessment to get the result
        BurnoutAssessmentResult result = orchestrator.completeAssessment(sessionId);

        // Assert - Verify the userId is correct
        assertThat(result.getUserId()).isEqualTo(userId);
    }




}
