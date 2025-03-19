package harvard.capstone.digitaltherapy;

import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketHandler;

public class WebSocketTestClient {

    public static void main(String[] args) throws Exception {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.doHandshake(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                System.out.println("Connected to WebSocket");
                session.sendMessage(new TextMessage("Hello from WebSocket client"));
            }

            @Override
            public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
                System.out.println("Received: " + message.getPayload());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.out.println("Transport error: " + exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                System.out.println("Connection closed");
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, "ws://localhost:8080/ws").get();

        // Keep the connection open to test communication
        Thread.sleep(5000); // Adjust based on how long you need to test
        session.close();
    }
}

