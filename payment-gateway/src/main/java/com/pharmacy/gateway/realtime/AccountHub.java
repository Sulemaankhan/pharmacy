package com.pharmacy.gateway.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.gateway.dto.AccountSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AccountHub {
    private static final Logger log = LoggerFactory.getLogger(AccountHub.class);
    private final ObjectMapper mapper;
    private final Map<String, Set<WebSocketSession>> byEmail = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> byUser = new ConcurrentHashMap<>();

    public AccountHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(WebSocketSession session) {
        String email = stringAttr(session, "email");
        if (email != null) {
            byEmail.computeIfAbsent(email.toLowerCase(), key -> new CopyOnWriteArraySet<>()).add(session);
        }
        Long userId = longAttr(session, "userId");
        if (userId != null) {
            byUser.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
        }
        log.info("Account socket registered session={} email={} userId={}", session.getId(), email, userId);
    }

    public void unregister(WebSocketSession session) {
        byEmail.values().forEach(set -> set.remove(session));
        byUser.values().forEach(set -> set.remove(session));
    }

    public void broadcast(AccountSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        try {
            TextMessage message = new TextMessage(mapper.writeValueAsString(snapshot));
            if (snapshot.email() != null) {
                sendAll(byEmail.get(snapshot.email().toLowerCase()), message);
            }
            if (snapshot.userId() != null) {
                sendAll(byUser.get(snapshot.userId()), message);
            }
        } catch (Exception ex) {
            log.warn("Account socket broadcast failed email={}", snapshot.email(), ex);
        }
    }

    private void sendAll(Set<WebSocketSession> sessions, TextMessage message) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (Exception ex) {
                log.warn("Account socket push failed session={}", session.getId(), ex);
            }
        }
    }

    private static String stringAttr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long longAttr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
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
