package harvard.capstone.digitaltherapy.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BedrockServiceTest {

    @Mock
    private BedrockRuntimeClient bedrockRuntimeClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private BedrockService bedrockService;

    private final String TEST_PROMPT = "This is a test prompt";
    private final String TEST_SYSTEM_PROMPT = "You are a helpful AI assistant";
    private final String EXPECTED_OUTPUT = "This is the model's response";

    @BeforeEach
    public void setup() {
        bedrockService = new BedrockService(bedrockRuntimeClient);
        ReflectionTestUtils.setField(bedrockService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(bedrockService, "systemPrompt", TEST_SYSTEM_PROMPT);
    }

    @Test
    public void testGenerateTextWithNovaLite() throws Exception {
        // Arrange
        // Create a model response
        ObjectNode responseContent = objectMapper.createObjectNode();
        ObjectNode outputNode = responseContent.putObject("output");
        ObjectNode messageNode = outputNode.putObject("message");
        ObjectNode contentItem = objectMapper.createObjectNode();
        contentItem.put("text", EXPECTED_OUTPUT);
        messageNode.putArray("content").add(contentItem);

        String responseJson = objectMapper.writeValueAsString(responseContent);

        // Mock the Bedrock runtime response
        InvokeModelResponse mockResponse = mock(InvokeModelResponse.class);
        when(mockResponse.body()).thenReturn(SdkBytes.fromUtf8String(responseJson));
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(TEST_PROMPT);

        // Assert
        assertEquals(EXPECTED_OUTPUT, result);

        // Verify the request contains both system prompt and user prompt
        ArgumentCaptor<InvokeModelRequest> requestCaptor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(bedrockRuntimeClient).invokeModel(requestCaptor.capture());

        InvokeModelRequest capturedRequest = requestCaptor.getValue();
        String requestBody = capturedRequest.body().asUtf8String();

        // Verify the request contains the correct model ID
        assertEquals("amazon.nova-lite-v1:0", capturedRequest.modelId());

        // Verify the request contains both prompts
        JsonNode requestNode = objectMapper.readTree(requestBody);
        JsonNode messagesNode = requestNode.get("messages");
        JsonNode userMessage = messagesNode.get(0);
        JsonNode userContent = userMessage.get("content").get(0);

        String messageText = userContent.get("text").asText();

        // Check that the message contains both the system prompt and user prompt
        assert(messageText.contains(TEST_SYSTEM_PROMPT));
        assert(messageText.contains(TEST_PROMPT));
    }

    @Test
    public void testParseTranscriptJSON() throws Exception {
        // Arrange - Create a transcript JSON structure
        ObjectNode transcriptJson = objectMapper.createObjectNode();
        ObjectNode resultsNode = transcriptJson.putObject("results");
        ObjectNode transcriptNode = objectMapper.createObjectNode();
        transcriptNode.put("transcript", "Transcribed text");
        resultsNode.putArray("transcripts").add(transcriptNode);

        String jsonString = objectMapper.writeValueAsString(transcriptJson);

        // Use reflection to access the private method
        String result = (String) ReflectionTestUtils.invokeMethod(
                bedrockService,
                "parsePromptJSON",
                jsonString
        );

        // Assert
        assertEquals("Transcribed text", result);
    }

    @Test
    public void testInvalidJSONFallsBackToOriginalPrompt() throws Exception {
        // Arrange - Create invalid JSON
        String invalidJson = "This is not valid JSON";

        // Use reflection to access the private method
        String result = (String) ReflectionTestUtils.invokeMethod(
                bedrockService,
                "parsePromptJSON",
                invalidJson
        );

        // Assert - Should return the original string
        assertEquals(invalidJson, result);
    }

    @Test
    public void testHandleBedrockError() {
        // Arrange
        RuntimeException exception = new RuntimeException("Test error");
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenThrow(exception);

        // Act & Assert
        RuntimeException thrownException = assertThrows(
                RuntimeException.class,
                () -> bedrockService.generateTextWithNovaLite(TEST_PROMPT)
        );

        assertEquals("Error invoking Bedrock model: Test error", thrownException.getMessage());
        assertEquals(exception, thrownException.getCause());
    }
}
