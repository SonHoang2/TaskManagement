package com.sonhoang2.common.ratelimit;

import com.sonhoang2.common.exception.RateLimiterUnavailableException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RateLimitServiceTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final AtomicInteger connectAttempts = new AtomicInteger();

    private Supplier<ProxyManager<byte[]>> failingConnect() {
        return () -> {
            connectAttempts.incrementAndGet();
            throw new IllegalStateException("connection refused");
        };
    }

    // Stands in for a connected proxy manager whose Redis commands fail.
    private static ProxyManager<byte[]> failingCommands() {
        return mock(ProxyManager.class, invocation -> {
            throw new IllegalStateException("command failed");
        });
    }

    private static void consume(RateLimitService service) {
        service.tryConsume("k", 1, Duration.ofMinutes(1));
    }

    @Test
    void doesNotConnectUntilFirstUse() {
        new RateLimitService(failingConnect(), now::get);

        assertThat(connectAttempts).hasValue(0);
    }

    @Test
    void refusesWithRetryAfterWhenConnectFails() {
        RateLimitService service = new RateLimitService(failingConnect(), now::get);

        assertThatThrownBy(() -> consume(service))
                .isInstanceOfSatisfying(RateLimiterUnavailableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(RateLimitService.RETRY_BACKOFF.toSeconds()))
                .hasRootCauseMessage("connection refused");
    }

    @Test
    void failsFastDuringBackoffAfterAFailedConnect() {
        RateLimitService service = new RateLimitService(failingConnect(), now::get);

        assertThatThrownBy(() -> consume(service)).isInstanceOf(RateLimiterUnavailableException.class);
        now.addAndGet(RateLimitService.RETRY_BACKOFF.toNanos() - 1);
        assertThatThrownBy(() -> consume(service)).isInstanceOf(RateLimiterUnavailableException.class);

        assertThat(connectAttempts).hasValue(1);
    }

    @Test
    void refusesWhenRedisCommandsFailAfterConnecting() {
        RateLimitService service = new RateLimitService(RateLimitServiceTest::failingCommands, now::get);

        assertThatThrownBy(() -> consume(service))
                .isInstanceOf(RateLimiterUnavailableException.class)
                .hasRootCauseMessage("command failed");
    }

    @Test
    void retriesConnectAfterBackoffAndKeepsTheConnectedProxyManager() {
        ProxyManager<byte[]> connected = failingCommands();
        Supplier<ProxyManager<byte[]>> connectOnSecondAttempt = () -> {
            if (connectAttempts.incrementAndGet() == 1) {
                throw new IllegalStateException("connection refused");
            }
            return connected;
        };
        RateLimitService service = new RateLimitService(connectOnSecondAttempt, now::get);

        assertThatThrownBy(() -> consume(service)).hasRootCauseMessage("connection refused");
        now.addAndGet(RateLimitService.RETRY_BACKOFF.toNanos());
        // Reaching the proxy manager's commands proves the second attempt connected; it is not rebuilt afterwards.
        assertThatThrownBy(() -> consume(service)).hasRootCauseMessage("command failed");
        assertThatThrownBy(() -> consume(service)).hasRootCauseMessage("command failed");

        assertThat(connectAttempts).hasValue(2);
    }
}
