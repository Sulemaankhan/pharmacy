package com.pharmacy.shipment.realtime;

import com.pharmacy.shipment.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Component
public class ShipmentHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ShipmentHandshakeInterceptor.class);
    private final JwtService jwt;

    public ShipmentHandshakeInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected missing token");
            return false;
        }
        try {
            String email = jwt.extractEmail(token);
            attributes.put("email", email);
            String tracking = servletRequest.getServletRequest().getParameter("trackingNumber");
            if (tracking != null && !tracking.isBlank()) {
                attributes.put("trackingNumber", tracking.trim());
            }
            String userId = servletRequest.getServletRequest().getParameter("userId");
            if (userId != null && !userId.isBlank()) {
                attributes.put("userId", Long.parseLong(userId.trim()));
            }
            return true;
        } catch (Exception ex) {
            log.warn("WebSocket handshake rejected invalid token");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
