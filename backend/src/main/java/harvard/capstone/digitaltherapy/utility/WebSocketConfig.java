package harvard.capstone.digitaltherapy.utility;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

 /*   @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // Disable CSRF for WebSocket connections
                .authorizeRequests()
                .antMatchers("/ws/**").permitAll() // Allow access to WebSocket endpoint
                .anyRequest().authenticated();
    }*/
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry the
        // messages back to the client on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");
        // Set application destination prefix (for example, "/app" will be used by the controller to send messages)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint, enabling the WebSocket handshake
        registry.addEndpoint("/ws").withSockJS(); // withSockJS for fallback options
    }
}


