package harvard.capstone.digitaltherapy.config;

/**
 * Configuration class for enabling and registering WebSocket handlers in the application.
 * Registers the BedrockWebSocketHandler at the endpoint "/ws/bedrock" to handle incoming WebSocket connections.
 */
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import harvard.capstone.digitaltherapy.websocket.BedrockWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BedrockWebSocketHandler bedrockWebSocketHandler;

    public WebSocketConfig(BedrockWebSocketHandler bedrockWebSocketHandler) {
        this.bedrockWebSocketHandler = bedrockWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(bedrockWebSocketHandler, "/ws/bedrock")
                .setAllowedOrigins("*");
    }
}
