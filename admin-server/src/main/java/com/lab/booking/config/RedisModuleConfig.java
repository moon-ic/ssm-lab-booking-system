package com.lab.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.infrastructure.cache.AppCacheService;
import com.lab.booking.infrastructure.cache.InMemoryAppCacheService;
import com.lab.booking.infrastructure.cache.RedisAppCacheService;
import com.lab.booking.infrastructure.session.InMemoryTokenStore;
import com.lab.booking.infrastructure.session.RedisTokenStore;
import com.lab.booking.infrastructure.session.TokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisModuleProperties.class)
public class RedisModuleConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
    public TokenStore redisTokenStore(StringRedisTemplate redisTemplate, RedisModuleProperties properties) {
        return new RedisTokenStore(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore inMemoryTokenStore(RedisModuleProperties properties) {
        return new InMemoryTokenStore(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
    public AppCacheService redisAppCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisModuleProperties properties
    ) {
        return new RedisAppCacheService(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(AppCacheService.class)
    public AppCacheService inMemoryAppCacheService(ObjectMapper objectMapper, RedisModuleProperties properties) {
        return new InMemoryAppCacheService(objectMapper, properties);
    }
}
