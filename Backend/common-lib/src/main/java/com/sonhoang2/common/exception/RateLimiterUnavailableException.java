package com.sonhoang2.common.exception;

/**
 * The rate limit could not be checked (Redis unreachable or failing). Requests are refused rather than let
 * through unlimited, so the caller gets 503 with a Retry-After hint.
 */
public class RateLimiterUnavailableException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimiterUnavailableException(String message, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
