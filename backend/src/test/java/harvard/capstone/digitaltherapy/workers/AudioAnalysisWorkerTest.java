package harvard.capstone.digitaltherapy.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AudioAnalysisWorkerTest {

    private AudioAnalysisWorker worker;
    private HttpClient mockClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();

        // Inject the mock client using reflection (since field is final)
        worker = new AudioAnalysisWorker() {
            {
                try {
                    var field = AudioAnalysisWorker.class.getDeclaredField("httpClient");
                    field.setAccessible(true);
                    field.set(this, mockClient);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    void analyzeAudioAsync_successfulJob_submissionFlow() throws Exception {
        String mockAudioUrl = "https://s3.amazonaws.com/myfile.mp3";
        String fakeJobId = "test_job_123";

        // Mock job submission response
        HttpResponse<String> mockSubmitResponse = mock(HttpResponse.class);
        when(mockSubmitResponse.statusCode()).thenReturn(202);
        when(mockSubmitResponse.body()).thenReturn("{\"job_id\": \"" + fakeJobId + "\"}");

        when(mockClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(mockSubmitResponse)); // For submitJob

        CompletableFuture<String> resultFuture = worker.analyzeAudioAsync(mockAudioUrl);

        // Since we haven't mocked pollJob or fetchPredictions, this future won't complete.
        // But we can still test that submission path worked without throwing.
        assertNotNull(resultFuture);
        assertFalse(resultFuture.isDone()); // Because polling hasn't completed
    }

    @Test
    void analyzeAudioAsync_invalidSubmission_completesExceptionally() throws Exception {
        String mockAudioUrl = "bad-url";

        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(400);
        when(errorResponse.body()).thenReturn("Bad request");

        when(mockClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(errorResponse));

        CompletableFuture<String> future = worker.analyzeAudioAsync(mockAudioUrl);

        Thread.sleep(200); // Allow async chain to run
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void extractTopRawEmotions_returnsTopSortedEmotions() throws Exception {
        String json = """
            [
              { "name": "Excited", "score": 0.7 },
              { "name": "Calm", "score": 0.5 },
              { "name": "Angry", "score": 0.9 },
              { "name": "Sad", "score": 0.2 }
            ]
        """;
        var node = objectMapper.readTree(json);
        List<Map<String, Object>> result = AudioAnalysisWorker.extractTopRawEmotions(node, 3);

        assertEquals(3, result.size());
        assertEquals("Angry", result.get(0).get("name"));
        assertEquals("Excited", result.get(1).get("name"));
        assertEquals("Calm", result.get(2).get("name"));
    }

    @Test
    void fetchPredictions_filtersEmotions_andBuildsTranscript() throws Exception {
        String fakeJobId = "test123";

        // Simulate valid Hume prediction JSON with 2 utterances
        String mockJson = """
        [
        {
            "results": {
            "predictions": [
                {
                "models": {
                    "prosody": {
                    "grouped_predictions": [
                        {
                        "predictions": [
                            {
                            "text": "I'm okay.",
                            "emotions": [
                                {"name": "Calm", "score": 0.4},
                                {"name": "Happy", "score": 0.7},
                                {"name": "Sad", "score": 0.3},
                                {"name": "Angry", "score": 0.1}
                            ]
                            },
                            {
                            "text": "Just tired.",
                            "emotions": [
                                {"name": "Tired", "score": 0.8},
                                {"name": "Calm", "score": 0.3}
                            ]
                            }
                        ]
                        }
                    ]
                    }
                }
                }
            ]
            }
        }
        ]
        """;

        // Mock HTTP response
        HttpResponse<String> predictionResponse = mock(HttpResponse.class);
        when(predictionResponse.body()).thenReturn(mockJson);

        when(mockClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(CompletableFuture.completedFuture(predictionResponse));

        // Build a worker instance with injected client (already handled in @BeforeEach)
        CompletableFuture<String> future = new CompletableFuture<>();

        // Use reflection to call fetchPredictions
        var method = AudioAnalysisWorker.class.getDeclaredMethod("fetchPredictions", String.class, CompletableFuture.class);
        method.setAccessible(true);
        method.invoke(worker, fakeJobId, future);

        // Wait for future to complete
        String result = future.get(2, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println("FETCH RESULT:\n" + result);

        assertNotNull(result);
        assertTrue(result.contains("Happy"));
        assertTrue(result.contains("Tired"));
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(result);
        String transcript = rootNode.get(0)
            .path("results")
            .path("predictions")
            .get(0)
            .path("transcript")
            .asText();
        assertEquals("I'm okay. Just tired.", transcript);

    }

}
