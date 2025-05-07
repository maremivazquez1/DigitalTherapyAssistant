package harvard.capstone.digitaltherapy.burnout.orchestration;

import harvard.capstone.digitaltherapy.burnout.model.*;
import harvard.capstone.digitaltherapy.workers.AudioAnalysisWorker;
import harvard.capstone.digitaltherapy.workers.VideoAnalysisWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BurnoutAssessmentOrchestratorRecordResponseTest {

    @Mock
    private AudioAnalysisWorker audioAnalysisWorker;

    @Mock
    private VideoAnalysisWorker videoAnalysisWorker;

    private BurnoutAssessmentOrchestrator orchestrator;
    private String sessionId;
    private String userId;
    private String questionId;

    @BeforeEach
    void setUp() {
        // Create a spy of the orchestrator
        orchestrator = spy(new BurnoutAssessmentOrchestrator());

        // Set up a session for testing
        userId = "testUser123";
        BurnoutSessionCreationResponse response = orchestrator.createAssessmentSession(userId);
        sessionId = response.getSessionId();
        questionId = response.getQuestions().get(0).getQuestionId();

        // Use reflection to replace the workers with mocks
        try {
            java.lang.reflect.Field audioWorkerField = BurnoutAssessmentOrchestrator.class.getDeclaredField("audioAnalysisWorker");
            audioWorkerField.setAccessible(true);
            audioWorkerField.set(orchestrator, audioAnalysisWorker);

            java.lang.reflect.Field videoWorkerField = BurnoutAssessmentOrchestrator.class.getDeclaredField("videoAnalysisWorker");
            videoWorkerField.setAccessible(true);
            videoWorkerField.set(orchestrator, videoAnalysisWorker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up mocks", e);
        }
    }

    @Test
    void recordResponse_shouldReturnTrue_whenRecordingValidResponse() {
        // Arrange
        String responseText = "I feel tired most days";

        // Act
        boolean result = orchestrator.recordResponse(sessionId, questionId, responseText, null, null);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void recordResponse_shouldStoreResponseInSession_whenRecordingValidResponse() {
        // Arrange
        String responseText = "I feel tired most days";

        // Act
        orchestrator.recordResponse(sessionId, questionId, responseText, null, null);

        // Assert - Get the session through reflection to verify the response was stored
        BurnoutAssessmentSession session = getSession(sessionId);
        assertThat(session).isNotNull();

        Map<String, BurnoutUserResponse> responses = session.getResponses();
        assertThat(responses).containsKey(questionId);

        BurnoutUserResponse storedResponse = responses.get(questionId);
        assertThat(storedResponse.getTextResponse()).isEqualTo(responseText);
        assertThat(storedResponse.getQuestionId()).isEqualTo(questionId);
    }

    @Test
    void recordResponse_shouldReturnFalse_whenSessionIdDoesNotExist() {
        // Arrange
        String nonExistentSessionId = "nonExistentSession";
        String responseText = "This should not be recorded";

        // Act
        boolean result = orchestrator.recordResponse(nonExistentSessionId, questionId, responseText, null, null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void recordResponse_shouldReturnFalse_whenQuestionIdDoesNotExist() {
        // Arrange
        String nonExistentQuestionId = "nonExistentQuestion";
        String responseText = "This should not be recorded";

        // Act
        boolean result = orchestrator.recordResponse(sessionId, nonExistentQuestionId, responseText, null, null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void recordResponse_shouldStartAudioAnalysis_whenAudioUrlProvided() {
        // Arrange
        String responseText = "I feel tired most days";
        String audioUrl = "s3://burnout-assessment/audio123.wav";

        // Configure the mock for this specific test
        when(audioAnalysisWorker.analyzeAudioAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{}"));

        // Act
        orchestrator.recordResponse(sessionId, questionId, responseText, null, audioUrl);

        // Assert
        verify(audioAnalysisWorker).analyzeAudioAsync(audioUrl);
    }

    @Test
    void recordResponse_shouldStartVideoAnalysis_whenVideoUrlProvided() {
        // Arrange
        String responseText = "I feel tired most days";
        String videoUrl = "s3://burnout-assessment/video123.mp4";

        // Configure the mock for this specific test
        when(videoAnalysisWorker.detectFacesFromVideoAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{}"));

        // Act
        orchestrator.recordResponse(sessionId, questionId, responseText, videoUrl, null);

        // Assert
        verify(videoAnalysisWorker).detectFacesFromVideoAsync(videoUrl);
    }

    @Test
    void recordResponse_shouldStartBothAnalyses_whenBothUrlsProvided() {
        // Arrange
        String responseText = "I feel tired most days";
        String videoUrl = "s3://burnout-assessment/video123.mp4";
        String audioUrl = "s3://burnout-assessment/audio123.wav";

        // Configure the mocks for this specific test
        when(videoAnalysisWorker.detectFacesFromVideoAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{}"));
        when(audioAnalysisWorker.analyzeAudioAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{}"));

        // Act
        orchestrator.recordResponse(sessionId, questionId, responseText, videoUrl, audioUrl);

        // Assert
        verify(videoAnalysisWorker).detectFacesFromVideoAsync(videoUrl);
        verify(audioAnalysisWorker).analyzeAudioAsync(audioUrl);
    }

    @Test
    void recordResponse_shouldStoreResponseImmediately_evenBeforeAnalysisComplete() {
        // Arrange
        String responseText = "I feel tired most days";
        String videoUrl = "s3://burnout-assessment/video123.mp4";

        // Setup a CompletableFuture that won't complete until we manually complete it
        CompletableFuture<String> pendingFuture = new CompletableFuture<>();
        when(videoAnalysisWorker.detectFacesFromVideoAsync(anyString()))
                .thenReturn(pendingFuture);

        // Act
        orchestrator.recordResponse(sessionId, questionId, responseText, videoUrl, null);

        // Assert - Response should be stored even though analysis is still pending
        BurnoutAssessmentSession session = getSession(sessionId);
        assertThat(session.getResponses()).containsKey(questionId);

        BurnoutUserResponse storedResponse = session.getResponses().get(questionId);
        assertThat(storedResponse.getTextResponse()).isEqualTo(responseText);
        assertThat(storedResponse.getMultimodalInsights()).isEmpty();

        // Complete the future to avoid hanging test
        pendingFuture.complete("{}");
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

    @Test
    void updateResponseWithVideoAnalysis_shouldUpdateExistingResponse() throws Exception {
        // Arrange
        String responseText = "I feel tired most days";
        orchestrator.recordResponse(sessionId, questionId, responseText, null, null);

        String analysisJson = "{\"jobStatus\":\"SUCCEEDED\",\"faces\":[{\"emotions\":[{\"type\":\"CALM\",\"confidence\":0.95}]}]}";

        // Get the updateResponseWithVideoAnalysis method via reflection
        java.lang.reflect.Method updateMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "updateResponseWithVideoAnalysis", String.class, String.class, String.class);
        updateMethod.setAccessible(true);

        // Act
        updateMethod.invoke(orchestrator, sessionId, questionId, analysisJson);

        // Assert
        BurnoutAssessmentSession session = getSession(sessionId);
        BurnoutUserResponse updatedResponse = session.getResponses().get(questionId);

        assertThat(updatedResponse.getMultimodalInsights()).containsKey("video");
        assertThat(updatedResponse.getMultimodalInsights().get("video")).isEqualTo(analysisJson);
    }

    @Test
    void updateResponseWithAudioAnalysis_shouldUpdateExistingResponse() throws Exception {
        // Arrange
        String responseText = "I feel tired most days";
        orchestrator.recordResponse(sessionId, questionId, responseText, null, null);

        String analysisJson = "{\"transcript\":\"I feel tired most days\",\"emotions\":[{\"name\":\"sadness\",\"score\":0.8}]}";

        // Get the updateResponseWithAudioAnalysis method via reflection
        java.lang.reflect.Method updateMethod = BurnoutAssessmentOrchestrator.class.getDeclaredMethod(
                "updateResponseWithAudioAnalysis", String.class, String.class, String.class);
        updateMethod.setAccessible(true);

        // Act
        updateMethod.invoke(orchestrator, sessionId, questionId, analysisJson);

        // Assert
        BurnoutAssessmentSession session = getSession(sessionId);
        BurnoutUserResponse updatedResponse = session.getResponses().get(questionId);

        assertThat(updatedResponse.getMultimodalInsights()).containsKey("audio");
        assertThat(updatedResponse.getMultimodalInsights().get("audio")).isEqualTo(analysisJson);
    }

    @Test
    void testAsyncVideoAnalysisUpdateResponse() {
        // Arrange
        String responseText = "I feel tired most days";
        String videoUrl = "s3://burnout-assessment/video123.mp4";
        String analysisJson = "{\"jobStatus\":\"SUCCEEDED\",\"faces\":[{\"emotions\":[{\"type\":\"CALM\",\"confidence\":0.95}]}]}";

        // Create a future we can complete manually
        CompletableFuture<String> analysisFuture = new CompletableFuture<>();
        when(videoAnalysisWorker.detectFacesFromVideoAsync(videoUrl))
                .thenReturn(analysisFuture);

        // Act - Record response with the video URL
        orchestrator.recordResponse(sessionId, questionId, responseText, videoUrl, null);

        // Assert - Initially the response should have no insights
        BurnoutAssessmentSession session = getSession(sessionId);
        BurnoutUserResponse initialResponse = session.getResponses().get(questionId);
        assertThat(initialResponse.getMultimodalInsights()).isEmpty();

        // Now complete the analysis
        analysisFuture.complete(analysisJson);

        // Wait briefly for async processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Re-fetch the session to get latest state
        session = getSession(sessionId);
        BurnoutUserResponse updatedResponse = session.getResponses().get(questionId);

        assertThat(updatedResponse.getMultimodalInsights()).containsKey("video");
        assertThat(updatedResponse.getMultimodalInsights().get("video")).isEqualTo(analysisJson);
    }

    @Test
    void testAsyncAudioAnalysisUpdateResponse() {
        // Arrange
        String responseText = "I feel tired most days";
        String audioUrl = "s3://burnout-assessment/audio123.wav";
        String analysisJson = "{\"transcript\":\"I feel tired most days\",\"emotions\":[{\"name\":\"sadness\",\"score\":0.8}]}";

        // Create a future we can complete manually
        CompletableFuture<String> analysisFuture = new CompletableFuture<>();
        when(audioAnalysisWorker.analyzeAudioAsync(audioUrl))
                .thenReturn(analysisFuture);

        // Act - Record response with the audio URL
        orchestrator.recordResponse(sessionId, questionId, responseText, null, audioUrl);

        // Assert - Initially the response should have no insights
        BurnoutAssessmentSession session = getSession(sessionId);
        BurnoutUserResponse initialResponse = session.getResponses().get(questionId);
        assertThat(initialResponse.getMultimodalInsights()).isEmpty();

        // Now complete the analysis
        analysisFuture.complete(analysisJson);

        // Wait briefly for async processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Re-fetch the session to get latest state
        session = getSession(sessionId);
        BurnoutUserResponse updatedResponse = session.getResponses().get(questionId);

        assertThat(updatedResponse.getMultimodalInsights()).containsKey("audio");
        assertThat(updatedResponse.getMultimodalInsights().get("audio")).isEqualTo(analysisJson);
    }
}