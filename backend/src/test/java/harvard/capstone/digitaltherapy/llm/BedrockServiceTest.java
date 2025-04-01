package harvard.capstone.digitaltherapy.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.llm.service.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceQuotaExceededException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BedrockServiceTest {

    private BedrockRuntimeClient bedrockRuntimeClient;
    private BedrockService bedrockService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
        bedrockService = new BedrockService(bedrockRuntimeClient);
        objectMapper = new ObjectMapper();

        // Inject systemPrompt via reflection
        var field = BedrockService.class.getDeclaredField("systemPrompt");
        field.setAccessible(true);
        field.set(bedrockService, "You are a helpful assistant.");
    }

    @Test
    public void testServiceInitialization() {
        assertNotNull(bedrockService);
    }

    @Test
    public void generateTextWithNovaLite_returnsExpectedResponse() {
        // Arrange
        String prompt = "Hello";
        String expectedResponse = "This is a test response.";

        // Create the correct response format that matches what BedrockService is expecting
        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": [
                    {
                      "text": "This is a test response."
                    }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
    }

    @Test
    public void generateTextWithNovaLite_withNullPrompt_shouldHandleGracefully() {
        // Arrange
        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": [
                    {
                      "text": "Empty prompt response"
                    }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(null);

        // Assert
        assertNotNull(result, "Response should not be null even with null input");

        // Verify that the service handled the null input and still made a request
        verify(bedrockRuntimeClient, times(1)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    public void generateTextWithNovaLite_withEmptyPrompt_shouldHandleGracefully() {
        // Arrange
        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": [
                    {
                      "text": "Empty prompt response"
                    }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite("");

        // Assert
        assertNotNull(result, "Response should not be null even with empty input");
    }

    @Test
    public void generateTextWithNovaLite_systemPromptIncluded() throws JsonProcessingException {
        // Arrange
        String userPrompt = "Tell me a joke";
        String systemPrompt = "You are a helpful assistant.";

        // Capture the request to verify the prompt content
        ArgumentCaptor<InvokeModelRequest> requestCaptor = ArgumentCaptor.forClass(InvokeModelRequest.class);

        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": [
                    {
                      "text": "Why did the chicken cross the road?"
                    }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(requestCaptor.capture())).thenReturn(mockResponse);

        // Act
        bedrockService.generateTextWithNovaLite(userPrompt);

        // Assert
        InvokeModelRequest capturedRequest = requestCaptor.getValue();
        String requestBodyJson = capturedRequest.body().asUtf8String();

        // Parse the request to verify system prompt is included
        var jsonNode = objectMapper.readTree(requestBodyJson);
        String messageText = jsonNode.get("messages").get(0).get("content").get(0).get("text").asText();

        assertTrue(messageText.contains(systemPrompt), "System prompt should be included in the request");
        assertTrue(messageText.contains(userPrompt), "User prompt should be included in the request");
    }

    @Test
    public void generateTextWithNovaLite_handlesEmptyResponse() {
        // Arrange
        String prompt = "Hello";

        // Empty response with valid structure but no text
        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": []
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertNotNull(result, "Result should not be null even with empty response");
        assertEquals("", result, "Result should be empty string when response has no content");
    }

    @Test
    public void generateTextWithNovaLite_handlesMalformedResponse() {
        // Arrange
        String prompt = "Hello";

        // Malformed JSON response
        String mockResponseJson = "{ malformed json }";

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bedrockService.generateTextWithNovaLite(prompt);
        });

        assertTrue(exception.getMessage().contains("Error invoking Bedrock model"));
    }

    @Test
    public void generateTextWithNovaLite_handlesServiceException() {
        // Arrange
        String prompt = "Hello";

        // Mock a service exception
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(ServiceQuotaExceededException.builder().message("Quota exceeded").build());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bedrockService.generateTextWithNovaLite(prompt);
        });

        assertTrue(exception.getMessage().contains("Error invoking Bedrock model"));
        assertTrue(exception.getCause() instanceof ServiceQuotaExceededException);
    }

    @Test
    public void generateTextWithNovaLite_concatenatesMultipleContentItems() {
        // Arrange
        String prompt = "Hello";
        String responseText1 = "First part of response.";
        String responseText2 = " Second part of response.";

        // Response with multiple content items
        String mockResponseJson = """
            {
              "output": {
                "message": {
                  "content": [
                    {
                      "text": "First part of response."
                    },
                    {
                      "text": " Second part of response."
                    }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromString(mockResponseJson, StandardCharsets.UTF_8))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals(responseText1 + responseText2, result, "Response should concatenate multiple content items");
    }
}