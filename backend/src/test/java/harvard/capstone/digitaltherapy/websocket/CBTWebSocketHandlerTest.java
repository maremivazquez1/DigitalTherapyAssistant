package harvard.capstone.digitaltherapy.websocket;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.cbt.controller.CBTController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.*;

import java.io.IOException;

class CBTWebSocketHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CBTController cbtController;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private CBTWebSocketHandler webSocketHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(session.getId()).thenReturn("session123");
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        webSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        // Ensure session is removed from active sessions (internal state validation can be added if needed)
    }

    @Test
    void testHandleTextMessage() throws IOException {
        String messagePayload = "{\"type\":\"text\",\"requestId\":\"123\"}";
        TextMessage textMessage = new TextMessage(messagePayload);

        when(objectMapper.readTree(messagePayload)).thenReturn(new ObjectMapper().readTree(messagePayload));

        webSocketHandler.handleTextMessage(session, textMessage);
        verify(cbtController, times(1)).handleMessage(eq(session), any(), eq("text"), eq("123"));
    }


    @Test
    void testHandleBinaryMessage() {
        BinaryMessage binaryMessage = new BinaryMessage(new byte[]{1, 2, 3});
        webSocketHandler.handleBinaryMessage(session, binaryMessage);
        verify(cbtController, times(1)).handleBinaryMessage(session, binaryMessage);
    }

}

