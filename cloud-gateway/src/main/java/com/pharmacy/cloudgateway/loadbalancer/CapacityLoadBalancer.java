package com.pharmacy.cloudgateway.loadbalancer;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

public class CapacityLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    private static final Logger log = LoggerFactory.getLogger(CapacityLoadBalancer.class);

    private final ObjectProvider<ServiceInstanceListSupplier> suppliers;
    private final String serviceId;
    private final InstanceLoadTracker tracker;
    private final LoadBalancerProperties properties;
    private final MeterRegistry meters;

    public CapacityLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> suppliers,
            String serviceId,
            InstanceLoadTracker tracker,
            LoadBalancerProperties properties,
            MeterRegistry meters) {
        this.suppliers = suppliers;
        this.serviceId = serviceId;
        this.tracker = tracker;
        this.properties = properties;
        this.meters = meters;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = suppliers.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }
        return supplier.get(request).next().map(instances -> {
            ServiceInstance chosen = tracker.tryAcquire(serviceId, instances, properties);
            if (chosen == null) {
                meters.counter("gateway.loadbalancer.saturated", "service", serviceId).increment();
                log.warn("Service {} has no instance under its maximum load", serviceId);
                return new EmptyResponse();
            }
            meters.counter("gateway.loadbalancer.selected", "service", serviceId).increment();
            log.debug("Selected {}:{} for {} active={}",
                    chosen.getHost(), chosen.getPort(), serviceId, tracker.active(chosen));
            return new DefaultResponse(chosen);
        });
    }
}
