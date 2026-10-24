package com.sonhoang2.userservice.auth;

import com.sonhoang2.common.exception.RateLimiterUnavailableException;
import com.sonhoang2.common.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private static final long RETRY_AFTER_SECONDS = 5;

    private final RateLimitService rateLimitService;
    private final StringRedisTemplate redisTemplate;

    // Fails closed: if Redis is down, login and signup are refused (503) rather than left open to brute force.
    public long checkRateLimit(String identifier) {
        String blockKey = "blocked:auth:" + identifier;
        try {
            Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);

            if (ttl != null && ttl > 0) {
                return ttl;
            }

            boolean consumed = rateLimitService.tryConsume(
                    "auth:" + identifier, 5, Duration.ofMinutes(1)
            );

            if (!consumed) {
                redisTemplate.opsForValue().set(blockKey, "1", Duration.ofMinutes(15));
                return 15 * 60;
            }
            return 0;
        } catch (DataAccessException e) {
            log.warn("Auth rate limit check failed for {}, refusing the request: {}", identifier, e.getMessage());
            throw new RateLimiterUnavailableException(
                    "Service temporarily unavailable, please retry shortly", RETRY_AFTER_SECONDS, e);
        }
    }
}
