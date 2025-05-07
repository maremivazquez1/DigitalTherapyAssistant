package harvard.capstone.digitaltherapy.config;

/**
 * Configuration class for enabling and registering WebSocket handlers in the application.
 * Registers the CBTWebSocketHandler at the endpoint "/ws/cbt" to handle incoming WebSocket connections.
 */
import harvard.capstone.digitaltherapy.websocket.CBTWebSocketHandler;
import harvard.capstone.digitaltherapy.websocket.BurnoutWebSocketHandler;
import harvard.capstone.digitaltherapy.authentication.service.JwtHandshakeInterceptor;
import harvard.capstone.digitaltherapy.authentication.service.JwtTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@ConditionalOnWebApplication
public class WebSocketConfig implements WebSocketConfigurer {

    private final CBTWebSocketHandler cbtWebSocketHandler;
    private final BurnoutWebSocketHandler burnoutWebSocketHandler;
    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketConfig(CBTWebSocketHandler cbtWebSocketHandler,
                           BurnoutWebSocketHandler burnoutWebSocketHandler,
                           JwtTokenProvider jwtTokenProvider) {
        this.cbtWebSocketHandler = cbtWebSocketHandler;
        this.burnoutWebSocketHandler = burnoutWebSocketHandler;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(cbtWebSocketHandler, "/ws/cbt")
                .addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider))
                .setAllowedOrigins("*");

        registry.addHandler(burnoutWebSocketHandler, "/ws/burnout")
                .addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider))
                .setAllowedOrigins("*");
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(2048 * 2048);
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 300); // 1MB
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L); // 15 minutes
        return container;
    }
}
