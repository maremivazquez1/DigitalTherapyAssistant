package harvard.capstone.digitaltherapy.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import harvard.capstone.digitaltherapy.service.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebSocketHandlerTest {

    private BedrockService bedrockService;
    private BedrockWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        bedrockService = mock(BedrockService.class);
        handler = new BedrockWebSocketHandler(bedrockService);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testHandleTextMessage_validInput_sendsResponse() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session1");

        String userInput = "I'm feeling anxious today.";
        when(bedrockService.processMessageWithHistory(any())).thenReturn("Let's talk about that.");

        handler.afterConnectionEstablished(session);

        String requestJson = objectMapper.writeValueAsString(Map.of("text", userInput, "requestId", "123"));
        handler.handleTextMessage(session, new TextMessage(requestJson));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String responsePayload = captor.getValue().getPayload();
        assertTrue(responsePayload.contains("Let's talk about that."));
        assertTrue(responsePayload.contains("\"requestId\":\"123\""));
    }

    @Test
    public void testHandleTextMessage_emptyText_sendsError() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session2");

        handler.afterConnectionEstablished(session);

        String requestJson = objectMapper.writeValueAsString(Map.of("text", "", "requestId", "456"));
        handler.handleTextMessage(session, new TextMessage(requestJson));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String responsePayload = captor.getValue().getPayload();
        assertTrue(responsePayload.contains("\"error\""));
        assertTrue(responsePayload.contains("cannot be empty"));
        assertTrue(responsePayload.contains("\"requestId\":\"456\""));
    }

    @Test
    public void testHandleTextMessage_malformedJson_sendsError() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session3");

        handler.afterConnectionEstablished(session);

        String malformedJson = "{\"text\":\"Hello\""; // Missing closing brace
        handler.handleTextMessage(session, new TextMessage(malformedJson));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String responsePayload = captor.getValue().getPayload();
        assertTrue(responsePayload.contains("\"error\""));
        System.out.println("Response payload: " + responsePayload);
        assertTrue(responsePayload.contains("Unexpected end-of-input"));
    }

    @Test
    public void testHandleTextMessage_missingTextField_sendsError() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session4");

        handler.afterConnectionEstablished(session);

        String requestJson = objectMapper.writeValueAsString(Map.of("requestId", "789"));
        handler.handleTextMessage(session, new TextMessage(requestJson));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String responsePayload = captor.getValue().getPayload();
        assertTrue(responsePayload.contains("\"error\""));
        System.out.println("Response payload: " + responsePayload);
        assertTrue(responsePayload.contains("Text/prompt cannot be empty"));
    }

    @Test
    public void testHandleTextMessage_serviceThrowsException_sendsError() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session5");

        when(bedrockService.processMessageWithHistory(any())).thenThrow(new RuntimeException("Service failure"));

        handler.afterConnectionEstablished(session);

        String requestJson = objectMapper.writeValueAsString(Map.of("text", "Trigger failure", "requestId", "999"));
        handler.handleTextMessage(session, new TextMessage(requestJson));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String responsePayload = captor.getValue().getPayload();
        assertTrue(responsePayload.contains("\"error\""));
        assertTrue(responsePayload.contains("\"requestId\":\"999\""));
    }
}
