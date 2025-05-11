package harvard.capstone.digitaltherapy.llm.service;

/**
 * BedrockService is responsible for interacting with the AWS Bedrock runtime,
 * specifically the Nova Lite model. It handles formatting the request, sending
 * it to Bedrock, and parsing the structured response to extract the generated text.
 *
 * This class is designed to be low-level and reusable. It does not handle any file I/O
 * or orchestration logic — that responsibility is delegated to higher-level services.
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

@Service
public class BedrockService {

    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    // AWS Nova Lite model ID
    private static final String NOVA_LITE_MODEL_ID = "amazon.nova-lite-v1:0";

    // System prompt with default value if not specified in application.properties
    @Value("${bedrock.system-prompt:You are a helpful AI assistant powered by AWS Bedrock. Provide concise, accurate information in a friendly tone.}")
    public String systemPrompt;

    @Autowired
    public BedrockService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = new ObjectMapper();
    }

    private String parsePromptJSON(String jsonString){
        try{
            // Parse Transcript JSON from original prompt data string
            JsonNode rootNode = objectMapper.readTree(jsonString);
            if (rootNode != null && rootNode.isObject()){
                JsonNode resultsNode = rootNode.path("results");
                if (resultsNode != null && resultsNode.isObject()) { // Ensure "results" exists and is an object
                    JsonNode transcriptsNode = resultsNode.path("transcripts");
                    if (transcriptsNode != null && transcriptsNode.isArray() && transcriptsNode.size() > 0) { // Ensure "transcripts" is an array with elements
                        JsonNode transcriptNode = transcriptsNode.get(0).path("transcript");
                        if (transcriptNode != null && transcriptNode.isTextual()) { // Ensure "transcript" exists and is a string
                            return transcriptNode.asText();
                        }
                    }
                }
            }
            return jsonString;
        }
        catch (Exception e){
            logger.error("Invalid JSON prompt: " + jsonString);
            return jsonString;
        }
    }

    public String generateTextWithNovaLite(String prompt) {
        try {
            // Parse JSON format from transcription job data string
            prompt = parsePromptJSON(prompt);
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
}
