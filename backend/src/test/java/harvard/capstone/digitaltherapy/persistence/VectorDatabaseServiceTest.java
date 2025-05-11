package harvard.capstone.digitaltherapy.persistence;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class VectorDatabaseServiceTest {

    /**
     * Test that storeEmbedding generates a UUID when documentId is null
     */
    @Test
    public void testStoreEmbeddingGeneratesUuid() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String content = "Test content";
        Map<String, Object> metadata = new HashMap<>();

        // Act
        String documentId = service.storeEmbedding(null, content, metadata);

        // Assert
        assertNotNull(documentId, "Document ID should be generated when null is provided");
        assertFalse(documentId.isEmpty(), "Generated Document ID should not be empty");

        // Verify the content was stored
        List<TextSegment> segments = service.getAllStoredSegments();
        assertEquals(1, segments.size(), "There should be one stored segment");
        assertEquals(content, segments.get(0).text(), "Content should match in stored segment");
    }

    /**
     * Test that storeEmbedding creates an empty HashMap when metadata is null
     */
    @Test
    public void testStoreEmbeddingWithNullMetadata() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String content = "Test content";
        String docId = "test-doc-id";

        // Act
        String resultId = service.storeEmbedding(docId, content, null);

        // Assert
        assertEquals(docId, resultId, "Document ID should match the provided ID");

        // Verify metadata was created and contains default fields
        List<TextSegment> segments = service.getAllStoredSegments();
        assertEquals(1, segments.size(), "There should be one stored segment");

        Map<String, Object> storedMetadata = segments.get(0).metadata().toMap();
        assertNotNull(storedMetadata, "Metadata should be created when null is provided");
        assertEquals(docId, storedMetadata.get("documentId"), "Document ID should be in metadata");
        assertNotNull(storedMetadata.get("timestamp"), "Timestamp should be added to metadata");
    }

    /**
     * Test that indexSessionMessage stores a message with correct metadata
     */
    @Test
    public void testIndexSessionMessage() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String sessionId = "session-123";
        String userId = "user-456";
        String message = "This is a test message";
        boolean isUserMessage = true;

        // Act
        String docId = service.indexSessionMessage(sessionId, userId, message, isUserMessage);

        // Assert
        assertNotNull(docId, "Document ID should be generated");

        // Verify metadata is correct
        List<TextSegment> segments = service.getAllStoredSegments();
        assertEquals(1, segments.size(), "There should be one stored segment");

        Map<String, Object> metadata = segments.get(0).metadata().toMap();
        assertEquals(sessionId, metadata.get("sessionId"), "Session ID should match");
        assertEquals(userId, metadata.get("userId"), "User ID should match");
        assertEquals("user", metadata.get("messageType"), "Message type should be user");
        assertEquals("message", metadata.get("contentType"), "Content type should be message");
    }

    /**
     * Test that indexSessionAnalysis stores analysis with correct metadata and format
     */
    @Test
    public void testIndexSessionAnalysis() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String sessionId = "session-123";
        String userId = "user-456";
        String sourceMessageId = "msg-789";

        Map<String, Object> analysisResults = new HashMap<>();
        analysisResults.put("emotion", "sad");
        analysisResults.put("cognitiveDistortions", Arrays.asList("catastrophizing", "black-and-white thinking"));
        analysisResults.put("keyThemes", Arrays.asList("work stress", "relationships"));
        analysisResults.put("otherMetric", 0.85);

        // Act
        String docId = service.indexSessionAnalysis(sessionId, userId, analysisResults, sourceMessageId);

        // Assert
        assertNotNull(docId, "Document ID should be generated");

        // Verify the content and metadata
        List<TextSegment> segments = service.getAllStoredSegments();
        assertEquals(1, segments.size(), "There should be one stored segment");

        TextSegment segment = segments.get(0);
        String content = segment.text();
        assertTrue(content.contains("emotion: sad"), "Content should contain emotion");
        assertTrue(content.contains("cognitiveDistortions: catastrophizing, black-and-white thinking"),
                "Content should contain cognitive distortions");
        assertTrue(content.contains("keyThemes: work stress, relationships"),
                "Content should contain key themes");
        assertTrue(content.contains("otherMetric: 0.85"), "Content should contain other metrics");

        Map<String, Object> metadata = segment.metadata().toMap();
        assertEquals(sessionId, metadata.get("sessionId"), "Session ID should match");
        assertEquals(userId, metadata.get("userId"), "User ID should match");
        assertEquals("analysis", metadata.get("contentType"), "Content type should be analysis");
        assertEquals(sourceMessageId, metadata.get("sourceMessageId"), "Source message ID should match");
        assertEquals("sad", metadata.get("emotion"), "Emotion should be in metadata");
        assertEquals("catastrophizing, black-and-white thinking", metadata.get("cognitiveDistortions"),
                "Cognitive distortions should be in metadata");
        assertEquals("work stress, relationships", metadata.get("keyThemes"),
                "Key themes should be in metadata");
    }

    /**
     * Test findSimilarSessions returns a map of session texts
     */
    @Test
    public void testFindSimilarSessions() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String userId = "user-123";
        String currentSessionId = "session-current";
        String otherSessionId = "session-other";

        // Store messages from current session
        service.indexSessionMessage(currentSessionId, userId, "Current session message", true);

        // Store messages from other session
        service.indexSessionMessage(otherSessionId, userId, "Other session message about anxiety", true);

        // Act
        Map<String, String> similarSessions = service.findSimilarSessions(
                userId,
                "Let's talk about my anxiety",
                5);

        // Assert
        assertNotNull(similarSessions, "Result should not be null");
        assertTrue(similarSessions.containsKey(otherSessionId),
                "Other session should be in results");
        assertFalse(similarSessions.containsKey(currentSessionId),
                "Current session should not be in results");
        assertEquals("Other session message about anxiety", similarSessions.get(otherSessionId),
                "Session text should match");
    }

    /**
     * Test findRelevantInterventions returns interventions for cognitive distortions
     */
    @Test
    public void testFindRelevantInterventions() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();

        // Store some interventions
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "intervention");
        service.storeEmbedding("int1", "Intervention for catastrophizing: Challenge your thoughts by examining evidence", metadata);

        // Act
        List<String> interventions = service.findRelevantInterventions(
                Arrays.asList("catastrophizing", "black-and-white thinking"),
                2);

        // Assert
        assertNotNull(interventions, "Result should not be null");
        assertFalse(interventions.isEmpty(), "Result should not be empty");
        assertTrue(interventions.get(0).contains("catastrophizing"),
                "Intervention should match the requested distortions");
    }

    /**
     * Test that findRelevantInterventions returns empty list for null or empty distortions
     */
    @Test
    public void testFindRelevantInterventionsWithNullOrEmpty() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();

        // Act & Assert
        List<String> result1 = service.findRelevantInterventions(null, 2);
        assertTrue(result1.isEmpty(), "Result should be empty for null distortions");

        List<String> result2 = service.findRelevantInterventions(new ArrayList<>(), 2);
        assertTrue(result2.isEmpty(), "Result should be empty for empty distortions list");
    }


    /**
     * Test buildContextForPrompt constructs context from relevant content
     */
    @Test
    public void testBuildContextForPrompt() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String currentSessionId = "session-current";
        String previousSessionId = "session-previous";
        String userId = "user-123";

        // Store a message from previous session
        service.indexSessionMessage(previousSessionId, userId,
                "I've been feeling anxious about work", true);

        // Act
        String context = service.buildContextForPrompt(
                currentSessionId,
                userId,
                "My anxiety at work is getting worse",
                5);

        // Assert
        assertNotNull(context, "Context should not be null");
        assertTrue(context.contains("Relevant historical context"),
                "Context should have the expected header");
        assertTrue(context.contains(previousSessionId),
                "Context should reference the previous session");
        assertTrue(context.contains("feeling anxious about work"),
                "Context should include the previous message");
        assertFalse(context.contains(currentSessionId),
                "Context should not include current session");
    }

    /**
     * Test deleteEmbedding returns false for in-memory store
     */
    @Test
    public void testDeleteEmbedding() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String docId = "test-doc";
        service.storeEmbedding(docId, "Test content", null);

        // Act
        boolean result = service.deleteEmbedding(docId);

        // Assert
        assertFalse(result, "Delete should return false for in-memory store");
    }


}