package harvard.capstone.digitaltherapy.burnout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import harvard.capstone.digitaltherapy.burnout.model.AssessmentDomain;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutQuestion;
import harvard.capstone.digitaltherapy.burnout.model.BurnoutSessionCreationResponse;
import harvard.capstone.digitaltherapy.burnout.orchestration.BurnoutAssessmentOrchestrator;
import harvard.capstone.digitaltherapy.burnout.service.BurnoutFhirService;
import harvard.capstone.digitaltherapy.utility.S3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.BinaryMessage;
import java.nio.ByteBuffer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BurnoutControllerTest {

    private BurnoutController controller;
    private BurnoutAssessmentOrchestrator orchestrator;
    private S3Utils s3Utils;
    private ObjectMapper objectMapper;
    private BurnoutFhirService burnoutFhirService;
    private WebSocketSession session;

    @BeforeEach
    void setup() {
        orchestrator = mock(BurnoutAssessmentOrchestrator.class);
        s3Utils = mock(S3Utils.class);
        objectMapper = new ObjectMapper();
        burnoutFhirService = mock(BurnoutFhirService.class);
        controller = new BurnoutController(objectMapper, orchestrator, s3Utils, burnoutFhirService);
        session = mock(WebSocketSession.class);
    }

    @Test
    void testStartBurnoutSession_sendsQuestions() throws Exception {
        AssessmentDomain domain = AssessmentDomain.values()[0];

        // Arrange
        BurnoutQuestion question = new BurnoutQuestion(
            "How do you feel today?", 
            "q_test_1", 
            domain, 
            false
        );
        BurnoutSessionCreationResponse response = new BurnoutSessionCreationResponse("sess123", List.of(question));
        when(orchestrator.createAssessmentSession("user123")).thenReturn(response);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "start-burnout");
        request.put("userId", "user123");

        // Act
        controller.handleMessage(session, request);

        // Assert
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assert payload.contains("\"type\":\"burnout-questions\"");
        assert payload.contains("\"sessionId\":\"sess123\"");
        assert payload.contains("How do you feel today?");
    }

    @Test
    void testHandleUserAnswer_recordsResponse() throws Exception {
        // Arrange
        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "answer");
        request.put("sessionId", "sess123");
        request.put("questionId", "q1");
        request.put("response", "I’m tired");

        when(orchestrator.recordResponse("sess123", "q1", "I’m tired", null, null)).thenReturn(true);

        // Act
        controller.handleMessage(session, request);

        // Assert
        verify(orchestrator).recordResponse("sess123", "q1", "I’m tired", null, null);
    }

    @Test
    void testHandleUnknownType_logsWarning() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "bogus-type");

        controller.handleMessage(session, request);

        // No exception should be thrown, log only
        verify(session, never()).sendMessage(any());
    }

    @Test
    void testHandleAudioMessage_uploadsToS3AndRecordsResponse() throws Exception {
        // Arrange
        byte[] mockAudioBytes = new byte[]{0x00, 0x01, 0x02};
        BinaryMessage binaryMessage = new BinaryMessage(ByteBuffer.wrap(mockAudioBytes));
        String sessionId = "sessAudio";
        String questionId = "qAudio";
        String expectedUrl = "https://s3.amazonaws.com/audio.mp3";

        when(s3Utils.uploadAudioBinaryFile(binaryMessage, "audio_sessAudio_qAudio.mp3")).thenReturn(expectedUrl);

        // Act
        controller.handleAudioMessage(session, sessionId, questionId, binaryMessage);

        // Assert
        verify(s3Utils).uploadAudioBinaryFile(binaryMessage, "audio_sessAudio_qAudio.mp3");
        verify(orchestrator).recordResponse(sessionId, questionId, "", null, expectedUrl);
    }

    @Test
    void testHandleVideoMessage_uploadsToS3AndRecordsResponse() throws Exception {
        // Arrange
        byte[] mockVideoBytes = new byte[]{0x0A, 0x0B, 0x0C};
        BinaryMessage binaryMessage = new BinaryMessage(ByteBuffer.wrap(mockVideoBytes));
        String sessionId = "sessVideo";
        String questionId = "qVideo";
        String expectedUrl = "https://s3.amazonaws.com/video.mp4";

        when(s3Utils.uploadVideoBinaryFile(binaryMessage, "video_sessVideo_qVideo.mp4")).thenReturn(expectedUrl);

        // Act
        controller.handleVideoMessage(session, sessionId, questionId, binaryMessage);

        // Assert
        verify(s3Utils).uploadVideoBinaryFile(binaryMessage, "video_sessVideo_qVideo.mp4");
        verify(orchestrator).recordResponse(sessionId, questionId, "", expectedUrl, null);
    }
}
