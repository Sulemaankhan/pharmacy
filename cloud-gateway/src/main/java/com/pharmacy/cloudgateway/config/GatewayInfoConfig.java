package com.pharmacy.cloudgateway.config;


import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class GatewayInfoConfig {
    @Bean
    public RouterFunction<ServerResponse> gatewayIndex(@Value("${server.port:8095}") int port) {
        return route(GET("/"), request -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "service", "cloud-gateway",
                        "status", "UP",
                        "mode", "reactive",
                        "entryPoint", "http://localhost:" + port)));
    }
}
