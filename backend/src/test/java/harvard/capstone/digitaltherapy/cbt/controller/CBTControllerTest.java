package harvard.capstone.digitaltherapy.cbt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.aws.service.RekognitionService;
import harvard.capstone.digitaltherapy.cbt.service.OrchestrationService;
import harvard.capstone.digitaltherapy.llm.service.LLMProcessingService;
import harvard.capstone.digitaltherapy.aws.service.PollyService;
import harvard.capstone.digitaltherapy.aws.service.TranscribeService;
import harvard.capstone.digitaltherapy.cbt.service.CBTHelper;
import harvard.capstone.digitaltherapy.llm.service.S3StorageService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
@ExtendWith(MockitoExtension.class)
class CBTControllerTest {

    private MockMvc mockMvc;

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
  
    @Mock
    private OrchestrationService orchestrationService;
    @Mock
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cbtController = new CBTController(
                objectMapper,
                s3Service,
                cbtHelper,
                transcribeService,
                llmProcessingService,
                pollyService,
                rekognitionService,
                orchestrationService,
                s3StorageService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(cbtController).build();
    }
    @Test
    public void testCBTControllerConstructor() {
        CBTController cbtController = new CBTController(
                objectMapper,
                s3Service,
                cbtHelper,
                transcribeService,
                llmProcessingService,
                pollyService,
                rekognitionService,
                orchestrationService,
                s3StorageService
        );

        assertNotNull(cbtController, "CBTController should be instantiated successfully");
    }
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
                rekognitionService,
                orchestrationService,
                s3StorageService
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
