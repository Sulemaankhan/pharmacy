package com.pharmacy.shipment.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.shipment.event.ShipmentUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ShipmentHub {
    private static final Logger log = LoggerFactory.getLogger(ShipmentHub.class);
    private final ObjectMapper mapper;
    private final Map<String, Set<WebSocketSession>> byTracking = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> byUser = new ConcurrentHashMap<>();

    public ShipmentHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(WebSocketSession session) {
        String tracking = attr(session, "trackingNumber");
        if (tracking != null && !tracking.isBlank()) {
            byTracking.computeIfAbsent(tracking, key -> new CopyOnWriteArraySet<>()).add(session);
        }
        Long userId = userId(session);
        if (userId != null) {
            byUser.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
        }
        log.info("WebSocket registered session={} tracking={} userId={}", session.getId(), tracking, userId);
    }

    public void unregister(WebSocketSession session) {
        byTracking.values().forEach(set -> set.remove(session));
        byUser.values().forEach(set -> set.remove(session));
    }

    public void broadcast(ShipmentUpdate update) {
        if (update == null) {
            return;
        }
        String payload;
        try {
            payload = mapper.writeValueAsString(update);
        } catch (IOException ex) {
            log.warn("Could not serialize shipment update tracking={}", update.trackingNumber(), ex);
            return;
        }
        TextMessage message = new TextMessage(payload);
        int sent = 0;
        if (update.trackingNumber() != null) {
            sent += sendAll(byTracking.get(update.trackingNumber()), message);
        }
        if (update.userId() != null) {
            sent += sendAll(byUser.get(update.userId()), message);
        }
        log.info("Shipment update pushed tracking={} status={} sockets={}",
                update.trackingNumber(), update.status(), sent);
    }

    private int sendAll(Set<WebSocketSession> sessions, TextMessage message) {
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
                sent++;
            } catch (Exception ex) {
                log.warn("WebSocket push failed session={}", session.getId(), ex);
            }
        }
        return sent;
    }

    private static String attr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        if (value instanceof Long number) {
            return number;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
