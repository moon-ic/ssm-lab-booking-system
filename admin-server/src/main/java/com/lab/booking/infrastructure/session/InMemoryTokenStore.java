package com.lab.booking.infrastructure.session;

import com.lab.booking.config.RedisModuleProperties;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTokenStore implements TokenStore {

    private final RedisModuleProperties properties;
    private final Map<String, SessionValue> sessions = new ConcurrentHashMap<>();

    public InMemoryTokenStore(RedisModuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public String createToken(Long userId) {
        String token = "demo-" + UUID.randomUUID();
        sessions.put(token, new SessionValue(userId, LocalDateTime.now().plus(properties.getTokenTtl())));
        return token;
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        SessionValue value = sessions.get(token);
        if (value == null) {
            return Optional.empty();
        }
        if (value.expireAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(value.userId());
    }

    @Override
    public String storageType() {
        return "MEMORY";
    }

    private record SessionValue(Long userId, LocalDateTime expireAt) {
    }
}
