package harvard.capstone.digitaltherapy.authentication.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtTokenProvider {

    // Automatically generate secret key for token
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Token expiration and refresh interval
    public static final long TOKEN_REFRESH_INTERVAL_MINUTES = 30;
    public static final long TOKEN_EXPIRATION_MINUTES = 60;
    public static final long TOKEN_EXPIRATION_MILLISECONDS = TOKEN_EXPIRATION_MINUTES * 60 * 1000;

    public String createToken(String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_EXPIRATION_MILLISECONDS);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    // Get username from token
    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
