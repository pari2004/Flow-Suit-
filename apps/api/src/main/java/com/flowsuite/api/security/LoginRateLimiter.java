package com.flowsuite.api.security;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Redis-backed login rate limiter (FR-06). Counts failed attempts per email within a
 * sliding fixed window; once the threshold is exceeded, further attempts are rejected with
 * {@link ErrorCode#RATE_LIMITED} until the window elapses. A successful login clears the
 * counter.
 */
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "login:attempts:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimiter(StringRedisTemplate redis, SecurityProperties properties) {
        this.redis = redis;
        this.maxAttempts = properties.loginMaxAttempts();
        this.window = Duration.ofMinutes(properties.loginWindowMinutes());
    }

    /** Rejects the attempt if the email is currently locked out. Call before verifying the password. */
    public void assertNotLocked(String email) {
        String value = redis.opsForValue().get(key(email));
        if (value != null && Integer.parseInt(value) >= maxAttempts) {
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    "Too many failed login attempts. Please try again in a few minutes.");
        }
    }

    /** Records a failed attempt, (re)setting the window TTL on the first failure. */
    public void recordFailure(String email) {
        String redisKey = key(email);
        Long attempts = redis.opsForValue().increment(redisKey);
        if (attempts != null && attempts == 1L) {
            redis.expire(redisKey, window);
        }
    }

    /** Clears the counter after a successful login. */
    public void reset(String email) {
        redis.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email.toLowerCase(Locale.ROOT);
    }
}
