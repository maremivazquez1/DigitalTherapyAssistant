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




}
