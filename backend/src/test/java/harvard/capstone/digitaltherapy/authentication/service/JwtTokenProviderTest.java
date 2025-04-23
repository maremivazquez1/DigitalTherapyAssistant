package harvard.capstone.digitaltherapy.authentication.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import harvard.capstone.digitaltherapy.authentication.service.JwtTokenProvider;


public class JwtTokenProviderTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider();

    @Test
    public void testTokenCreationAndValidation() {
        String username = "testuser";
        String token = tokenProvider.createToken(username);
        
        assertNotNull(token, "Token should not be null");
        assertTrue(tokenProvider.validateToken(token), "Token should be valid");
        
        String extractedUsername = tokenProvider.getUsername(token);
        assertEquals(username, extractedUsername, "Extracted username should match the original");
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalid.token.value";
        assertFalse(tokenProvider.validateToken(invalidToken), "Invalid token should not be valid");
    }

    @Test
    public void testExpiredToken() {
        // Retrieve the same key used by the tokenProvider via reflection
        try {
            Field keyField = JwtTokenProvider.class.getDeclaredField("key");
            keyField.setAccessible(true);
            Key providerKey = (Key) keyField.get(tokenProvider);

            // Create an expired token using the provider's key.
            Date now = new Date();
            Date expiredDate = new Date(now.getTime() - 1000); // expired 1 second ago
            String expiredToken = Jwts.builder()
                    .setSubject("expiredUser")
                    .setIssuedAt(now)
                    .setExpiration(expiredDate)
                    .signWith(providerKey, SignatureAlgorithm.HS256)
                    .compact();

            assertFalse(tokenProvider.validateToken(expiredToken), "Expired token should not be valid");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access tokenProvider key via reflection: " + e.getMessage());
        }
    }
}
