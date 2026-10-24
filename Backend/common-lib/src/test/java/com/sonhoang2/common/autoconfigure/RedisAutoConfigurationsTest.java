package com.sonhoang2.common.autoconfigure;

import com.sonhoang2.common.membership.MembershipCache;
import com.sonhoang2.common.ratelimit.RateLimitAspect;
import com.sonhoang2.common.ratelimit.RateLimitService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisAutoConfigurationsTest {

    private static final AutoConfigurations COMMON = AutoConfigurations.of(
            JacksonAutoConfiguration.class,
            RedisClientAutoConfiguration.class,
            RateLimitAutoConfiguration.class,
            MembershipCacheAutoConfiguration.class);

    // Nothing listens on port 1: the context must still start, since no bean may connect to Redis at startup.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("spring.data.redis.port=1", "spring.data.redis.connect-timeout=1s");

    @Test
    void registersRedisBeansWhenSpringDataRedisIsConfigured() {
        runner.withConfiguration(COMMON).withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RateLimitService.class);
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
                    assertThat(context).hasSingleBean(MembershipCache.class);
                });
    }

    @Test
    void rateLimitFailsOnUseNotAtStartupWhileRedisIsUnreachable() {
        runner.withConfiguration(COMMON).withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(RateLimitService.class)
                            .tryConsume("test", 1, Duration.ofMinutes(1)))
                            .isInstanceOf(RedisConnectionException.class);
                });
    }

    @Test
    void rejectsCommandsWhileDisconnectedAndKeepsSpringBootClientOptions() {
        runner.withConfiguration(COMMON).withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .run(context -> {
                    ClientOptions options = context.getBean(LettuceConnectionFactory.class)
                            .getClientConfiguration().getClientOptions().orElseThrow();
                    assertThat(options.getDisconnectedBehavior())
                            .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
                    assertThat(options.getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(1));
                });
    }

    @Test
    void registersNoRedisBeansWithoutSpringDataRedis() {
        runner.withConfiguration(COMMON).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RateLimitService.class);
            assertThat(context).doesNotHaveBean(RateLimitAspect.class);
            assertThat(context).doesNotHaveBean(MembershipCache.class);
        });
    }

    @Test
    void rateLimitCanBeTurnedOff() {
        runner.withConfiguration(COMMON).withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues("taskmgmt.rate-limit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RateLimitService.class);
                    assertThat(context).doesNotHaveBean(RateLimitAspect.class);
                    assertThat(context).hasSingleBean(MembershipCache.class);
                });
    }

    @Test
    void membershipCacheCanBeTurnedOff() {
        runner.withConfiguration(COMMON).withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues("taskmgmt.membership-cache.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MembershipCache.class);
                    assertThat(context).hasSingleBean(RateLimitService.class);
                });
    }
}
