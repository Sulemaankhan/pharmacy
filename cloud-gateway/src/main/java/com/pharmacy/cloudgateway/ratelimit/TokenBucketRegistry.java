package com.pharmacy.cloudgateway.ratelimit;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class TokenBucketRegistry {
    private static final long IDLE_TTL_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final int EVICT_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Holder> buckets = new ConcurrentHashMap<>();

    public TokenBucket.Snapshot consume(String key, RateLimitProperties.Policy policy) {
        maybeEvict();
        Holder holder = buckets.compute(key, (ignored, existing) -> {
            if (existing != null && existing.policy.equals(policy)) {
                existing.lastAccessNanos = System.nanoTime();
                return existing;
            }
            return new Holder(new TokenBucket(policy), policy);
        });
        return holder.bucket.tryConsume();
    }

    private void maybeEvict() {
        if (buckets.size() < EVICT_THRESHOLD) {
            return;
        }
        long cutoff = System.nanoTime() - IDLE_TTL_NANOS;
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessNanos < cutoff);
    }

    private static final class Holder {
        private final TokenBucket bucket;
        private final RateLimitProperties.Policy policy;
        private volatile long lastAccessNanos = System.nanoTime();

        private Holder(TokenBucket bucket, RateLimitProperties.Policy policy) {
            this.bucket = bucket;
            this.policy = policy;
        }
    }
}
