package com.sonhoang2.userservice.auth;

import com.sonhoang2.common.exception.RateLimiterUnavailableException;
import com.sonhoang2.common.ratelimit.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthRateLimitServiceTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final AuthRateLimitService service = new AuthRateLimitService(rateLimitService, redisTemplate);

    @Test
    void allowsWhenUnderTheLimit() {
        when(rateLimitService.tryConsume("auth:1.2.3.4", 5, Duration.ofMinutes(1))).thenReturn(true);

        assertThat(service.checkRateLimit("1.2.3.4")).isZero();
    }

    @Test
    void blocksForFifteenMinutesOnceTheLimitIsHit() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(rateLimitService.tryConsume(anyString(), anyInt(), any())).thenReturn(false);

        assertThat(service.checkRateLimit("1.2.3.4")).isEqualTo(15 * 60);
        verify(values).set("blocked:auth:1.2.3.4", "1", Duration.ofMinutes(15));
    }

    @Test
    void refusesWhenRedisIsDown() {
        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> service.checkRateLimit("1.2.3.4"))
                .isInstanceOf(RateLimiterUnavailableException.class)
                .hasCauseInstanceOf(RedisConnectionFailureException.class);
        verifyNoInteractions(rateLimitService);
    }
}
