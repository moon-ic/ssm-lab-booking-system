package com.lab.booking.infrastructure.session;

import com.lab.booking.config.RedisModuleProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;

public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisModuleProperties properties;

    public RedisTokenStore(StringRedisTemplate redisTemplate, RedisModuleProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public String createToken(Long userId) {
        String token = "demo-" + UUID.randomUUID();
        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(userId), properties.getTokenTtl());
        return token;
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        String value = redisTemplate.opsForValue().get(tokenKey(token));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }

    @Override
    public String storageType() {
        return "REDIS";
    }

    private String tokenKey(String token) {
        return properties.getKeyPrefix() + "auth:token:" + token;
    }
}
