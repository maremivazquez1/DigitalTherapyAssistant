package harvard.capstone.digitaltherapy.authentication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Stores the token with an associated username and TTL.
     *
     * @param token the JWT token
     * @param username the associated username
     * @param expirationInMinutes expiration time in minutes
     */
    public void storeToken(String token, String username, long expirationInMinutes) {
        String key = TOKEN_PREFIX + username;
        redisTemplate.opsForValue().set(key, token, expirationInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Retrieves the token for a given username.
     *
     * @param username the associated username
     * @return the JWT token
     */
    public String getToken(String username) {
        String key = TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Deletes a token from Redis.
     *
     * @param username the associated username
     */
    public void deleteToken(String username) {
        String key = TOKEN_PREFIX + username;
        redisTemplate.delete(key);
    }

    /**
     * Refreshes the token for a given username.
     *
     * @param username the associated username
     * @return the refreshed JWT token
     */
    public String refreshToken(String username) {
        String currentToken = getToken(username);
        if (currentToken != null && jwtTokenProvider.validateToken(currentToken)) {
            // Create new token
            String newToken = jwtTokenProvider.createToken(username);
            // Store new token with expiration
            storeToken(newToken, username, JwtTokenProvider.TOKEN_EXPIRATION_MINUTES);
            return newToken;
        }
        return null;
    }

    /**
     * Checks whether a token is expiring within the refresh interval.
     *
     * @param username the associated username
     * @return true if the token is expiring within the refresh interval, false otherwise
     */
    public boolean isTokenExpiring(String username) {
        String key = TOKEN_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
        return ttl != null && ttl <= JwtTokenProvider.TOKEN_REFRESH_INTERVAL_MINUTES;
    }
}
