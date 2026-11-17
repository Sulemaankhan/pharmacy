package com.pharmacy.cloudgateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private Policy defaultPolicy = Policy.of(60, 30, Duration.ofSeconds(1));
    private Policy authPolicy = Policy.of(10, 10, Duration.ofMinutes(1));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(Policy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Policy getAuthPolicy() {
        return authPolicy;
    }

    public void setAuthPolicy(Policy authPolicy) {
        this.authPolicy = authPolicy;
    }

    public static class Policy {
        private int capacity = 60;
        private int refillTokens = 30;
        private Duration refillPeriod = Duration.ofSeconds(1);

        public static Policy of(int capacity, int refillTokens, Duration refillPeriod) {
            Policy policy = new Policy();
            policy.capacity = capacity;
            policy.refillTokens = refillTokens;
            policy.refillPeriod = refillPeriod;
            return policy;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }

        double refillPerNanos() {
            long nanos = refillPeriod == null ? 0 : refillPeriod.toNanos();
            if (nanos <= 0 || refillTokens <= 0) {
                return 0;
            }
            return (double) refillTokens / nanos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Policy policy)) {
                return false;
            }
            return capacity == policy.capacity
                    && refillTokens == policy.refillTokens
                    && Objects.equals(refillPeriod, policy.refillPeriod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(capacity, refillTokens, refillPeriod);
        }
    }
}
