package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harvard.capstone.digitaltherapy.burnout.controller.BurnoutController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;

import static org.mockito.Mockito.*;

public class BurnoutWebSocketHandlerTest {

    private BurnoutWebSocketHandler handler;
    private ObjectMapper objectMapper;
    private BurnoutController burnoutController;
    private WebSocketSession session;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        burnoutController = mock(BurnoutController.class);
        handler = new BurnoutWebSocketHandler(objectMapper, burnoutController);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
    }

    @Test
    void handleTextMessage_forwardsToControllerForStandardMessage() throws Exception {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "start-burnout");
        msg.put("userId", "user123");

        handler.handleTextMessage(session, new TextMessage(msg.toString()));

        verify(burnoutController).handleMessage(eq(session), any());
    }

    @Test
    void handleTextMessage_registersPendingUploadContextForAudio() throws Exception {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "audio_upload");
        msg.put("sessionId", "sess-audio");
        msg.put("questionId", "q1");

        handler.handleTextMessage(session, new TextMessage(msg.toString()));

        // now simulate binary upload
        BinaryMessage binaryMessage = new BinaryMessage(new byte[]{0x00});
        handler.handleBinaryMessage(session, binaryMessage);

        verify(burnoutController).handleAudioMessage(eq(session), eq("sess-audio"), eq("q1"), eq(binaryMessage));
    }

    @Test
    void handleTextMessage_registersPendingUploadContextForVideo() throws Exception {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "video_upload");
        msg.put("sessionId", "sess-video");
        msg.put("questionId", "q2");

        handler.handleTextMessage(session, new TextMessage(msg.toString()));

        BinaryMessage binaryMessage = new BinaryMessage(new byte[]{0x01});
        handler.handleBinaryMessage(session, binaryMessage);

        verify(burnoutController).handleVideoMessage(eq(session), eq("sess-video"), eq("q2"), eq(binaryMessage));
    }

    @Test
    void handleBinaryMessage_withoutPendingContext_logsError() {
        // Not asserting logs here, just making sure no exceptions thrown
        BinaryMessage binaryMessage = new BinaryMessage(new byte[]{0x02});
        handler.handleBinaryMessage(session, binaryMessage);
        // No controller method should be called
        verifyNoInteractions(burnoutController);
    }

    // Optional: test logging only, no assertions needed
    @Test
    void afterConnectionEstablished_logsConnection() {
        handler.afterConnectionEstablished(session);
    }

    @Test
    void afterConnectionClosed_logsDisconnection() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
