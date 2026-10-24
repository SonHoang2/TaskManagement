package com.sonhoang2.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.common.membership.MembershipCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * The shared {@link MembershipCache}. Active when the service has spring-boot-starter-data-redis;
 * turn off with taskmgmt.membership-cache.enabled=false.
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean({StringRedisTemplate.class, ObjectMapper.class})
@ConditionalOnProperty(prefix = "taskmgmt.membership-cache", name = "enabled", matchIfMissing = true)
public class MembershipCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MembershipCache membershipCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new MembershipCache(redisTemplate, objectMapper);
    }
}
