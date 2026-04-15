package com.lab.booking.repository;

import com.lab.booking.infrastructure.session.TokenStore;
import com.lab.booking.mapper.UserMapper;
import com.lab.booking.model.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class AuthRepository {

    private final UserMapper userMapper;
    private final TokenStore tokenStore;

    public AuthRepository(UserMapper userMapper, TokenStore tokenStore) {
        this.userMapper = userMapper;
        this.tokenStore = tokenStore;
    }

    public Optional<UserEntity> findByLoginId(String loginId) {
        return Optional.ofNullable(userMapper.selectByLoginId(loginId));
    }

    public Optional<UserEntity> findByToken(String token) {
        return tokenStore.findUserIdByToken(token).flatMap(this::findById);
    }

    public String createToken(Long userId) {
        return tokenStore.createToken(userId);
    }

    public String tokenStorageType() {
        return tokenStore.storageType();
    }

    public Map<Long, UserEntity> getUsers() {
        Map<Long, UserEntity> result = new LinkedHashMap<>();
        userMapper.selectAll().forEach(user -> result.put(user.getUserId(), user));
        return result;
    }

    public Optional<UserEntity> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    public long nextUserId() {
        Long next = userMapper.selectNextUserId();
        return next == null ? 11L : next;
    }

    public void save(UserEntity user) {
        userMapper.upsertUser(user);
    }
}
