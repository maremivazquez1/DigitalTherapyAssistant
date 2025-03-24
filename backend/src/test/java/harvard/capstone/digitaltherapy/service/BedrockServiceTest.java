package harvard.capstone.digitaltherapy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

public class BedrockServiceTest {

    private BedrockRuntimeClient bedrockRuntimeClient;
    private BedrockService bedrockService;

    @BeforeEach
    public void setUp() throws Exception {
        bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
        bedrockService = new BedrockService(bedrockRuntimeClient);

        // Inject systemPrompt via reflection
        Field systemPromptField = BedrockService.class.getDeclaredField("systemPrompt");
        systemPromptField.setAccessible(true);
        systemPromptField.set(bedrockService, "You are a helpful assistant.");
    }

    @Test
    public void testProcessMessage_withValidPrompt_returnsExpectedText() {
        String mockResponse = """
            {
              "output": {
                "message": {
                  "content": [
                    { "text": "Hello, how can I assist you today?" }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse invokeResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(mockResponse, StandardCharsets.UTF_8))
            .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(invokeResponse);

        String result = bedrockService.processMessage("Hello");
        assertEquals("Hello, how can I assist you today?", result);
    }

    @Test
    public void testProcessMessage_withEmptyInput_usesDefault() {
        String mockResponse = """
            {
              "output": {
                "message": {
                  "content": [
                    { "text": "Hi there!" }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse invokeResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(mockResponse, StandardCharsets.UTF_8))
            .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(invokeResponse);

        String result = bedrockService.processMessage("   ");
        assertEquals("Hi there!", result);
    }

    @Test
    public void testProcessMessageWithHistory_returnsExpectedText() {
        String mockResponse = """
            {
              "output": {
                "message": {
                  "content": [
                    { "text": "Let's work on that together." }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse invokeResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(mockResponse, StandardCharsets.UTF_8))
            .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(invokeResponse);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "I'm feeling stressed")
        );

        String result = bedrockService.processMessageWithHistory(messages);
        assertEquals("Let's work on that together.", result);
    }

    @Test
    public void testProcessMessageWithHistory_withEmptyList_returnsValidResponse() {
        String mockResponse = """
            {
              "output": {
                "message": {
                  "content": [
                    { "text": "Hello, I'm here to help." }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse invokeResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(mockResponse, StandardCharsets.UTF_8))
            .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(invokeResponse);

        String result = bedrockService.processMessageWithHistory(List.of());
        assertEquals("Hello, I'm here to help.", result);
    }

    @Test
    public void testProcessMessageWithHistory_withException_returnsErrorMessage() {
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenThrow(new RuntimeException("Simulated failure"));

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "I'm anxious")
        );

        String result = bedrockService.processMessageWithHistory(messages);
        assertEquals(true, result.startsWith("Error processing message:"));
    }

    @Test
    public void testProcessMessageWithHistory_prependsSystemPromptToUserMessage() {
        String mockResponse = """
            {
              "output": {
                "message": {
                  "content": [
                    { "text": "System prompt successfully prepended." }
                  ]
                }
              }
            }
            """;

        InvokeModelResponse invokeResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(mockResponse, StandardCharsets.UTF_8))
            .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(invokeResponse);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "This is system prompt"),
            Map.of("role", "user", "content", "What should I do?")
        );

        String result = bedrockService.processMessageWithHistory(messages);
        assertEquals("System prompt successfully prepended.", result);
    }
}
