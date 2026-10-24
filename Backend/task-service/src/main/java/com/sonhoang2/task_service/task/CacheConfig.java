package com.sonhoang2.task_service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.task_service.task.dto.TaskDistributionResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Spring caches live in Redis so every task-service replica shares them.
 * Keys: task-service:taskDistribution::{userId}
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("task-service:")
                .entryTtl(Duration.ofMinutes(1))
                .disableCachingNullValues();

        // One concrete type per cache: plain JSON in Redis, no class names embedded in the payload.
        RedisCacheConfiguration taskDistribution = defaults.serializeValuesWith(SerializationPair.fromSerializer(
                new Jackson2JsonRedisSerializer<>(objectMapper, TaskDistributionResponse.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration("taskDistribution", taskDistribution)
                .build();
    }

    // Redis down must not fail requests: log the cache error and fall through to the database.
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
