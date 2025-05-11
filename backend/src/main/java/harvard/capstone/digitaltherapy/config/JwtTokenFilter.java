package harvard.capstone.digitaltherapy.config;

import harvard.capstone.digitaltherapy.authentication.service.JwtTokenProvider;
import harvard.capstone.digitaltherapy.authentication.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

// Token filter checks HTTP requests before going to normal authorization checks
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Autowired
    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider, TokenService tokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        // case if using websocket
        if (token == null && "websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            String query = request.getQueryString();
            if (query != null && query.contains("token=")) {
                String[] parts = query.split("token=");
                if (parts.length > 1) {
                    token = parts[1];
                    // remove extra stuff after '&'
                    if (token.contains("&")) {
                        token = token.substring(0, token.indexOf("&"));
                    }
                }
            }
        }
        // typical HTTP request
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        
        if (token != null) {
            try {
                // First validate the token
                if (!jwtTokenProvider.validateToken(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token has expired");
                    return;
                }

                String username = jwtTokenProvider.getUsername(token);
                
                // Check if token exists in Redis
                String storedToken = tokenService.getToken(username);
                if (storedToken == null || !storedToken.equals(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }
                
                // Check if token needs refresh
                if (tokenService.isTokenExpiring(username)) {
                    String newToken = tokenService.refreshToken(username);
                    if (newToken != null) {
                        response.setHeader("X-New-Token", newToken);
                    }
                }

                // Set up Spring Security context
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
