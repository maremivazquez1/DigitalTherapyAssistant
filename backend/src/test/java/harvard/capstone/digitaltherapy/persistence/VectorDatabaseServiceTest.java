package harvard.capstone.digitaltherapy.persistence;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VectorDatabaseServiceTest {

    private VectorDatabaseService vectorDatabaseService;
    private final String TEST_USER_ID = "test-user-123";
    private final String TEST_SESSION_ID = "test-session-456";

    @BeforeEach
    public void setup() {
        // Initialize with test-friendly parameters
        vectorDatabaseService = new VectorDatabaseService(256, 0.5, 3);
    }

    @Test
    public void testStoreAndRetrieveEmbedding() {
        // Arrange
        String content = "This is a test document for embedding storage and retrieval.";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("testKey", "testValue");

        // Act
        String documentId = vectorDatabaseService.storeEmbedding(null, content, metadata);

        // Assert
        assertNotNull(documentId, "Document ID should not be null");

        // Retrieve the stored segment
        List<TextSegment> storedSegments = vectorDatabaseService.getAllStoredSegments();
        assertEquals(1, storedSegments.size(), "Should have stored one segment");

        TextSegment storedSegment = storedSegments.get(0);
        assertEquals(content, storedSegment.text(), "Stored text should match input");
        assertEquals("testValue", storedSegment.metadata().getString("testKey"), "Metadata should be preserved");
        assertNotNull(storedSegment.metadata().getString("timestamp"), "Timestamp should be added");
    }

    @Test
    public void testFindSimilarContent() {
        // Arrange - Store some test content
        vectorDatabaseService.storeEmbedding(null, "Anxiety can be managed with deep breathing exercises.", null);
        vectorDatabaseService.storeEmbedding(null, "Depression often involves feelings of sadness and hopelessness.", null);
        vectorDatabaseService.storeEmbedding(null, "CBT techniques can help address negative thought patterns.", null);
        vectorDatabaseService.storeEmbedding(null, "Regular exercise can improve mood and reduce stress.", null);

        // Act - Search for similar content
        List<EmbeddingMatch<TextSegment>> matches = vectorDatabaseService.findSimilarContent("How to manage anxiety?", 2);

        // Assert
        assertNotNull(matches, "Matches should not be null");
        assertTrue(matches.size() <= 2, "Should return at most 2 results");

        // The first match should be related to anxiety
        boolean foundAnxietyMatch = false;
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match.embedded().text().contains("Anxiety")) {
                foundAnxietyMatch = true;
                break;
            }
        }
        assertTrue(foundAnxietyMatch, "Should find content related to anxiety");
    }

    @Test
    public void testIndexSessionMessage() {
        // Arrange
        String messageContent = "I've been feeling anxious about my upcoming presentation.";
        boolean isUserMessage = true;

        // Act
        String documentId = vectorDatabaseService.indexSessionMessage(
                TEST_SESSION_ID, TEST_USER_ID, messageContent, isUserMessage);

        // Assert
        assertNotNull(documentId, "Document ID should not be null");

        // Verify the message is stored with correct metadata
        List<TextSegment> storedSegments = vectorDatabaseService.getAllStoredSegments();
        assertEquals(1, storedSegments.size(), "Should have stored one segment");

        TextSegment segment = storedSegments.get(0);
        assertEquals(messageContent, segment.text(), "Stored text should match input");
        assertEquals(TEST_SESSION_ID, segment.metadata().getString("sessionId"), "Session ID should be stored");
        assertEquals(TEST_USER_ID, segment.metadata().getString("userId"), "User ID should be stored");
        assertEquals("user", segment.metadata().getString("messageType"), "Message type should be 'user'");
        assertEquals("message", segment.metadata().getString("contentType"), "Content type should be 'message'");
    }

    @Test
    public void testIndexSessionAnalysis() {
        // Arrange
        Map<String, Object> analysisResults = new HashMap<>();
        analysisResults.put("emotion", "anxiety");
        analysisResults.put("cognitiveDistortions", List.of("catastrophizing", "fortune-telling"));
        analysisResults.put("keyThemes", List.of("work stress", "public speaking"));

        // Act
        String documentId = vectorDatabaseService.indexSessionAnalysis(
                TEST_SESSION_ID, TEST_USER_ID, analysisResults, "source-message-id");

        // Assert
        assertNotNull(documentId, "Document ID should not be null");

        // Verify the analysis is stored with correct metadata
        List<TextSegment> storedSegments = vectorDatabaseService.getAllStoredSegments();
        assertEquals(1, storedSegments.size(), "Should have stored one segment");

        TextSegment segment = storedSegments.get(0);
        assertTrue(segment.text().contains("Analysis Results"), "Text should contain analysis header");
        assertTrue(segment.text().contains("emotion: anxiety"), "Text should contain emotion data");
        assertTrue(segment.text().contains("catastrophizing, fortune-telling"), "Text should contain cognitive distortions");

        assertEquals(TEST_SESSION_ID, segment.metadata().getString("sessionId"), "Session ID should be stored");
        assertEquals(TEST_USER_ID, segment.metadata().getString("userId"), "User ID should be stored");
        assertEquals("analysis", segment.metadata().getString("contentType"), "Content type should be 'analysis'");
        assertEquals("source-message-id", segment.metadata().getString("sourceMessageId"), "Source message ID should be stored");
    }

    @Test
    public void testFindSimilarContentWithMetadataFilters() {
        // Arrange - Store content with different metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("userId", TEST_USER_ID);
        metadata1.put("contentType", "message");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("userId", "other-user");
        metadata2.put("contentType", "message");

        vectorDatabaseService.storeEmbedding(null, "User 1 content about anxiety", metadata1);
        vectorDatabaseService.storeEmbedding(null, "User 2 content about anxiety", metadata2);

        // Act - Search with metadata filter for specific user
        Map<String, Object> filter = new HashMap<>();
        filter.put("userId", TEST_USER_ID);

        List<EmbeddingMatch<TextSegment>> matches = vectorDatabaseService.findSimilarContent(
                "anxiety", filter, 5);

        // Assert
        assertNotNull(matches, "Matches should not be null");
        assertFalse(matches.isEmpty(), "Should find at least one match");

        // All matches should have the filtered userId
        for (EmbeddingMatch<TextSegment> match : matches) {
            assertEquals(TEST_USER_ID, match.embedded().metadata().getString("userId"),
                    "All matches should have the filtered userId");
        }
    }

    @Test
    public void testProcessDocument() {
        // Arrange
        String documentId = "test-doc-789";
        String content = "This is a test document that should be split into multiple chunks. " +
                "It needs to be long enough to actually trigger chunking based on the token limit. " +
                "Let's add more content to ensure we get at least two chunks for testing purposes. " +
                "Cognitive Behavioral Therapy is a psychosocial intervention that aims to reduce symptoms " +
                "of various mental health conditions, primarily depression and anxiety disorders. " +
                "CBT focuses on challenging and changing cognitive distortions and behaviors, " +
                "improving emotional regulation, and developing personal coping strategies. " +
                "It has been demonstrated to be effective for a range of conditions, including mood, " +
                "anxiety, personality, eating, substance abuse, and psychotic disorders.";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", "educational");

        // Act
        List<String> chunkIds = vectorDatabaseService.processDocument(documentId, content, metadata);

        // Assert
        assertNotNull(chunkIds, "Chunk IDs should not be null");
        assertFalse(chunkIds.isEmpty(), "Should have at least one chunk");

        // Verify chunks are stored
        List<TextSegment> storedSegments = vectorDatabaseService.getAllStoredSegments();
        assertTrue(storedSegments.size() >= chunkIds.size(),
                "Should have at least as many stored segments as chunk IDs");

        // Verify chunk metadata
        for (TextSegment segment : storedSegments) {
            if (segment.metadata().getString("documentId").equals(documentId)) {
                assertEquals("educational", segment.metadata().getString("docType"),
                        "Document type metadata should be preserved");
                assertNotNull(segment.metadata().getString("chunkIndex"),
                        "Chunk index should be added");
            }
        }
    }

    @Test
    public void testBuildContextForPrompt() {
        // Arrange - Store some messages from different sessions
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("userId", TEST_USER_ID);
        metadata1.put("sessionId", TEST_SESSION_ID); // Current session
        metadata1.put("contentType", "message");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("userId", TEST_USER_ID);
        metadata2.put("sessionId", "past-session-1");
        metadata2.put("contentType", "message");

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("userId", TEST_USER_ID);
        metadata3.put("sessionId", "past-session-2");
        metadata3.put("contentType", "message");

        // Different user - should not appear in results
        Map<String, Object> metadata4 = new HashMap<>();
        metadata4.put("userId", "other-user");
        metadata4.put("sessionId", "other-session");
        metadata4.put("contentType", "message");

        vectorDatabaseService.storeEmbedding(null, "I'm feeling anxious about public speaking", metadata1);
        vectorDatabaseService.storeEmbedding(null, "Last week I had a panic attack before my presentation", metadata2);
        vectorDatabaseService.storeEmbedding(null, "My therapist suggested deep breathing for anxiety", metadata3);
        vectorDatabaseService.storeEmbedding(null, "I also have anxiety about public speaking", metadata4);

        // Act
        String context = vectorDatabaseService.buildContextForPrompt(
                TEST_SESSION_ID, TEST_USER_ID, "How do I deal with anxiety about giving presentations?", 5);

        // Assert
        assertNotNull(context, "Context should not be null");
        assertTrue(context.contains("Relevant historical context"), "Context should have the header");
        assertTrue(context.contains("past-session"), "Context should include content from past sessions");
        assertFalse(context.contains(TEST_SESSION_ID), "Context should not include current session");
        assertFalse(context.contains("other-user"), "Context should not include other users' content");
    }
}
