package com.pharmacy.shipment.config;

import com.pharmacy.shipment.realtime.ShipmentHandshakeInterceptor;
import com.pharmacy.shipment.realtime.ShipmentSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ShipmentSocketHandler handler;
    private final ShipmentHandshakeInterceptor interceptor;

    public WebSocketConfig(ShipmentSocketHandler handler, ShipmentHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/shipments")
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns("http://localhost:5173", "http://localhost:5174");
    }
}
