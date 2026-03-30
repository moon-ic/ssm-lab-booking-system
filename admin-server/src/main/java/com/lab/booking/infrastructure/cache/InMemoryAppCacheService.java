package com.lab.booking.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.config.RedisModuleProperties;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAppCacheService implements AppCacheService {

    private final ObjectMapper objectMapper;
    private final RedisModuleProperties properties;
    private final Map<String, CacheValue> cache = new ConcurrentHashMap<>();

    public InMemoryAppCacheService(ObjectMapper objectMapper, RedisModuleProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        CacheValue value = cache.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value.expireAt().isBefore(LocalDateTime.now())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(value.data(), clazz));
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, new CacheValue(value, LocalDateTime.now().plus(properties.getCacheTtl())));
    }

    @Override
    public void evict(String key) {
        cache.remove(key);
    }

    @Override
    public String storageType() {
        return "MEMORY";
    }

    private record CacheValue(Object data, LocalDateTime expireAt) {
    }
}
