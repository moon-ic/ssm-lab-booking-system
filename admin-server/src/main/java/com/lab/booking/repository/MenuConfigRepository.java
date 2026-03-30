package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.MenuEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class MenuConfigRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public MenuConfigRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MenuEntity> findAllMenus() {
        return jdbcTemplate.query("SELECT * FROM sys_menu ORDER BY menu_id", this::mapMenu);
    }

    public Optional<MenuEntity> findMenuById(Long menuId) {
        return jdbcTemplate.query("SELECT * FROM sys_menu WHERE menu_id = ? LIMIT 1", this::mapMenu, menuId)
                .stream()
                .findFirst();
    }

    public void saveMenu(MenuEntity menu) {
        jdbcTemplate.update("""
                        INSERT INTO sys_menu (menu_id, menu_name, path, icon, permission_code)
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            menu_name = VALUES(menu_name),
                            path = VALUES(path),
                            icon = VALUES(icon),
                            permission_code = VALUES(permission_code)
                        """,
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getPath(),
                menu.getIcon(),
                menu.getPermissionCode()
        );
    }

    public List<String> findAllIcons() {
        return jdbcTemplate.queryForList("SELECT icon_name FROM sys_icon ORDER BY sort_order, icon_name", String.class);
    }

    private MenuEntity mapMenu(ResultSet rs, int rowNum) throws SQLException {
        MenuEntity menu = new MenuEntity();
        menu.setMenuId(rs.getLong("menu_id"));
        menu.setMenuName(normalizeLegacyText(rs.getString("menu_name")));
        menu.setPath(rs.getString("path"));
        menu.setIcon(rs.getString("icon"));
        menu.setPermissionCode(rs.getString("permission_code"));
        return menu;
    }
}
