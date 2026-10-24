package com.sonhoang2.common.autoconfigure;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Tunes the Lettuce client that Spring Boot builds from spring.data.redis.* in every service that has
 * spring-boot-starter-data-redis.
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@ConditionalOnClass({RedisClient.class, LettuceClientConfigurationBuilderCustomizer.class})
public class RedisClientAutoConfiguration {

    // While Redis is down, fail commands at once (callers degrade) instead of queueing them for the timeout.
    // Spring Boot sets its client options (timeouts, SSL) before running customizers, so mutate them rather than replace.
    @Bean
    public LettuceClientConfigurationBuilderCustomizer rejectCommandsWhileDisconnected() {
        return builder -> {
            ClientOptions options = builder.build().getClientOptions().orElseGet(ClientOptions::create);
            builder.clientOptions(options.mutate()
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .build());
        };
    }
}
