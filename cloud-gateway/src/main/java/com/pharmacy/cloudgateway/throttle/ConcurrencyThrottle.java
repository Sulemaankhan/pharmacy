package com.pharmacy.cloudgateway.throttle;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ConcurrencyThrottle {
    private static final long IDLE_TTL_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final int EVICT_THRESHOLD = 10_000;

    private final ThrottleProperties properties;
    private final PermitPool global;
    private final ConcurrentHashMap<String, ClientPool> clients = new ConcurrentHashMap<>();

    public ConcurrencyThrottle(ThrottleProperties properties) {
        this.properties = properties;
        this.global = new PermitPool(properties.getMaxConcurrentGlobal());
    }

    public int clientLimit(String key) {
        return client(key).limit();
    }

    public int clientRemaining(String key) {
        return client(key).remaining();
    }

    private PermitPool client(String key) {
        maybeEvict();
        return clients.compute(key, (ignored, existing) -> {
            int limit = Math.max(1, properties.getMaxConcurrentPerClient());
            if (existing != null && existing.pool.limit() == limit) {
                existing.lastAccessNanos = System.nanoTime();
                return existing;
            }
            return new ClientPool(new PermitPool(limit));
        }).pool;
    }

    private Duration maxWait() {
        Duration wait = properties.getMaxWait();
        return wait == null ? Duration.ZERO : wait;
    }

    public Mono<Boolean> acquire(String clientKey) {
        Duration wait = maxWait();
        PermitPool clientPool = client(clientKey);
        return global.acquire(wait).flatMap(globalAcquired -> {
            if (!globalAcquired) {
                return Mono.just(false);
            }
            return clientPool.acquire(wait).doOnNext(clientAcquired -> {
                if (!clientAcquired) {
                    global.release();
                }
            });
        });
    }

    public void release(String clientKey) {
        ClientPool holder = clients.get(clientKey);
        if (holder != null) {
            holder.pool.release();
        }
        global.release();
    }

    private void maybeEvict() {
        if (clients.size() < EVICT_THRESHOLD) {
            return;
        }
        long cutoff = System.nanoTime() - IDLE_TTL_NANOS;
        clients.entrySet().removeIf(entry -> entry.getValue().pool.remaining() == entry.getValue().pool.limit()
                && entry.getValue().lastAccessNanos < cutoff);
    }

    private static final class ClientPool {
        private final PermitPool pool;
        private volatile long lastAccessNanos = System.nanoTime();

        private ClientPool(PermitPool pool) {
            this.pool = pool;
        }
    }
}
