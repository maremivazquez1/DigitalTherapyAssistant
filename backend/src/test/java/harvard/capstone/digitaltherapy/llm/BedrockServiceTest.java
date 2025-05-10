package harvard.capstone.digitaltherapy.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.llm.service.BedrockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BedrockServiceTest {

    @Mock
    private BedrockRuntimeClient bedrockRuntimeClient;

    @InjectMocks
    private BedrockService bedrockService;

    @Mock
    private InvokeModelResponse invokeModelResponse;

    @Mock
    private ObjectMapper objectMapper;
    /**
     * Test case to verify the successful initialization of BedrockService with BedrockRuntimeClient
     */
    @Test
    public void testBedrockServiceInitialization() {
        assertNotNull(bedrockService, "BedrockService should be initialized");
    }

    /**
     * Tests the generateTextWithNovaLite method when the system prompt is not empty,
     * the response has an output with a message, the message has content as an array,
     * but the content items do not have a "text" field.
     */
    @Test
    public void test_generateTextWithNovaLite_emptyResponseText() throws JsonProcessingException {
        // Arrange
        String prompt = "Test prompt";

        // Create response JSON structure
        ObjectMapper realMapper = new ObjectMapper();
        ObjectNode responseNode = realMapper.createObjectNode();
        ObjectNode outputNode = responseNode.putObject("output");
        ObjectNode messageNode = outputNode.putObject("message");
        messageNode.putArray("content").addObject();

        // Mock only the Bedrock client response
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(InvokeModelResponse.builder()
                        .body(SdkBytes.fromUtf8String(responseNode.toString()))
                        .build());

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals("", result, "The response text should be empty when content items do not have 'text' field");
    }

    /**
     * Tests the generateTextWithNovaLite method when the system prompt is empty,
     * and the response from Nova Lite contains a valid output structure.
     */
    @Test
    public void test_generateTextWithNovaLite_emptySystemPrompt() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResponse = "Generated response";

        // Use real ObjectMapper for creating test data
        ObjectMapper realMapper = new ObjectMapper();
        ObjectNode responseBody = realMapper.createObjectNode();
        ObjectNode output = responseBody.putObject("output");
        ObjectNode message = output.putObject("message");
        ArrayNode content = message.putArray("content");
        ObjectNode contentItem = content.addObject();
        contentItem.put("text", expectedResponse);

        // Mock the Bedrock client response
        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(responseBody.toString()))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(mockResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals(expectedResponse, result);
    }


    @Test
    public void test_generateTextWithNovaLite_extractsResponseTextCorrectly() throws Exception {
        // Arrange
        String prompt = "Test prompt";
        String expectedResponseText = "Generated response";

        // Create actual JSON response structure
        ObjectMapper realMapper = new ObjectMapper();
        ObjectNode responseJson = realMapper.createObjectNode();
        ObjectNode outputNode = responseJson.putObject("output");
        ObjectNode messageNode = outputNode.putObject("message");
        ArrayNode contentArray = messageNode.putArray("content");
        ObjectNode contentItem = contentArray.addObject();
        contentItem.put("text", expectedResponseText);

        // Mock only the BedrockRuntimeClient response
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(InvokeModelResponse.builder()
                        .body(SdkBytes.fromUtf8String(responseJson.toString()))
                        .build());

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals(expectedResponseText, result);
    }



    /**
     * Tests the behavior of generateTextWithNovaLite when the Bedrock service throws an exception.
     * This test verifies that the method properly handles and wraps any exceptions thrown during the invocation of the Bedrock model.
     */
    @Test
    public void test_generateTextWithNovaLite_handleServiceException() {
        // Arrange
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(new RuntimeException("Bedrock service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> bedrockService.generateTextWithNovaLite("test prompt"),
                "Error invoking Bedrock model: Bedrock service error");
    }

    /**
     * Tests the generateTextWithNovaLite method when:
     * - systemPrompt is not null and not empty
     * - The response JSON has an "output" field with a "message" field
     * - The "message" field does not have a "content" array
     *
     * Expected: The method should return an empty string as no text content is present.
     */
    @Test
    public void test_generateTextWithNovaLite_whenResponseLacksContentArray() {
        // Arrange
        String prompt = "Test prompt";
        String mockResponse = "{\"output\":{\"message\":{}}}";
        InvokeModelResponse mockInvokeModelResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(mockResponse))
                .build();
        Mockito.when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(mockInvokeModelResponse);

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals("", result, "Expected an empty string when response lacks content array");
    }

    @Test
    public void test_generateTextWithNovaLite_whenSystemPromptNotEmptyAndInvalidResponseStructure() {
        // Arrange
        String prompt = "Test prompt";
        String systemPrompt = "You are a helpful AI assistant.";
        bedrockService.systemPrompt = systemPrompt;

        // Create a real ObjectMapper for creating the test JSON
        ObjectMapper realObjectMapper = new ObjectMapper();
        ObjectNode responseJson = realObjectMapper.createObjectNode();
        responseJson.put("someField", "someValue");

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(invokeModelResponse);
        when(invokeModelResponse.body()).thenReturn(SdkBytes.fromUtf8String(responseJson.toString()));

        // Act
        String result = bedrockService.generateTextWithNovaLite(prompt);

        // Assert
        assertEquals("", result, "The response text should be empty when the JSON structure is invalid");
    }

}
