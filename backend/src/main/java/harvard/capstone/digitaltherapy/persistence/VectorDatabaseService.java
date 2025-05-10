package harvard.capstone.digitaltherapy.persistence;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.data.document.Metadata;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * VectorDatabaseService
 *
 * Manages vector embeddings for therapeutic content, enabling similarity searching
 * and retrieval of relevant historical information. This service supports the
 * RAG (Retrieval Augmented Generation) pattern for enhanced therapy sessions.
 */
public class VectorDatabaseService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;

    private final List<TextSegment> storedSegments = new ArrayList<>();

    // Configuration parameters
    private final int maxTokensPerChunk;
    private final double relevanceThreshold;
    final int defaultMaxResults;

    /**
     * Constructor with default configuration
     */
    public VectorDatabaseService() {
        this(512, 0.7, 5);
    }

    /**
     * Constructor with custom configuration
     *
     * @param maxTokensPerChunk Maximum tokens per text chunk
     * @param relevanceThreshold Minimum similarity score threshold
     * @param defaultMaxResults Default maximum number of results to return
     */
    public VectorDatabaseService(int maxTokensPerChunk, double relevanceThreshold, int defaultMaxResults) {
        this.maxTokensPerChunk = maxTokensPerChunk;
        this.relevanceThreshold = relevanceThreshold;
        this.defaultMaxResults = defaultMaxResults;

        // Initialize the embedding model
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // Initialize the embedding store (in-memory for now)
        this.embeddingStore = new InMemoryEmbeddingStore<>();

        // Initialize document splitter for chunking longer texts
        this.documentSplitter = DocumentSplitters.recursive(maxTokensPerChunk, 0);
    }

    /**
     * Stores a text with its embedding and metadata
     *
     * @param documentId Unique identifier for the document (if null, generates UUID)
     * @param content The text content to embed
     * @param metadata Associated metadata for retrieval and filtering
     * @return The document ID
     */
    public String storeEmbedding(String documentId, String content, Map<String, Object> metadata) {
        if (documentId == null) {
            documentId = UUID.randomUUID().toString();
        }

        if (metadata == null) {
            metadata = new HashMap<>();
        }

        // Add document ID to metadata
        metadata.put("documentId", documentId);
        metadata.put("timestamp", LocalDateTime.now().toString());

        // Create text segment with metadata
        TextSegment segment = TextSegment.from(content, Metadata.from(metadata));

        // Generate embedding
        Embedding embedding = embeddingModel.embed(content).content();

        // Store embedding
        embeddingStore.add(embedding, segment);
        storedSegments.add(segment);

        return documentId;
    }

    /**
     * Stores user or system message from a therapy session
     *
     * @param sessionId The therapy session ID
     * @param userId The user ID
     * @param messageContent The message content
     * @param isUserMessage Whether this is a user message (true) or system message (false)
     * @return The document ID
     */
    public String indexSessionMessage(String sessionId, String userId, String messageContent, boolean isUserMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("messageType", isUserMessage ? "user" : "system");
        metadata.put("contentType", "message");

        return storeEmbedding(null, messageContent, metadata);
    }

    /**
     * Stores analysis results from a therapy session
     *
     * @param sessionId The therapy session ID
     * @param userId The user ID
     * @param analysisResults The analysis results
     * @return The document ID
     */
    public String indexSessionAnalysis(String sessionId, String userId, Map<String, Object> analysisResults, String sourceMessageId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("contentType", "analysis");
        metadata.put("sourceMessageId", sourceMessageId);

        // Convert analysis results to a format suitable for storage
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("Analysis Results:\n");

        for (Map.Entry<String, Object> entry : analysisResults.entrySet()) {
            contentBuilder.append(entry.getKey()).append(": ");

            if (entry.getValue() instanceof List) {
                contentBuilder.append(String.join(", ", (List<String>) entry.getValue()));
            } else {
                contentBuilder.append(entry.getValue().toString());
            }

            contentBuilder.append("\n");

            // Also add key analysis points to metadata for filtering
            if (entry.getKey().equals("emotion") ||
                    entry.getKey().equals("cognitiveDistortions") ||
                    entry.getKey().equals("keyThemes")) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    metadata.put(entry.getKey(), String.join(", ", (List<String>) value));
                } else {
                    metadata.put(entry.getKey(), value.toString());
                }
            }
        }

        return storeEmbedding(null, contentBuilder.toString(), metadata);
    }

    /**
     * Finds similar content based on a query
     *
     * @param query The query text
     * @param maxResults Maximum number of results to return
     * @return List of matching text segments with relevance scores
     */
    public List<EmbeddingMatch<TextSegment>> findSimilarContent(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults > 0 ? maxResults : defaultMaxResults)
            .minScore(relevanceThreshold)
            .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches();
    }

    /**
     * Finds similar content with metadata filtering
     *
     * @param query The query text
     * @param metadataFilters Metadata key-value pairs to filter results
     * @param maxResults Maximum number of results to return
     * @return List of matching text segments with relevance scores
     */
    public List<EmbeddingMatch<TextSegment>> findSimilarContent(
            String query,
            Map<String, Object> metadataFilters,
            int maxResults) {

        List<EmbeddingMatch<TextSegment>> matches = findSimilarContent(query, maxResults);

        if (metadataFilters == null || metadataFilters.isEmpty()) {
            return matches;
        }

        // Apply metadata filters
        return matches.stream()
                .filter(match -> {
                    Map<String, Object> metadata = match.embedded().metadata().toMap();
                    for (Map.Entry<String, Object> filter : metadataFilters.entrySet()) {
                        if (!metadata.containsKey(filter.getKey()) ||
                                !metadata.get(filter.getKey()).equals(filter.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Finds similar sessions based on the current session context
     *
     * @param userId The user ID
     * @param currentSessionContent The current session content to match
     * @param maxResults Maximum number of results to return
     * @return List of similar session IDs with relevance scores
     */
    public Map<String, String> findSimilarSessions(
            String userId,
            String currentSessionContent,
            int maxResults) {

        Map<String, Object> metadataFilters = new HashMap<>();
        metadataFilters.put("userId", userId);

        List<EmbeddingMatch<TextSegment>> matches = findSimilarContent(
                currentSessionContent,
                metadataFilters,
                maxResults);

        // Group by session ID and store the text content
        Map<String, String> sessionTexts = new HashMap<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            String sessionId = match.embedded().metadata().getString("sessionId");
            String text = match.embedded().text();
            if (sessionId != null &&
                    (!sessionTexts.containsKey(sessionId) ||
                            // If you still want to keep the text of the highest scoring match
                            sessionTexts.get(sessionId).length() < text.length())) {
                sessionTexts.put(sessionId, text);
            }
        }
        return sessionTexts;
    }


    /**
     * Finds relevant therapeutic interventions for cognitive distortions
     *
     * @param cognitiveDistortions List of cognitive distortions
     * @param maxResults Maximum number of results to return
     * @return List of relevant interventions
     */
    public List<String> findRelevantInterventions(
            List<String> cognitiveDistortions,
            int maxResults) {

        if (cognitiveDistortions == null || cognitiveDistortions.isEmpty()) {
            return new ArrayList<>();
        }

        // Build a query from the cognitive distortions
        String query = "Therapeutic interventions for: " + String.join(", ", cognitiveDistortions);

        Map<String, Object> metadataFilters = new HashMap<>();
        metadataFilters.put("contentType", "intervention");

        List<EmbeddingMatch<TextSegment>> matches = findSimilarContent(
                query,
                metadataFilters,
                maxResults);

        // Extract intervention text
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }

    /**
     * Builds context for a prompt based on session history and relevant content
     *
     * @param sessionId The current session ID
     * @param userId The user ID
     * @param currentInput The current user input
     * @param maxResults Maximum number of relevant items to include
     * @return Relevant context text to include in prompt
     */
    public String buildContextForPrompt(
            String sessionId,
            String userId,
            String currentInput,
            int maxResults) {

        // Find similar content from this user's history
        Map<String, Object> metadataFilters = new HashMap<>();
        metadataFilters.put("userId", userId);

        List<EmbeddingMatch<TextSegment>> matches = findSimilarContent(
                currentInput,
                metadataFilters,
                maxResults);

        // Build context from matches
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Relevant historical context:\n\n");

        for (EmbeddingMatch<TextSegment> match : matches) {
            Map<String, Object> metadata = match.embedded().metadata().toMap();
            String matchSessionId = (String) metadata.get("sessionId");

            // Skip current session
            if (sessionId.equals(matchSessionId)) {
                continue;
            }

            String contentType = (String) metadata.get("contentType");
            String timestamp = (String) metadata.get("timestamp");

            contextBuilder.append("From session ").append(matchSessionId);

            if (timestamp != null) {
                contextBuilder.append(" (").append(timestamp).append(")");
            }

            contextBuilder.append(":\n");
            contextBuilder.append(match.embedded().text()).append("\n\n");
        }

        return contextBuilder.toString();
    }

    /**
     * Deletes embeddings for a document ID
     *
     * @param documentId The document ID to delete
     * @return True if deleted, false otherwise
     */
    public boolean deleteEmbedding(String documentId) {
        // Note: InMemoryEmbeddingStore doesn't support direct deletion by ID
        // This would be implemented with a persistent store like OpenSearch
        // For now, we'll return false to indicate no operation performed
        return false;
    }


    // Get stored items
    public List<TextSegment> getAllStoredSegments() {
        return new ArrayList<>(storedSegments);
    }
}
