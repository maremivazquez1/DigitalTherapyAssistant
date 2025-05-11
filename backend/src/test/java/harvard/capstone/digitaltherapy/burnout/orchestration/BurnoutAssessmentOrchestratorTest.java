package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.burnout.workers.BurnoutWorker;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
        // Create orchestrator and manually inject mocks
        orchestrator = new BurnoutAssessmentOrchestrator();

        // Use reflection to set the private fields
        ReflectionTestUtils.setField(orchestrator, "burnoutWorker", burnoutWorker);
        ReflectionTestUtils.setField(orchestrator, "videoAnalysisWorker", videoAnalysisWorker);
        ReflectionTestUtils.setField(orchestrator, "audioAnalysisWorker", audioAnalysisWorker);

        // Mock the behavior of burnoutWorker to return a test assessment
        when(burnoutWorker.generateBurnoutAssessment()).thenReturn(createMockAssessment());

        // Only configure the stubs needed for specific tests with lenient()
        // This tells Mockito not to be strict about these stubs if they aren't used
        lenient().when(videoAnalysisWorker.detectFacesFromVideoAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{\"jobStatus\":\"SUCCEEDED\",\"faces\":[]}"));

        lenient().when(audioAnalysisWorker.analyzeAudioAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{\"results\":{\"emotions\":[]}}"));

        // Mock behavior for score generation
        Map<String, Object> mockScoreResult = new HashMap<>();
        mockScoreResult.put("score", 3.5);
        mockScoreResult.put("explanation", "Mock explanation");
        lenient().when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(mockScoreResult);

        // Mock behavior for summary generation
        lenient().when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn("Mock insight summary");
    }

    private BurnoutAssessment createMockAssessment() {
        List<BurnoutQuestion> questions = new ArrayList<>();

        // Add a few test questions with correct constructor parameters
        questions.add(new BurnoutQuestion(
                "q1",
                "How often do you feel tired?",
                AssessmentDomain.WORK,
                false
        ));

        questions.add(new BurnoutQuestion(
                "q2",
                "Describe your work-life balance",
                AssessmentDomain.PERSONAL,
                true
        ));

        questions.add(new BurnoutQuestion(
                "q3",
                "Rate your job satisfaction",
                AssessmentDomain.WORK,
                false
        ));

        return new BurnoutAssessment(questions);
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

        // Verify the mock was called
        verify(burnoutWorker).generateBurnoutAssessment();
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
        assertThat(response.getQuestions()).hasSize(3); // We created 3 mock questions
        assertThat(response.getQuestions().get(0).getQuestionId()).isEqualTo("q1");
        assertThat(response.getQuestions().get(1).getQuestionId()).isEqualTo("q2");
    }

    @Test
    void createAssessmentSession_shouldStoreSessionForSubsequentUse() {
        // Arrange
        String userId = "user123";

        // Act - Create a session
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Get the first question ID from the response
        String questionId = response.getQuestions().get(0).getQuestionId();

        // Act - Record a response for this session
        boolean recordResult = orchestrator.recordResponse(sessionId, questionId, "Test response", null, null);

        // Assert - Verify the response was recorded successfully
        assertThat(recordResult).isTrue();
    }

    @Test
    void recordResponse_shouldHandleMultimodalContent() {
        // Arrange
        String userId = "user123";
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Get the multimodal question ID (q2 in our mock)
        String questionId = "q2";
        String videoUrl = "s3://bucket/video.mp4";
        String audioUrl = "s3://bucket/audio.mp3";

        // Configure mocks specifically for this test case
        when(videoAnalysisWorker.detectFacesFromVideoAsync(videoUrl))
                .thenReturn(CompletableFuture.completedFuture("{\"jobStatus\":\"SUCCEEDED\",\"faces\":[]}"));

        when(audioAnalysisWorker.analyzeAudioAsync(audioUrl))
                .thenReturn(CompletableFuture.completedFuture("{\"results\":{\"emotions\":[]}}"));

        // Act
        boolean result = orchestrator.recordResponse(sessionId, questionId, "Text response", videoUrl, audioUrl);

        // Assert
        assertThat(result).isTrue();
        verify(videoAnalysisWorker).detectFacesFromVideoAsync(videoUrl);
        verify(audioAnalysisWorker).analyzeAudioAsync(audioUrl);
    }

    @Test
    void createAssessmentSession_shouldInitializeSessionWithCorrectUserId() {
        // Arrange
        String userId = "user123";

        // Act - Create a session
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Setup stubs for this specific test
        Map<String, Object> mockScoreResult = new HashMap<>();
        mockScoreResult.put("score", 3.5);
        mockScoreResult.put("explanation", "Mock explanation");
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(mockScoreResult);
        when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn("Mock insight summary");

        // Record a response for all questions to allow completing the assessment
        for (BurnoutQuestion question : response.getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Complete the assessment to get the result
        BurnoutAssessmentResult result = orchestrator.completeAssessment(sessionId);

        // Assert - Verify the userId is correct
        assertThat(result.getUserId()).isEqualTo(userId);

        // Verify the burnoutWorker methods were called for score and summary generation
        verify(burnoutWorker).generateBurnoutScore(anyString());
        verify(burnoutWorker).generateBurnoutSummary(anyString());
    }

    @Test
    void completeAssessment_shouldGenerateScoreAndSummary() {
        // Arrange
        String userId = "user123";
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        String sessionId = response.getSessionId();

        // Setup stubs for this specific test
        Map<String, Object> mockScoreResult = new HashMap<>();
        mockScoreResult.put("score", 3.5);
        mockScoreResult.put("explanation", "Mock explanation");
        when(burnoutWorker.generateBurnoutScore(anyString())).thenReturn(mockScoreResult);
        when(burnoutWorker.generateBurnoutSummary(anyString())).thenReturn("Mock insight summary");

        // Record responses for all questions
        for (BurnoutQuestion question : response.getQuestions()) {
            orchestrator.recordResponse(sessionId, question.getQuestionId(), "Test response", null, null);
        }

        // Act
        BurnoutAssessmentResult result = orchestrator.completeAssessment(sessionId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isNotNull();
        assertThat(result.getScore().getOverallScore()).isEqualTo(3.5); // From our mock
        assertThat(result.getSummary()).isEqualTo("Mock insight summary"); // From our mock
        assertThat(result.getCompletedAt()).isNotNull();
    }
}