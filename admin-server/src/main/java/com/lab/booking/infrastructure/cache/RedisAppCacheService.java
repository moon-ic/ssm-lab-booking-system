package com.lab.booking.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.config.RedisModuleProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

public class RedisAppCacheService implements AppCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisModuleProperties properties;

    public RedisAppCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, RedisModuleProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(cacheKey(key));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, clazz));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize redis cache", ex);
        }
    }

    @Override
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(cacheKey(key), objectMapper.writeValueAsString(value), properties.getCacheTtl());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize redis cache", ex);
        }
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(cacheKey(key));
    }

    @Override
    public String storageType() {
        return "REDIS";
    }

    private String cacheKey(String key) {
        return properties.getKeyPrefix() + "cache:" + key;
    }
}
