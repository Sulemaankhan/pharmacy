package com.pharmacy.gateway.loadbalancer;

import com.pharmacy.cloudgateway.loadbalancer.CapacityLoadBalancer;
import com.pharmacy.cloudgateway.loadbalancer.InstanceLoadTracker;
import com.pharmacy.cloudgateway.loadbalancer.LoadBalancerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CapacityLoadBalancerConfiguration {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory factory,
            InstanceLoadTracker tracker,
            LoadBalancerProperties properties,
            MeterRegistry meters) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new CapacityLoadBalancer(
                factory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name,
                tracker,
                properties,
                meters);
    }
}
