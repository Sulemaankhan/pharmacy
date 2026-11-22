package com.pharmacy.cloudgateway.loadbalancer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.load-balancer")
public class LoadBalancerProperties {
    private int defaultMaxLoad = 50;
    private Map<String, Integer> services = new LinkedHashMap<>();

    public int getDefaultMaxLoad() {
        return defaultMaxLoad;
    }

    public void setDefaultMaxLoad(int defaultMaxLoad) {
        this.defaultMaxLoad = defaultMaxLoad;
    }

    public Map<String, Integer> getServices() {
        return services;
    }

    public void setServices(Map<String, Integer> services) {
        this.services = services == null ? new LinkedHashMap<>() : services;
    }
}
