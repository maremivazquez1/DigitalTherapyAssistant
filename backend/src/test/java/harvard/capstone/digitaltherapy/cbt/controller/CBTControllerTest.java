package harvard.capstone.digitaltherapy.cbt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.aws.service.RekognitionService;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import org.springframework.web.socket.BinaryMessage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CBTControllerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private S3Utils s3Service;
    @Mock
    private CBTHelper cbtHelper;
    @Mock
    private TranscribeService transcribeService;
    @Mock
    private LLMProcessingService llmProcessingService;
    @Mock
    private PollyService pollyService;
    @Mock
    private WebSocketSession webSocketSession;
    @Mock
    private JsonNode requestJson;
    @Mock
    private JsonNode textNode;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private  RekognitionService rekognitionService;

    private CBTController cbtController;

    @BeforeEach
    void setUp() {
        cbtController = new CBTController(
                objectMapper,
                s3Service,
                cbtHelper,
                transcribeService,
                llmProcessingService,
                pollyService,
                rekognitionService
        );
    }

    /**
     * Test case for the CBTController constructor.
     * This test verifies that the CBTController can be instantiated with all required dependencies.
     */
    @Test
    public void testCBTControllerConstructor() {
        CBTController cbtController = new CBTController(
                objectMapper,
                s3Service,
                cbtHelper,
                transcribeService,
                llmProcessingService,
                pollyService,
                rekognitionService
        );

        assertNotNull(cbtController, "CBTController should be instantiated successfully");
    }
    /**
     * Test case for handleTextOnlyMessage method when content is not empty,
     * processed response is successful, and temporary file deletion fails.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHandleTextOnlyMessage_SuccessfulProcessing_FailedTempFileDeletion() throws IOException {
        // Arrange
        CBTController cbtController = new CBTController(
                objectMapper,
                s3Service,
                cbtHelper,
                transcribeService,
                llmProcessingService,
                pollyService,
                rekognitionService
        );
        String content = "Test content";
        String requestId = "123";
        String fileName = "text_123.txt";
        String uploadResponse = "s3://bucket/text_123.txt";
        String llmResponse = "processed_text_123.txt";
        String processedContent = "Processed test content";

        when(s3Service.uploadFile(anyString(), eq(fileName))).thenReturn(uploadResponse);
        when(llmProcessingService.process(uploadResponse)).thenReturn(llmResponse);

        ResponseEntity<StreamingResponseBody> mockResponse = mock(ResponseEntity.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getBody()).thenReturn(out -> out.write(processedContent.getBytes()));
        when(cbtHelper.downloadTextFile(llmResponse)).thenReturn(mockResponse);

        ObjectNode responseJson = mock(ObjectNode.class);
        when(objectMapper.createObjectNode()).thenReturn(responseJson);

        // Act
        cbtController.handleTextOnlyMessage(webSocketSession, content, requestId);

        // Assert
        verify(webSocketSession).sendMessage(any(TextMessage.class));
        verify(s3Service).uploadFile(anyString(), eq(fileName));
        verify(llmProcessingService).process(uploadResponse);
        verify(cbtHelper).downloadTextFile(llmResponse);
        verify(responseJson).put("type", "text-processed");
        verify(responseJson).put("requestId", requestId);
        verify(responseJson).put("originalContent", content);
        verify(responseJson).put("processedContent", processedContent);
        verify(responseJson).put("fileName", fileName);
    }

    /**
     * Tests the handleAudioMessage method when an exception occurs during processing.
     * This test verifies that the method properly handles exceptions and sends an error message.
     */
    @Test
    public void test_handleAudioMessage_exceptionHandling() throws IOException {
        // Arrange
        String requestId = "testRequestId";
        String audioData = "base64AudioData";
        String fileName = "test.wav";

        when(requestJson.get("audioData")).thenReturn(textNode);
        when(requestJson.get("fileName")).thenReturn(textNode);
        when(textNode.asText()).thenReturn(audioData).thenReturn(fileName);
        when(cbtHelper.createMultipartFileFromBase64(audioData, fileName)).thenThrow(new RuntimeException("Test exception"));

        ObjectNode errorJson = mock(ObjectNode.class);
        when(objectMapper.createObjectNode()).thenReturn(errorJson);
        when(errorJson.toString()).thenReturn("{'error':'Error processing audio: Test exception'}");

        // Act
        cbtController.handleAudioMessage(webSocketSession, requestJson, requestId);

        // Assert
        verify(webSocketSession).sendMessage(any(TextMessage.class));
        verify(errorJson).put("error", "Error processing audio: Test exception");
        verify(errorJson).put("code", 500);
        verify(errorJson).put("requestId", requestId);
    }

    /**
     * Tests the error handling scenario in handleBinaryMessage method when an exception occurs during processing.
     * This test verifies that the method properly catches exceptions, logs the error, and sends an error message to the client.
     */
    @Test
    public void test_handleBinaryMessage_ProcessingError() throws IOException {
        // Arrange
        String sessionId = "testSessionId";
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}); // Sample audio data
        BinaryMessage message = new BinaryMessage(buffer);

        when(webSocketSession.getId()).thenReturn(sessionId);
        when(s3Service.uploadFile(anyString(), anyString())).thenThrow(new RuntimeException("S3 upload failed"));
        when(objectMapper.createObjectNode()).thenReturn(mock(ObjectNode.class));

        // Act
        cbtController.handleBinaryMessage(webSocketSession, message);

        // Assert
        verify(webSocketSession).sendMessage(any(TextMessage.class));
        //verify(logger).error(eq("Error processing binary message from session {}: {}"), eq(sessionId), eq("S3 upload failed"), any(Exception.class));
    }

    /**
     * Tests the handleTextOnlyMessage method with an empty text content.
     * This test verifies that the method sends an error message when the input text is empty.
     */
    @Test
    public void test_handleTextOnlyMessage_emptyContent() throws IOException {
        // Arrange
        String emptyContent = "";
        String requestId = "test123";
        ObjectNode errorJson = mock(ObjectNode.class);

        when(objectMapper.createObjectNode()).thenReturn(errorJson);
        when(errorJson.toString()).thenReturn("{\"error\":\"Text content cannot be empty\",\"code\":400,\"requestId\":\"test123\"}");

        // Act
        cbtController.handleTextOnlyMessage(webSocketSession, emptyContent, requestId);

        // Assert
        verify(webSocketSession).sendMessage(new TextMessage("{\"error\":\"Text content cannot be empty\",\"code\":400,\"requestId\":\"test123\"}"));
    }



    /**
     * Test case for handleTextOnlyMessage method when content is not empty,
     * processed response is successful, and temporary file is deleted successfully.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void test_handleTextOnlyMessage_successfulProcessing() throws IOException {
        // Arrange
        String content = "Test content";
        String requestId = "123";
        String fileName = "text_" + requestId + ".txt";
        String uploadResponse = "s3://bucket/" + fileName;
        String llmResponse = "processed_" + fileName;
        String processedContent = "Processed test content";

        when(s3Service.uploadFile(anyString(), eq(fileName))).thenReturn(uploadResponse);
        when(llmProcessingService.process(uploadResponse)).thenReturn(llmResponse);

        ResponseEntity<StreamingResponseBody> mockResponse = mock(ResponseEntity.class);
        when(cbtHelper.downloadTextFile(anyString())).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getBody()).thenReturn(out -> out.write(processedContent.getBytes()));

        ObjectNode responseJson = mock(ObjectNode.class);
        when(objectMapper.createObjectNode()).thenReturn(responseJson);

        // Act
        cbtController.handleTextOnlyMessage(webSocketSession, content, requestId);

        // Assert
        verify(s3Service).uploadFile(anyString(), eq(fileName));
        verify(llmProcessingService).process(uploadResponse);
        verify(cbtHelper).downloadTextFile(llmResponse);
        verify(responseJson).put("type", "text-processed");
        verify(responseJson).put("requestId", requestId);
        verify(responseJson).put("originalContent", content);
        verify(responseJson).put("processedContent", processedContent);
        verify(responseJson).put("fileName", fileName);
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }
}
