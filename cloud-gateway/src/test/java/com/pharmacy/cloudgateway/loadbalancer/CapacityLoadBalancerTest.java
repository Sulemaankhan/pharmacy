package com.pharmacy.cloudgateway.loadbalancer;


import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;

class CapacityLoadBalancerTest {
    @Test
    void sendsTrafficToTheInstanceWithTheMostRemainingCapacity() {
        ServiceInstance small = instance("small", 8081, 2);
        ServiceInstance large = instance("large", 8082, 5);
        CapacityLoadBalancer balancer = balancer(small, large);

        Response<ServiceInstance> chosen = balancer.choose(request()).block();

        assertThat(chosen.hasServer()).isTrue();
        assertThat(chosen.getServer().getInstanceId()).isEqualTo("large");
    }

    @Test
    void stopsUsingAnInstanceThatReachedItsMaximum() {
        ServiceInstance first = instance("first", 8081, 1);
        ServiceInstance second = instance("second", 8082, 1);
        CapacityLoadBalancer balancer = balancer(first, second);

        String firstChoice = balancer.choose(request()).block().getServer().getInstanceId();
        String secondChoice = balancer.choose(request()).block().getServer().getInstanceId();

        assertThat(List.of(firstChoice, secondChoice)).containsExactlyInAnyOrder("first", "second");
        assertThat(balancer.choose(request()).block().hasServer()).isFalse();
    }

    @Test
    void releaseMakesCapacityAvailableAgain() {
        ServiceInstance only = instance("only", 8088, 1);
        InstanceLoadTracker tracker = new InstanceLoadTracker();
        CapacityLoadBalancer balancer = balancer(tracker, only);

        assertThat(balancer.choose(request()).block().hasServer()).isTrue();
        assertThat(balancer.choose(request()).block().hasServer()).isFalse();

        tracker.release(only);

        assertThat(balancer.choose(request()).block().getServer().getInstanceId()).isEqualTo("only");
    }

    private static CapacityLoadBalancer balancer(ServiceInstance... instances) {
        return balancer(new InstanceLoadTracker(), instances);
    }

    private static CapacityLoadBalancer balancer(InstanceLoadTracker tracker, ServiceInstance... instances) {
        ServiceInstanceListSupplier supplier = new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "inventory-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(instances));
            }
        };
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("supplier", supplier);
        ObjectProvider<ServiceInstanceListSupplier> provider = beans.getBeanProvider(ServiceInstanceListSupplier.class);
        LoadBalancerProperties properties = new LoadBalancerProperties();
        properties.getServices().put("inventory-service", 120);
        return new CapacityLoadBalancer(provider, "inventory-service", tracker, properties, new SimpleMeterRegistry());
    }

    private static ServiceInstance instance(String id, int port, int maxLoad) {
        return new DefaultServiceInstance(id, "inventory-service", "localhost", port, false,
                Map.of("max-load", Integer.toString(maxLoad)));
    }

    private static Request<Object> request() {
        return new DefaultRequest<>();
    }
}
