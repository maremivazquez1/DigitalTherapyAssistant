package harvard.capstone.digitaltherapy.persistence;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VectorDatabaseServiceTest {

    /**
     * Tests the behavior of findSimilarContent when provided with empty metadata filters.
     */
    @Test
    public void testFindSimilarContentWithEmptyMetadataFilters() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String query = "test query";
        Map<String, Object> emptyMetadataFilters = new HashMap<>();
        int maxResults = 5;

        // Store some test data with mutable maps
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("key1", "value1");
        service.storeEmbedding("doc1", "Test content 1", metadata1);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("key2", "value2");
        service.storeEmbedding("doc2", "Test content 2", metadata2);

        // Act
        List<EmbeddingMatch<TextSegment>> result = service.findSimilarContent(query, emptyMetadataFilters, maxResults);
        List<EmbeddingMatch<TextSegment>> expectedResult = service.findSimilarContent(query, maxResults);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedResult.size(), result.size(), "Result size should be the same as without filters");
    }


    /**
     * Tests the behavior of findSimilarContent when provided with null metadata filters.
     */
    @Test
    public void testFindSimilarContentWithNullMetadataFilters() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String query = "test query";
        Map<String, Object> nullMetadataFilters = null;
        int maxResults = 5;

        // Store some test data
        service.storeEmbedding("doc1", "Test content 1", createMutableMetadata("key1", "value1"));
        service.storeEmbedding("doc2", "Test content 2", createMutableMetadata("key2", "value2"));

        // Act
        List<EmbeddingMatch<TextSegment>> result = service.findSimilarContent(query, nullMetadataFilters, maxResults);
        List<EmbeddingMatch<TextSegment>> expectedResult = service.findSimilarContent(query, maxResults);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedResult.size(), result.size(), "Result size should be the same as without filters");
    }

    /**
     * Helper method to create a mutable metadata map
     */
    private Map<String, Object> createMutableMetadata(String key, Object value) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(key, value);
        return metadata;
    }


    /**
     * Tests updating an embedding with null content throws IllegalArgumentException.
     */
    @Test
    public void testUpdateEmbeddingWithNullContent() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String documentId = "test-doc-id";
        Map<String, Object> metadata = new HashMap<>();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateEmbedding(documentId, null, metadata),
                "Should throw IllegalArgumentException when content is null");

        assertTrue(exception.getMessage().contains("text cannot be null or blank"),
                "Exception message should indicate that text cannot be null");

        // Verify no segments were stored
        List<TextSegment> segments = service.getAllStoredSegments();
        assertTrue(segments.isEmpty(), "No segments should be stored when content is null");
    }


    /**
     * Tests updating an embedding with a null document ID.
     */
    @Test
    public void testUpdateEmbeddingWithNullDocumentId() {
        // Arrange
        VectorDatabaseService service = new VectorDatabaseService();
        String newContent = "Updated content";
        Map<String, Object> metadata = new HashMap<>();

        // Act
        boolean result = service.updateEmbedding(null, newContent, metadata);

        // Assert
        assertTrue(result, "Updating with null document ID should succeed");

        // Verify the update
        List<TextSegment> segments = service.getAllStoredSegments();
        assertFalse(segments.isEmpty(), "Segments should not be empty after update");
        TextSegment segment = segments.get(0);
        assertNotNull(segment.metadata().getString("documentId"),
                "Document ID should be generated automatically");
        assertEquals(newContent, segment.text(),
                "Content should match in stored segment");
    }
}
