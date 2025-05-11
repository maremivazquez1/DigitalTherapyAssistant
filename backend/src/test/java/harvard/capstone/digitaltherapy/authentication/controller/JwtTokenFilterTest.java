package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.service.JwtTokenProvider;
import harvard.capstone.digitaltherapy.authentication.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.PrintWriter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import harvard.capstone.digitaltherapy.config.JwtTokenFilter;

class JwtTokenFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenService tokenService;
    private JwtTokenFilter jwtTokenFilter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tokenService = mock(TokenService.class);
        jwtTokenFilter = new JwtTokenFilter(jwtTokenProvider, tokenService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_withValidToken_noRefresh() throws Exception {
        String token = "validtoken";
        String authHeader = "Bearer " + token;
        String username = "user";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        when(tokenService.getToken(username)).thenReturn(token);
        when(tokenService.isTokenExpiring(username)).thenReturn(false);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Verify filter chain is executed
        verify(filterChain, times(1)).doFilter(request, response);
        // Verify security context is set
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        // Verify no refresh header is set
        verify(response, never()).setHeader(eq("X-New-Token"), anyString());
    }

    @Test
    void testDoFilterInternal_withExpiredToken() throws Exception {
        String token = "expiredtoken";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(writer, times(1)).write("Token has expired");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withInvalidToken() throws Exception {
        String token = "validtoken";
        String authHeader = "Bearer " + token;
        String username = "user";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        // Return a different token than provided or null
        when(tokenService.getToken(username)).thenReturn(null);

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(writer, times(1)).write("Invalid token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withTokenRefresh() throws Exception {
        String token = "validtoken";
        String authHeader = "Bearer " + token;
        String username = "user";
        String newToken = "newToken";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        when(tokenService.getToken(username)).thenReturn(token);
        when(tokenService.isTokenExpiring(username)).thenReturn(true);
        when(tokenService.refreshToken(username)).thenReturn(newToken);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setHeader("X-New-Token", newToken);
        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void testDoFilterInternal_withWebsocketTokenExtraction() throws Exception {
        String tokenFromQuery = "wsToken";
        String queryString = "param1=value1&token=" + tokenFromQuery + "&param2=value2";
        // Simulate no Authorization header and upgrade is websocket.
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("Upgrade")).thenReturn("websocket");
        when(request.getQueryString()).thenReturn(queryString);
        // As token from query does not start with "Bearer ", it is used as is.
        when(jwtTokenProvider.validateToken(tokenFromQuery)).thenReturn(true);
        when(jwtTokenProvider.getUsername(tokenFromQuery)).thenReturn("wsUser");
        when(tokenService.getToken("wsUser")).thenReturn(tokenFromQuery);
        when(tokenService.isTokenExpiring("wsUser")).thenReturn(false);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("wsUser", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}