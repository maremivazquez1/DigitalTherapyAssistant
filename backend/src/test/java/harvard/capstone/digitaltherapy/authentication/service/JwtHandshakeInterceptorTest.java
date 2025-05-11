package harvard.capstone.digitaltherapy.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

public class JwtHandshakeInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtHandshakeInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler webSocketHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        interceptor = new JwtHandshakeInterceptor(jwtTokenProvider);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        webSocketHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    @Test
    void testBeforeHandshake_WithValidToken() throws Exception {
        // Arrange
        String token = "validToken";
        URI uri = URI.create("ws://localhost?token=" + token);
        when(request.getURI()).thenReturn(uri);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsername(token)).thenReturn("testuser");

        // Act
        boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Assert
        assertTrue(result);
        assertEquals("testuser", attributes.get("username"));
    }

    @Test
    void testBeforeHandshake_MissingToken() throws Exception {
        // Arrange: URI with no query parameters.
        URI uri = URI.create("ws://localhost");
        when(request.getURI()).thenReturn(uri);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Assert
        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testBeforeHandshake_InvalidToken() throws Exception {
        // Arrange: Token provided but invalid.
        String token = "invalidToken";
        URI uri = URI.create("ws://localhost?token=" + token);
        when(request.getURI()).thenReturn(uri);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Assert
        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}