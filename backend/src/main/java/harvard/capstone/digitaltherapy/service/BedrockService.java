package harvard.capstone.digitaltherapy.service;

/**
 * Service class for interacting with the AWS Bedrock Nova Lite model.
 * Provides methods for generating text completions using a single prompt or
 * a sequence of messages with conversation history.
 * Handles formatting requests and parsing responses according to the Nova Lite API specification.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class BedrockService {

    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    // AWS Nova Lite model ID
    private static final String NOVA_LITE_MODEL_ID = "amazon.nova-lite-v1:0";

    // System prompt with default value if not specified in application.properties
    @Value("${bedrock.system-prompt:You are a helpful AI assistant powered by AWS Bedrock. Provide concise, accurate information in a friendly tone.}")
    private String systemPrompt;

    @Autowired
    public BedrockService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = new ObjectMapper();
    }

    public String generateTextWithNovaLite(String prompt) {
        try {
            logger.debug("Generating text with Nova Lite for prompt: '{}'", prompt);

            // Create request body for Nova Lite
            ObjectNode requestBody = objectMapper.createObjectNode();

            // Configure inference settings
            ObjectNode inferenceConfig = objectMapper.createObjectNode();
            inferenceConfig.put("max_new_tokens", 1000);
            requestBody.set("inferenceConfig", inferenceConfig);

            // Create messages array
            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Since Nova Lite doesn't support "system" role, we'll prepend the system prompt to the user's message
            String userMessageText = prompt;
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                userMessageText = systemPrompt + "\n\n" + prompt;
                logger.debug("Prepended system prompt to user message: '{}'", userMessageText);
            }

            // Create user message
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");

            ArrayNode userContentArray = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("text", userMessageText);
            userContentArray.add(userContent);

            userMessage.set("content", userContentArray);
            messagesArray.add(userMessage);

            // Add messages to request
            requestBody.set("messages", messagesArray);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request to Nova Lite: {}", requestBodyJson);

            // Create the request
            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                    .modelId(NOVA_LITE_MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBodyJson))
                    .build();

            // Invoke the model
            logger.debug("Sending request to AWS Bedrock...");
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(invokeModelRequest);

            // Parse the response
            String responseBody = response.body().asUtf8String();
            logger.debug("Raw response from Nova Lite: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Extract the generated text from Nova Lite's response format
            String responseText = "";
            if (jsonNode.has("output") && jsonNode.get("output").has("message")) {
                JsonNode message = jsonNode.get("output").get("message");
                if (message.has("content") && message.get("content").isArray()) {
                    JsonNode contentArray = message.get("content");
                    for (JsonNode contentItem : contentArray) {
                        if (contentItem.has("text")) {
                            responseText += contentItem.get("text").asText();
                        }
                    }
                }
            }

            logger.debug("Extracted response text: '{}'", responseText);
            return responseText;

        } catch (Exception e) {
            logger.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Error invoking Bedrock model: " + e.getMessage(), e);
        }
    }

    /**
     * Process a message from the WebSocket client.
     * This method uses Nova Lite model instead of Titan.
     *
     * @param message The message from the client
     * @return The response to send back to the client
     */
    public String processMessage(String message) {
        try {
            // Handle null or empty message
            if (message == null || message.trim().isEmpty()) {
                message = "Hello";
            }

            return generateTextWithNovaLite(message);
        } catch (Exception e) {
            logger.error("Error processing message", e);
            return "Error processing message: " + e.getMessage();
        }
    }

    /**
     * Process a message with conversation history from the WebSocket client.
     *
     * @param messages List of previous messages with roles and content
     * @return The response to send back to the client
     */
    public String processMessageWithHistory(List<Map<String, String>> messages) {
        try {
            logger.debug("Processing message with conversation history. History size: {}", messages.size());

            // Create request body for Nova Lite
            ObjectNode requestBody = objectMapper.createObjectNode();

            // Configure inference settings
            ObjectNode inferenceConfig = objectMapper.createObjectNode();
            inferenceConfig.put("max_new_tokens", 1000);
            requestBody.set("inferenceConfig", inferenceConfig);

            // Create messages array
            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Process and add all conversation messages
            boolean hasSystemPrompt = false;

            for (int i = 0; i < messages.size(); i++) {
                Map<String, String> msg = messages.get(i);
                String role = msg.get("role");
                String content = msg.get("content");

                logger.debug("Processing message {}: role={}, content='{}'", i, role, content);

                // Skip any system role messages as Nova Lite doesn't support them
                if ("system".equals(role)) {
                    // If this is the first message and it's a system message,
                    // we'll prepend it to the first user message we find
                    hasSystemPrompt = true;
                    continue;
                }

                // Only process messages with valid roles and content
                if (role != null && content != null) {
                    // If this is the first user message and we have a system prompt,
                    // prepend the system prompt to this user message
                    if ("user".equals(role) && hasSystemPrompt && !systemPrompt.isEmpty()) {
                        content = systemPrompt + "\n\n" + content;
                        logger.debug("Prepended system prompt to user message: '{}'", content);
                        hasSystemPrompt = false;  // Reset so we don't prepend to other messages
                    }

                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", role);

                    ArrayNode msgContentArray = objectMapper.createArrayNode();
                    ObjectNode contentNode = objectMapper.createObjectNode();
                    contentNode.put("text", content);
                    msgContentArray.add(contentNode);

                    messageNode.set("content", msgContentArray);
                    messagesArray.add(messageNode);
                }
            }

            // If we didn't find a user message to prepend the system prompt to,
            // and we have conversation messages, prepend it to the first message regardless of role
            if (hasSystemPrompt && !systemPrompt.isEmpty() && messagesArray.size() > 0) {
                JsonNode firstMsg = messagesArray.get(0);
                JsonNode contentArray = firstMsg.get("content");
                if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                    JsonNode firstContent = contentArray.get(0);
                    if (firstContent.has("text")) {
                        String updatedContent = systemPrompt + "\n\n" + firstContent.get("text").asText();
                        ((ObjectNode)firstContent).put("text", updatedContent);
                        logger.debug("Prepended system prompt to first message: '{}'", updatedContent);
                    }
                }
            }

            // Add messages to request
            requestBody.set("messages", messagesArray);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request to Nova Lite: {}", requestBodyJson);

            // Create the request
            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                    .modelId(NOVA_LITE_MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBodyJson))
                    .build();

            // Invoke the model
            logger.debug("Sending request to AWS Bedrock...");
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(invokeModelRequest);

            // Parse the response
            String responseBody = response.body().asUtf8String();
            logger.debug("Raw response from Nova Lite: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Extract the generated text from Nova Lite's response format
            String responseText = "";
            if (jsonNode.has("output") && jsonNode.get("output").has("message")) {
                JsonNode message = jsonNode.get("output").get("message");
                if (message.has("content") && message.get("content").isArray()) {
                    JsonNode contentArray = message.get("content");
                    for (JsonNode contentItem : contentArray) {
                        if (contentItem.has("text")) {
                            responseText += contentItem.get("text").asText();
                        }
                    }
                }
            }

            logger.debug("Extracted response text: '{}'", responseText);
            return responseText;

        } catch (Exception e) {
            logger.error("Error invoking Bedrock model with conversation history", e);
            return "Error processing message: " + e.getMessage();
        }
    }
}
