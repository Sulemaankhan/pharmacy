package com.pharmacy.cloudgateway.throttle;

import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class PermitPool {
    private final int maxPermits;
    private final AtomicInteger available;

    PermitPool(int maxPermits) {
        this.maxPermits = Math.max(1, maxPermits);
        this.available = new AtomicInteger(this.maxPermits);
    }

    int limit() {
        return maxPermits;
    }

    int remaining() {
        return Math.max(0, available.get());
    }

    Mono<Boolean> acquire(Duration timeout) {
        if (tryAcquire()) {
            return Mono.just(true);
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return Mono.just(false);
        }
        return retry(System.nanoTime() + timeout.toNanos());
    }

    void release() {
        available.updateAndGet(current -> Math.min(maxPermits, current + 1));
    }

    private Mono<Boolean> retry(long deadlineNanos) {
        return Mono.defer(() -> {
            if (tryAcquire()) {
                return Mono.just(true);
            }
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return Mono.just(false);
            }
            long delayMs = Math.min(25, Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
            return Mono.delay(Duration.ofMillis(delayMs)).then(retry(deadlineNanos));
        });
    }

    private boolean tryAcquire() {
        while (true) {
            int current = available.get();
            if (current <= 0) {
                return false;
            }
            if (available.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }
}
