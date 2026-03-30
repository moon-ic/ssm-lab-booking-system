package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.infrastructure.session.TokenStore;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.model.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class AuthRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;
    private final TokenStore tokenStore;

    public AuthRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, TokenStore tokenStore) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
        this.tokenStore = tokenStore;
    }

    public Optional<UserEntity> findByLoginId(String loginId) {
        return jdbcTemplate.query("""
                        SELECT * FROM sys_user
                        WHERE deleted = 0 AND (login_id = ? OR account = ?)
                        LIMIT 1
                        """,
                this::mapUser,
                loginId,
                loginId
        ).stream().findFirst();
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
        jdbcTemplate.query("SELECT * FROM sys_user ORDER BY user_id", this::mapUser)
                .forEach(user -> result.put(user.getUserId(), user));
        return result;
    }

    public Optional<UserEntity> findById(Long userId) {
        return jdbcTemplate.query("""
                        SELECT * FROM sys_user
                        WHERE user_id = ? AND deleted = 0
                        LIMIT 1
                        """,
                this::mapUser,
                userId
        ).stream().findFirst();
    }

    public long nextUserId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(user_id), 10) + 1 FROM sys_user", Long.class);
        return next == null ? 11L : next;
    }

    public void save(UserEntity user) {
        jdbcTemplate.update("""
                        INSERT INTO sys_user (
                            user_id, name, account, login_id, phone, role_code, status, credit_score, password, first_login_required, manager_id, deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            name = VALUES(name),
                            account = VALUES(account),
                            login_id = VALUES(login_id),
                            phone = VALUES(phone),
                            role_code = VALUES(role_code),
                            status = VALUES(status),
                            credit_score = VALUES(credit_score),
                            password = VALUES(password),
                            first_login_required = VALUES(first_login_required),
                            manager_id = VALUES(manager_id),
                            deleted = VALUES(deleted)
                        """,
                user.getUserId(),
                user.getName(),
                user.getAccount(),
                user.getLoginId(),
                user.getPhone(),
                user.getRoleCode().name(),
                user.getStatus().name(),
                user.getCreditScore(),
                user.getPassword(),
                user.isFirstLoginRequired(),
                user.getManagerId(),
                user.isDeleted()
        );
    }

    private UserEntity mapUser(ResultSet rs, int rowNum) throws SQLException {
        UserEntity user = new UserEntity();
        user.setUserId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setAccount(rs.getString("account"));
        user.setLoginId(rs.getString("login_id"));
        user.setPhone(rs.getString("phone"));
        user.setRoleCode(RoleCode.valueOf(rs.getString("role_code")));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setCreditScore(rs.getInt("credit_score"));
        user.setPassword(rs.getString("password"));
        user.setFirstLoginRequired(rs.getBoolean("first_login_required"));
        Long managerId = rs.getObject("manager_id", Long.class);
        user.setManagerId(managerId);
        user.setDeleted(rs.getBoolean("deleted"));
        return user;
    }
}
