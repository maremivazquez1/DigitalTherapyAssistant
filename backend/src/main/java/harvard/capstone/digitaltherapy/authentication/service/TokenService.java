package harvard.capstone.digitaltherapy.authentication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Stores the token with an associated username and TTL.
     *
     * @param token the JWT token
     * @param username the associated username
     * @param expirationInSeconds expiration time in seconds
     */
    public void storeToken(String token, String username, long expirationInSeconds) {
        redisTemplate.opsForValue().set(token, username, expirationInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Checks whether a token exists in Redis.
     *
     * @param token the JWT token to check
     * @return true if the token exists, false otherwise
     */
    public boolean isTokenValid(String token) {
        Boolean exists = redisTemplate.hasKey(token);
        return exists != null && exists;
    }

    /**
     * Deletes a token from Redis.
     *
     * @param token the JWT token to delete
     */
    public void deleteToken(String token) {
        redisTemplate.delete(token);
    }
}
