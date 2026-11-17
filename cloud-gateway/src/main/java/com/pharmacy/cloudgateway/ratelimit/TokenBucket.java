package com.pharmacy.cloudgateway.ratelimit;

import java.util.concurrent.TimeUnit;

public final class TokenBucket {
    private final int capacity;
    private final double refillPerNanos;
    private double tokens;
    private long lastRefillNanos;

    TokenBucket(RateLimitProperties.Policy policy) {
        this.capacity = Math.max(1, policy.getCapacity());
        this.refillPerNanos = policy.refillPerNanos();
        this.tokens = this.capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized Snapshot tryConsume() {
        refill();
        if (tokens >= 1.0d) {
            tokens -= 1.0d;
            return snapshot(true);
        }
        return snapshot(false);
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed > 0 && refillPerNanos > 0) {
            tokens = Math.min(capacity, tokens + (elapsed * refillPerNanos));
            lastRefillNanos = now;
        }
    }

    private Snapshot snapshot(boolean allowed) {
        int remaining = (int) Math.floor(tokens);
        long retryAfterSeconds = 0;
        if (!allowed) {
            if (refillPerNanos <= 0) {
                retryAfterSeconds = 60;
            } else {
                double waitNanos = (1.0d - tokens) / refillPerNanos;
                retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds((long) Math.ceil(waitNanos)));
            }
        }
        return new Snapshot(allowed, remaining, capacity, retryAfterSeconds);
    }

    public record Snapshot(boolean allowed, int remaining, int limit, long retryAfterSeconds) {
    }
}
