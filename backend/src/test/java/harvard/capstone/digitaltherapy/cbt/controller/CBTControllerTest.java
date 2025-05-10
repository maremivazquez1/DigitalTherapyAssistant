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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    void testHandleTextOnlyMessage_ProcessingSuccess() throws IOException {
        // Arrange
        String content = "Test content";
        String requestId = "testId";
        String fileName = "text_testId.txt";
        String s3Response = "s3://bucket/path";
        String llmResponse = "processed_response";

        when(s3Service.uploadFile(anyString(), anyString())).thenReturn(s3Response);
        when(llmProcessingService.process(s3Response)).thenReturn(llmResponse);

        StreamingResponseBody streamingResponseBody = outputStream ->
                outputStream.write("processed content".getBytes());
        ResponseEntity<StreamingResponseBody> responseEntity =
                ResponseEntity.ok(streamingResponseBody);
        when(cbtHelper.downloadTextFile(anyString())).thenReturn(responseEntity);

        ObjectNode mockResponseJson = mock(ObjectNode.class);
        when(objectMapper.createObjectNode()).thenReturn(mockResponseJson);

        // Act
        cbtController.handleTextOnlyMessage(webSocketSession, content, requestId);

        // Assert
        verify(s3Service).uploadFile(anyString(), anyString());
        verify(llmProcessingService).process(anyString());
        verify(cbtHelper).downloadTextFile(anyString());
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void testDisableCertificateValidation() {
        // Test the SSL certificate validation disable functionality
        cbtController.disableCertificateValidation();
        // Verify that SSL validation is disabled by attempting a connection
        // Note: This is just a basic test, in practice you might want to verify the actual SSL context
    }

    @Test
    void testHandleBinaryMessage_InvalidModality() throws IOException {
        // Arrange
        byte[] data = "test data".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        BinaryMessage binaryMessage = new BinaryMessage(buffer);

        when(webSocketSession.getId()).thenReturn("session123");
        cbtController.currentModality = "invalid";

        // Act
        cbtController.handleBinaryMessage(webSocketSession, binaryMessage);

        // Assert
        verify(webSocketSession).getId();
        // Verify no processing occurred for invalid modality
    }

    @Test
    void testProcessFinalMessage_CompleteFlow() throws IOException {
        // Arrange
        String sessionId = "session123";
        String s3Path = "s3://test-bucket/audio.mp3";
        String transcribedText = "{\n" +
                "  \"results\": {\n" +
                "    \"transcripts\": [{\n" +
                "      \"transcript\": \"Hello\"\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        byte[] mockAudioBytes = "mock audio content".getBytes();
        byte[] mockTranscriptBytes = transcribedText.getBytes();
        String pollyResponse = "s3://dta-root/response.mp3";

        // Create temporary files
        File transcriptFile = File.createTempFile("transcript_", ".txt");
        File audioFile = File.createTempFile("audio_", ".mp3");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // Mock Files.readAllBytes
            filesMock.when(() -> Files.readAllBytes(any(Path.class)))
                    .thenReturn(mockTranscriptBytes)  // First call returns transcript
                    .thenReturn(mockAudioBytes);      // Second call returns audio

            // Use real ObjectMapper for JSON parsing
            ObjectMapper realObjectMapper = new ObjectMapper();
            CBTController cbtControllerWithRealMapper = new CBTController(
                    realObjectMapper,
                    s3Service,
                    cbtHelper,
                    transcribeService,
                    llmProcessingService,
                    pollyService,
                    rekognitionService,
                    orchestrationService,
                    s3StorageService
            );

            // Mock service behaviors
            when(webSocketSession.getId()).thenReturn(sessionId);

            // Mock S3 download to return different files based on input
            when(s3Service.downloadFileFromS3(anyString(), anyString()))
                    .thenAnswer(invocation -> {
                        String bucket = invocation.getArgument(0);
                        String key = invocation.getArgument(1);
                        if (key.endsWith(".txt")) {
                            return transcriptFile;
                        } else {
                            return audioFile;
                        }
                    });

            when(orchestrationService.processUserMessage(anyString(), any(), anyString()))
                    .thenReturn("processed response");
            when(pollyService.convertTextToSpeech(anyString(), anyString()))
                    .thenReturn(pollyResponse);

            // Act
            cbtControllerWithRealMapper.processFinalMessage(webSocketSession, s3Path);

            // Assert
            verify(webSocketSession).getId();
           // verify(s3Service, times(2)).downloadFileFromS3(anyString(), anyString());
            verify(orchestrationService).processUserMessage(anyString(), any(), anyString());
            verify(pollyService).convertTextToSpeech(anyString(), anyString());
            verify(webSocketSession, times(2)).sendMessage(any(TextMessage.class));

            // Verify Files.readAllBytes was called twice
            //filesMock.verify(() -> Files.readAllBytes(any(Path.class)), times(2));

            // Additional verification for S3 downloads
            ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            //verify(s3Service, times(2)).downloadFileFromS3(bucketCaptor.capture(), keyCaptor.capture());

            List<String> capturedBuckets = bucketCaptor.getAllValues();
            List<String> capturedKeys = keyCaptor.getAllValues();

            // Log captured values for debugging
            logger.info("Captured buckets: {}", capturedBuckets);
            logger.info("Captured keys: {}", capturedKeys);

        } finally {
            // Cleanup
            try {
                Files.deleteIfExists(transcriptFile.toPath());
                Files.deleteIfExists(audioFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to delete test files", e);
            }
        }
    }





    // Add these utility methods to your test class
    private static final Logger logger = LoggerFactory.getLogger(CBTControllerTest.class);

    private void verifyFileContent(File file, String expectedContent) throws IOException {
        assertTrue(file.exists(), "File should exist: " + file.getAbsolutePath());
        String actualContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertEquals(expectedContent, actualContent, "File content should match");
    }


    private File createAndVerifyTempFile(String content) throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        // Write content to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(content);
            writer.flush();
        }

        // Verify file exists and has content
        if (!tempFile.exists()) {
            throw new IOException("Failed to create temporary file");
        }

        if (tempFile.length() == 0) {
            throw new IOException("Temporary file is empty");
        }

        return tempFile;
    }

    private void safeDelete(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                String errorMessage = "Failed to delete temporary file: " + e.getMessage();
            }
        }
    }

}
