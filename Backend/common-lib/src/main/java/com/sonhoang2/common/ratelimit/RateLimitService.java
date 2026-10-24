package com.sonhoang2.common.ratelimit;

import com.sonhoang2.common.exception.RateLimiterUnavailableException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Redis-backed token buckets. Bucket4j opens its Redis connection on first use rather than at startup,
 * so a service boots while Redis is down. Fails closed: while Redis is unreachable, {@link #tryConsume}
 * throws {@link RateLimiterUnavailableException} (503) instead of letting requests through unlimited.
 */
@Slf4j
public class RateLimitService {

    // After a failed connect, fail at once for this long instead of making every request wait for the connect timeout.
    static final Duration RETRY_BACKOFF = Duration.ofSeconds(5);

    private final Supplier<ProxyManager<byte[]>> proxyManagerFactory;
    private final LongSupplier nanoTime;
    private volatile ProxyManager<byte[]> proxyManager;
    private long retryAt;

    public RateLimitService(Supplier<ProxyManager<byte[]>> proxyManagerFactory) {
        this(proxyManagerFactory, System::nanoTime);
    }

    RateLimitService(Supplier<ProxyManager<byte[]>> proxyManagerFactory, LongSupplier nanoTime) {
        this.proxyManagerFactory = proxyManagerFactory;
        this.nanoTime = nanoTime;
    }

    public boolean tryConsume(String key, int points, Duration duration) {
        ProxyManager<byte[]> manager = proxyManager();
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            Bucket bucket = manager.builder().build(keyBytes, () ->
                    BucketConfiguration.builder()
                            .addLimit(Bandwidth.builder().capacity(points).refillGreedy(points, duration).build())
                            .build()
            );
            return bucket.tryConsume(1);
        } catch (RuntimeException e) {
            log.warn("Rate limit check failed for key {}, refusing the request: {}", key, e.getMessage());
            throw unavailable(e);
        }
    }

    // Once connected, Lettuce reconnects the same connection by itself, so the proxy manager is kept for good.
    private ProxyManager<byte[]> proxyManager() {
        ProxyManager<byte[]> current = proxyManager;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (proxyManager == null) {
                if (retryAt != 0 && nanoTime.getAsLong() - retryAt < 0) {
                    throw unavailable(null);
                }
                try {
                    proxyManager = proxyManagerFactory.get();
                } catch (RuntimeException e) {
                    retryAt = nanoTime.getAsLong() + RETRY_BACKOFF.toNanos();
                    log.warn("Rate limiter cannot connect to Redis, refusing requests for {}s: {}",
                            RETRY_BACKOFF.toSeconds(), e.getMessage());
                    throw unavailable(e);
                }
            }
            return proxyManager;
        }
    }

    private static RateLimiterUnavailableException unavailable(Throwable cause) {
        return new RateLimiterUnavailableException(
                "Service temporarily unavailable, please retry shortly", RETRY_BACKOFF.toSeconds(), cause);
    }
}
