package harvard.capstone.digitaltherapy.authentication.service;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        // Extract the query parameters from the request URI
        URI uri = request.getURI();
        String query = uri.getQuery(); // e.g., "token=abc123"
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    String token = keyValue[1];
                    if (jwtTokenProvider.validateToken(token)) {
                        // If token is valid, extract the username and store it in handshake attributes
                        String username = jwtTokenProvider.getUsername(token);
                        attributes.put("username", username);
                        return true;
                    }
                }
            }
        }
        // 401 tokens that are missing or rejected
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Nothing needed afterwards
    }
}
