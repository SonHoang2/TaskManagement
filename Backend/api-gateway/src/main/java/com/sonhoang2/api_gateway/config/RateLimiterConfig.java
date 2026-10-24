package com.sonhoang2.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    // Per client IP and route. Overridable (e.g. RATE_LIMIT_REPLENISH_RATE) so load tests from one machine are possible.
    @Bean
    public RedisRateLimiter redisRateLimiter(@Value("${rate-limit.replenish-rate:2}") int replenishRate,
                                             @Value("${rate-limit.burst-capacity:150}") int burstCapacity) {
        RedisRateLimiter limiter = new RedisRateLimiter(replenishRate, burstCapacity, 1);
        limiter.setIncludeHeaders(false);
        return limiter;
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}