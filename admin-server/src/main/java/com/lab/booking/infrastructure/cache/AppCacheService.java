package com.lab.booking.infrastructure.cache;

import java.util.Optional;

public interface AppCacheService {

    <T> Optional<T> get(String key, Class<T> clazz);

    void put(String key, Object value);

    void evict(String key);

    String storageType();
}
