package com.pharmacy.cloudgateway.throttle;


import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.throttle")
public class ThrottleProperties {
    private boolean enabled = true;
    private int maxConcurrentGlobal = 200;
    private int maxConcurrentPerClient = 16;
    private Duration maxWait = Duration.ofMillis(300);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxConcurrentGlobal() {
        return maxConcurrentGlobal;
    }

    public void setMaxConcurrentGlobal(int maxConcurrentGlobal) {
        this.maxConcurrentGlobal = maxConcurrentGlobal;
    }

    public int getMaxConcurrentPerClient() {
        return maxConcurrentPerClient;
    }

    public void setMaxConcurrentPerClient(int maxConcurrentPerClient) {
        this.maxConcurrentPerClient = maxConcurrentPerClient;
    }

    public Duration getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Duration maxWait) {
        this.maxWait = maxWait;
    }
}
