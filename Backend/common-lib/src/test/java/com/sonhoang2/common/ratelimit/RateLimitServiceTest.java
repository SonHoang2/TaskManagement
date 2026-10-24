package com.sonhoang2.common.ratelimit;

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

    @Test
    void doesNotConnectUntilFirstUse() {
        new RateLimitService(failingConnect(), now::get);

        assertThat(connectAttempts).hasValue(0);
    }

    @Test
    void failsFastDuringBackoffAfterAFailedConnect() {
        RateLimitService service = new RateLimitService(failingConnect(), now::get);

        assertThatThrownBy(() -> service.tryConsume("k", 1, Duration.ofMinutes(1))).hasMessage("connection refused");
        now.addAndGet(RateLimitService.RETRY_BACKOFF.toNanos() - 1);
        assertThatThrownBy(() -> service.tryConsume("k", 1, Duration.ofMinutes(1)))
                .hasMessageContaining("Redis connection failed recently");

        assertThat(connectAttempts).hasValue(1);
    }

    @Test
    void retriesConnectAfterBackoffAndKeepsTheConnectedProxyManager() {
        ProxyManager<byte[]> connected = mock(ProxyManager.class, invocation -> {
            throw new UnsupportedOperationException("bucket built");
        });
        Supplier<ProxyManager<byte[]>> connectOnSecondAttempt = () -> {
            if (connectAttempts.incrementAndGet() == 1) {
                throw new IllegalStateException("connection refused");
            }
            return connected;
        };
        RateLimitService service = new RateLimitService(connectOnSecondAttempt, now::get);

        assertThatThrownBy(() -> service.tryConsume("k", 1, Duration.ofMinutes(1))).hasMessage("connection refused");
        now.addAndGet(RateLimitService.RETRY_BACKOFF.toNanos());
        // Reaching the proxy manager (the mock throws on builder()) proves the second attempt connected.
        assertThatThrownBy(() -> service.tryConsume("k", 1, Duration.ofMinutes(1))).hasMessage("bucket built");
        assertThatThrownBy(() -> service.tryConsume("k", 1, Duration.ofMinutes(1))).hasMessage("bucket built");

        assertThat(connectAttempts).hasValue(2);
    }
}
