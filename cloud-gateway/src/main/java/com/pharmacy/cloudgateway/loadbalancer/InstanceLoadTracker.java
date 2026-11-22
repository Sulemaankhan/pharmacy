package com.pharmacy.cloudgateway.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class InstanceLoadTracker {
    private final ConcurrentHashMap<String, AtomicInteger> active = new ConcurrentHashMap<>();

    public ServiceInstance tryAcquire(String serviceId, List<ServiceInstance> instances, LoadBalancerProperties properties) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        synchronized (this) {
            forgetIdleInstances(serviceId, instances);
            ServiceInstance best = null;
            int bestRemaining = 0;
            int bestMax = -1;
            int bestActive = Integer.MAX_VALUE;
            for (ServiceInstance instance : instances) {
                int max = capacity(instance, properties);
                int current = current(instance);
                int remaining = max - current;
                if (remaining <= 0) {
                    continue;
                }
                boolean better = best == null
                        || remaining > bestRemaining
                        || (remaining == bestRemaining && max > bestMax)
                        || (remaining == bestRemaining && max == bestMax && current < bestActive);
                if (better) {
                    best = instance;
                    bestRemaining = remaining;
                    bestMax = max;
                    bestActive = current;
                }
            }
            if (best == null) {
                return null;
            }
            active.computeIfAbsent(key(best), ignored -> new AtomicInteger()).incrementAndGet();
            return best;
        }
    }

    public void release(ServiceInstance instance) {
        if (instance == null) {
            return;
        }
        active.computeIfPresent(key(instance), (ignored, count) -> {
            count.updateAndGet(value -> Math.max(0, value - 1));
            return count;
        });
    }

    public int active(ServiceInstance instance) {
        AtomicInteger count = active.get(key(instance));
        return count == null ? 0 : count.get();
    }

    private void forgetIdleInstances(String serviceId, List<ServiceInstance> instances) {
        Set<String> live = instances.stream().map(this::key).collect(Collectors.toSet());
        String prefix = serviceId + "|";
        active.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix)
                && !live.contains(entry.getKey())
                && entry.getValue().get() == 0);
    }

    private int current(ServiceInstance instance) {
        AtomicInteger count = active.get(key(instance));
        return count == null ? 0 : count.get();
    }

    private int capacity(ServiceInstance instance, LoadBalancerProperties properties) {
        String advertised = instance.getMetadata() == null ? null : instance.getMetadata().get("max-load");
        if (advertised != null && !advertised.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(advertised.trim()));
            } catch (NumberFormatException ignored) {
                // fall through to the configured service limit
            }
        }
        Integer configured = properties.getServices().get(instance.getServiceId());
        if (configured != null && configured > 0) {
            return configured;
        }
        return Math.max(1, properties.getDefaultMaxLoad());
    }

    private String key(ServiceInstance instance) {
        return instance.getServiceId() + "|" + instance.getHost() + "|" + instance.getPort();
    }
}
