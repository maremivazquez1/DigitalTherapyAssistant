package harvard.capstone.digitaltherapy.config;

/**
 * Configuration class for enabling and registering WebSocket handlers in the application.
 * Registers the BedrockWebSocketHandler at the endpoint "/ws/bedrock" to handle incoming WebSocket connections.
 */
import harvard.capstone.digitaltherapy.websocket.CBTWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import harvard.capstone.digitaltherapy.websocket.BedrockWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BedrockWebSocketHandler bedrockWebSocketHandler;
    private final CBTWebSocketHandler cbtWebSocketHandler;

    public WebSocketConfig(BedrockWebSocketHandler bedrockWebSocketHandler,
                           CBTWebSocketHandler cbtWebSocketHandler) {
        this.bedrockWebSocketHandler = bedrockWebSocketHandler;
        this.cbtWebSocketHandler = cbtWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(bedrockWebSocketHandler, "/ws/bedrock")
                .setAllowedOrigins("*");

        registry.addHandler(cbtWebSocketHandler, "/ws/cbt")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB
        container.setMaxSessionIdleTimeout(15 * 60 * 1000L); // 15 minutes
        return container;
    }
}
