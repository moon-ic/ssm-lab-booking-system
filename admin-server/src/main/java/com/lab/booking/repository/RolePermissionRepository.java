package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RolePermissionRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public RolePermissionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoleEntity> findAllRoles() {
        return jdbcTemplate.query("SELECT * FROM sys_role ORDER BY role_id", this::mapRole)
                .stream()
                .map(this::fillRoleRelations)
                .toList();
    }

    public Optional<RoleEntity> findRoleById(Long roleId) {
        return jdbcTemplate.query("SELECT * FROM sys_role WHERE role_id = ? LIMIT 1", this::mapRole, roleId)
                .stream()
                .findFirst()
                .map(this::fillRoleRelations);
    }

    public void saveRole(RoleEntity role) {
        jdbcTemplate.update("""
                        INSERT INTO sys_role (role_id, role_name, role_code, remark)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            role_name = VALUES(role_name),
                            role_code = VALUES(role_code),
                            remark = VALUES(remark)
                        """,
                role.getRoleId(),
                role.getRoleName(),
                role.getRoleCode(),
                role.getRemark()
        );

        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", role.getRoleId());
        for (Long permissionId : role.getPermissionIds()) {
            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", role.getRoleId(), permissionId);
        }

        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", role.getRoleId());
        for (Long menuId : role.getMenuIds()) {
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", role.getRoleId(), menuId);
        }
    }

    public long nextRoleId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(role_id), 10) + 1 FROM sys_role", Long.class);
        return next == null ? 11L : next;
    }

    public boolean existsByRoleCode(String roleCode, Long excludeRoleId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM sys_role
                        WHERE role_code = ? AND (? IS NULL OR role_id <> ?)
                        """,
                Integer.class,
                roleCode,
                excludeRoleId,
                excludeRoleId
        );
        return count != null && count > 0;
    }

    public List<PermissionEntity> findPermissions(String type) {
        if (type == null) {
            return jdbcTemplate.query("SELECT * FROM sys_permission ORDER BY permission_id", this::mapPermission);
        }
        return jdbcTemplate.query("SELECT * FROM sys_permission WHERE type = ? ORDER BY permission_id", this::mapPermission, type);
    }

    private RoleEntity fillRoleRelations(RoleEntity role) {
        role.setPermissionIds(new ArrayList<>(jdbcTemplate.queryForList(
                "SELECT permission_id FROM sys_role_permission WHERE role_id = ? ORDER BY permission_id",
                Long.class,
                role.getRoleId()
        )));
        role.setMenuIds(new ArrayList<>(jdbcTemplate.queryForList(
                "SELECT menu_id FROM sys_role_menu WHERE role_id = ? ORDER BY menu_id",
                Long.class,
                role.getRoleId()
        )));
        return role;
    }

    private RoleEntity mapRole(ResultSet rs, int rowNum) throws SQLException {
        RoleEntity role = new RoleEntity();
        role.setRoleId(rs.getLong("role_id"));
        role.setRoleName(rs.getString("role_name"));
        role.setRoleCode(rs.getString("role_code"));
        role.setRemark(rs.getString("remark"));
        return role;
    }

    private PermissionEntity mapPermission(ResultSet rs, int rowNum) throws SQLException {
        return new PermissionEntity(
                rs.getLong("permission_id"),
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("type")
        );
    }
}
