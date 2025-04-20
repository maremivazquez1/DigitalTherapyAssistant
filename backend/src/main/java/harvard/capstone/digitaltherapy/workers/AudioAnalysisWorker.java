package harvard.capstone.digitaltherapy.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;


import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class AudioAnalysisWorker {

    private static final String HUME_API_KEY = "4uQuBCZQWwZzhUNUvBSDruAoSPdU8WfJMu9dejNszNnREaC2";
    private static final String HUME_JOB_ENDPOINT = "https://api.hume.ai/v0/batch/jobs";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, CompletableFuture<String>> jobFutures = new ConcurrentHashMap<>();

    public AudioAnalysisWorker() {
        if (HUME_API_KEY == null || HUME_API_KEY.isBlank()) {
            throw new IllegalStateException("HUME_API_KEY environment variable is not set.");
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            System.setProperty("https.proxyHost", "proxy.example.com");
            System.setProperty("https.proxyPort", "8080");
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .sslContext(sslContext)
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HTTP client", e);
        }
    }

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

    private void pollJob(String jobId, CompletableFuture<String> future, int attempt) {
        scheduler.schedule(() -> {
            try {
                System.out.printf("📡 Polling job %s (attempt %d)...%n", jobId, attempt + 1);

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
    

    public static List<String> extractTopEmotions(JsonNode emotionArray, double threshold, int maxCount) {
        List<Map.Entry<String, Double>> filtered = new ArrayList<>();
    
        for (JsonNode emotion : emotionArray) {
            String name = emotion.path("name").asText();
            double score = emotion.path("score").asDouble();
    
            if (score >= threshold) {
                filtered.add(Map.entry(name, score));
            }
        }
    
        // Sort descending by score
        filtered.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
    
        // Limit to top N
        return filtered.stream()
                .limit(maxCount)
                .map(entry -> String.format("%s (%.0f%%)", entry.getKey(), entry.getValue() * 100))
                .toList();
    }
    
}
