package com.sonhoang2.common.autoconfigure;

import com.sonhoang2.common.ratelimit.RateLimitAspect;
import com.sonhoang2.common.ratelimit.RateLimitService;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Redis-backed rate limiting ({@link RateLimitService} and the {@code @RateLimit} aspect).
 * Active when the service has spring-boot-starter-data-redis; turn off with taskmgmt.rate-limit.enabled=false.
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass({LettuceConnectionFactory.class, LettuceBasedProxyManager.class})
@ConditionalOnBean(LettuceConnectionFactory.class)
@ConditionalOnProperty(prefix = "taskmgmt.rate-limit", name = "enabled", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimitService rateLimitService(LettuceConnectionFactory connectionFactory) {
        return new RateLimitService(() -> proxyManager(connectionFactory));
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimitService rateLimitService) {
        return new RateLimitAspect(rateLimitService);
    }

    // Reuses the client Spring Boot built from spring.data.redis.*; building connects, so RateLimitService calls it lazily.
    private static ProxyManager<byte[]> proxyManager(LettuceConnectionFactory connectionFactory) {
        AbstractRedisClient client = connectionFactory.getRequiredNativeClient();
        Bucket4jLettuce.LettuceBasedProxyManagerBuilder<byte[]> builder = switch (client) {
            case RedisClient standalone -> Bucket4jLettuce.casBasedBuilder(standalone);
            case RedisClusterClient cluster -> Bucket4jLettuce.casBasedBuilder(cluster);
            default -> throw new IllegalStateException("Unsupported Redis client: " + client.getClass().getName());
        };
        return builder
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }
}
