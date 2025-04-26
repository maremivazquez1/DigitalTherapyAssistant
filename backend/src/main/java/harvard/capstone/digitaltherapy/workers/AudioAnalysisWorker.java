package harvard.capstone.digitaltherapy.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;

/**
 * Worker responsible for submitting audio files to Hume AI's prosody analysis model,
 * polling the analysis job, and parsing the resulting emotions.
 *
 * This class supports asynchronous job submission and result retrieval with filtered output
 * to include only the top 3 emotions per utterance.
 *
 * Used in the Digital Therapy Assistant for intonation/emotion detection from audio.
 */
public class AudioAnalysisWorker {

    private static final String HUME_API_KEY = "4uQuBCZQWwZzhUNUvBSDruAoSPdU8WfJMu9dejNszNnREaC2";
    private static final String HUME_JOB_ENDPOINT = "https://api.hume.ai/v0/batch/jobs";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, CompletableFuture<String>> jobFutures = new ConcurrentHashMap<>();

    /**
     * Constructs a AudioAnalysisWorker instance with env level HUME credentials
     */
    public AudioAnalysisWorker() {
        if (HUME_API_KEY == null || HUME_API_KEY.isBlank()) {
            throw new IllegalStateException("HUME_API_KEY environment variable is not set.");
        }
        this.httpClient = createTrustedHttpClient();
    }

    private HttpClient createTrustedHttpClient() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
    }

    /**
     * Submits an audio analysis job to Hume and begins polling for completion.
     *
     * @param audioUrl a pre-signed S3 URL pointing to the audio file
     * @return CompletableFuture that completes with a filtered JSON result or an error
     */
    public CompletableFuture<String> analyzeAudioAsync(String audioUrl) {
        CompletableFuture<String> future = new CompletableFuture<>();
        submitJob(audioUrl).thenAccept(jobId -> {
            jobFutures.put(jobId, future);
            scheduler.schedule(() -> pollJob(jobId, future, 0), 2, TimeUnit.SECONDS);
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });
        return future;
    }

    /**
     * Constructs and sends the job submission request to Hume AI.
     *
     * @param audioUrl a public or pre-signed URL to an audio file
     * @return CompletableFuture resolving to the Hume job ID
     */
    private CompletableFuture<String> submitJob(String audioUrl) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "urls", List.of(audioUrl),
                    "models", Map.of("prosody", Map.of())
            );

            String json = objectMapper.writeValueAsString(requestBody);
            System.out.println("Sending JSON: " + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HUME_JOB_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("X-Hume-Api-Key", HUME_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("Hume Response Code: " + response.statusCode());
                        System.out.println("Hume Response Body: " + response.body());

                        if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 202) {
                            throw new RuntimeException("Failed to submit Hume job: " + response.body());
                        }

                        JsonNode root;
                        try {
                            root = objectMapper.readTree(response.body());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to parse JSON from Hume response", e);
                        }

                        String jobId = root.path("job_id").asText();
                        if (jobId == null || jobId.isEmpty()) {
                            jobId = root.path("id").asText();
                        }
                        if (jobId == null || jobId.isEmpty()) {
                            throw new RuntimeException("Could not extract job ID from Hume response: " + response.body());
                        }
                        return jobId;
                    });

        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * Polls Hume AI for the status of a submitted job. If completed, it fetches and filters results.
     *
     * @param jobId the Hume job ID to track
     * @param future the future to complete with the result or error
     * @param attempt how many times we've polled already (used to time out)
     */
    private void pollJob(String jobId, CompletableFuture<String> future, int attempt) {
        scheduler.schedule(() -> {
            try {
                System.out.printf("\uD83D\uDCF1 Polling job %s (attempt %d)...%n", jobId, attempt + 1);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HUME_JOB_ENDPOINT + "/" + jobId))
                        .header("X-Hume-Api-Key", HUME_API_KEY)
                        .GET()
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            try {
                                JsonNode root = objectMapper.readTree(response.body());
                                String status = Optional.ofNullable(root.path("state").path("status").asText()).orElse("").toLowerCase();

                                System.out.printf("→ Job %s status: %s%n", jobId, status);

                                if (status.equals("done") || status.equals("complete") || status.equals("completed")) {
                                    fetchPredictions(jobId, future);
                                } else if (status.equals("failed")) {
                                    future.completeExceptionally(new RuntimeException("Hume job failed: " + jobId));
                                    jobFutures.remove(jobId);
                                } else {
                                    if (attempt < 60) {
                                        pollJob(jobId, future, attempt + 1);
                                    } else {
                                        future.completeExceptionally(new TimeoutException("Hume job timeout: " + jobId));
                                        jobFutures.remove(jobId);
                                    }
                                }
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                                jobFutures.remove(jobId);
                            }
                        });
            } catch (Exception e) {
                future.completeExceptionally(e);
                jobFutures.remove(jobId);
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Fetches prediction results from Hume and injects top-3 emotions into each utterance.
     *
     * @param jobId the ID of the Hume job
     * @param future the async response to complete with the final JSON
     */
    private void fetchPredictions(String jobId, CompletableFuture<String> future) {
        String predictionUrl = HUME_JOB_ENDPOINT + "/" + jobId + "/predictions";

        HttpRequest predictionRequest = HttpRequest.newBuilder()
                .uri(URI.create(predictionUrl))
                .header("X-Hume-Api-Key", HUME_API_KEY)
                .GET()
                .build();

        httpClient.sendAsync(predictionRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(predictionResponse -> {
                    try {
                        JsonNode root = objectMapper.readTree(predictionResponse.body());

                        if (!root.isArray() || root.size() == 0) {
                            throw new RuntimeException("Unexpected prediction format: root is not an array.");
                        }

                        JsonNode groupedPredictions = root.get(0)
                                .path("results")
                                .path("predictions")
                                .get(0)
                                .path("models")
                                .path("prosody")
                                .path("grouped_predictions");

                        if (groupedPredictions == null || !groupedPredictions.isArray()) {
                            throw new RuntimeException("Missing or invalid grouped_predictions.");
                        }

                        // For each utterance, replace full emotion array with the top 3 emotions only
                        for (JsonNode group : groupedPredictions) {
                            JsonNode predictions = group.path("predictions");
                            if (predictions != null && predictions.isArray()) {
                                for (JsonNode utterance : predictions) {
                                    JsonNode emotions = utterance.path("emotions");
                                    if (emotions.isArray() && emotions.size() > 0) {
                                        List<Map<String, Object>> top = extractTopRawEmotions(emotions, 3);
                                        ((ObjectNode) utterance).putPOJO("emotions", top);
                                    }
                                }
                            }
                        }

                        // Extract full transcript text for compatibility with AWS Transcribe-like formats
                        StringBuilder transcriptBuilder = new StringBuilder();
                        for (JsonNode group : groupedPredictions) {
                            JsonNode predictions = group.path("predictions");
                            if (predictions != null && predictions.isArray()) {
                                for (JsonNode utterance : predictions) {
                                    JsonNode emotions = utterance.path("emotions");
                                    if (emotions.isArray() && emotions.size() > 0) {
                                        List<Map<String, Object>> top = extractTopRawEmotions(emotions, 3);
                                        ((ObjectNode) utterance).putPOJO("emotions", top);
                                    }
                                    String text = utterance.path("text").asText();
                                    if (text != null && !text.isBlank()) {
                                        transcriptBuilder.append(text).append(" ");
                                    }
                                }
                            }
                        }

                        // Inject top-level transcript field
                        ((ObjectNode) root.get(0).path("results").path("predictions").get(0))
                                .put("transcript", transcriptBuilder.toString().trim());

                        String filteredJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                        System.out.println(filteredJson);
                        future.complete(filteredJson);
                        jobFutures.remove(jobId);

                    } catch (Exception e) {
                        future.completeExceptionally(new RuntimeException("Failed to parse prediction response.", e));
                        jobFutures.remove(jobId);
                    }
                });
    }

    /**
     * Filters the emotion array to retain only the top-N scoring entries.
     *
     * @param emotionArray the array of emotion objects from Hume
     * @param maxCount the maximum number of top emotions to retain
     * @return list of emotion maps with name and score
     */
    public static List<Map<String, Object>> extractTopRawEmotions(JsonNode emotionArray, int maxCount) {
        List<Map.Entry<String, Double>> all = new ArrayList<>();

        for (JsonNode emotion : emotionArray) {
            String name = emotion.path("name").asText();
            double score = emotion.path("score").asDouble();
            all.add(Map.entry(name, score));
        }

        all.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> topEmotions = new ArrayList<>();
        for (int i = 0; i < Math.min(maxCount, all.size()); i++) {
            Map<String, Object> emotionObj = new LinkedHashMap<>();
            emotionObj.put("name", all.get(i).getKey());
            emotionObj.put("score", all.get(i).getValue());
            topEmotions.add(emotionObj);
        }

        return topEmotions;
    }

}
