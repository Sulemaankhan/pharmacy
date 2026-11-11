package com.pharmacy.gateway.config;

import com.pharmacy.gateway.realtime.AccountHandshakeInterceptor;
import com.pharmacy.gateway.realtime.AccountSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AccountSocketHandler handler;
    private final AccountHandshakeInterceptor interceptor;

    public WebSocketConfig(AccountSocketHandler handler, AccountHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/accounts")
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns("http://localhost:5173", "http://localhost:5174");
    }
}
