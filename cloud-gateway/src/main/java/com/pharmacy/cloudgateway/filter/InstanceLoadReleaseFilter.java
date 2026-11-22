package com.pharmacy.cloudgateway.filter;

import com.pharmacy.cloudgateway.loadbalancer.InstanceLoadTracker;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InstanceLoadReleaseFilter implements GlobalFilter, Ordered {
    private final InstanceLoadTracker tracker;

    public InstanceLoadReleaseFilter(InstanceLoadTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).doFinally(signal -> {
            ServiceInstance instance = selected(exchange);
            if (instance != null) {
                tracker.release(instance);
            }
        });
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }

    private static ServiceInstance selected(ServerWebExchange exchange) {
        Object attribute = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);
        if (!(attribute instanceof Response<?> response) || !response.hasServer()) {
            return null;
        }
        Object server = response.getServer();
        return server instanceof ServiceInstance instance ? instance : null;
    }
}
