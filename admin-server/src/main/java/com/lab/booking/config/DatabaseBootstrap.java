package com.lab.booking.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class DatabaseBootstrap {

    private static final List<String> CORE_TABLES = List.of(
            "sys_user",
            "sys_role",
            "sys_permission",
            "sys_role_permission",
            "sys_role_menu",
            "sys_menu",
            "sys_icon",
            "lab_device",
            "lab_reservation",
            "lab_borrow_record",
            "lab_repair",
            "lab_notification",
            "lab_task_execution_log"
    );

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseBootstrap(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeIfNeeded() {
        if (!coreTablesExist()) {
            runSchemaScript();
        }

        upgradeSchemaIfNeeded();

        if (!tableExists("sys_user") || isTableEmpty("sys_user")) {
            runSeedScript();
        }

        upgradeBuiltinPasswordsIfNeeded();
    }

    public void resetDatabase() {
        List<String> tables = List.of(
                "lab_task_execution_log",
                "lab_notification",
                "lab_repair",
                "lab_borrow_record",
                "lab_reservation",
                "lab_device",
                "sys_icon",
                "sys_menu",
                "sys_role_menu",
                "sys_role_permission",
                "sys_permission",
                "sys_role",
                "sys_user"
        );
        for (String table : tables) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + table);
        }
        runSchemaScript();
        runSeedScript();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE() AND table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean coreTablesExist() {
        return CORE_TABLES.stream().allMatch(this::tableExists);
    }

    private boolean isTableEmpty(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + tableName, Integer.class);
        return count == null || count == 0;
    }

    private void runSchemaScript() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/migration/V1__init_schema.sql"));
        populator.execute(dataSource);
    }

    private void runSeedScript() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/migration/V2__seed_data.sql"));
        populator.execute(dataSource);
    }

    private void upgradeSchemaIfNeeded() {
        ensureColumnExists("sys_user", "first_login_required", "ALTER TABLE sys_user ADD COLUMN first_login_required TINYINT(1) NOT NULL DEFAULT 1 AFTER password");
        ensureColumnExists("lab_device", "image_url", "ALTER TABLE lab_device ADD COLUMN image_url VARCHAR(255) NULL AFTER location");
    }

    private void upgradeBuiltinPasswordsIfNeeded() {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET password = '0000'
                WHERE login_id IN ('SA001', 'A001', 'T2026001', '20230001')
                  AND password IN ('123456', '000000')
                """);
        jdbcTemplate.update("""
                UPDATE sys_user
                SET first_login_required = 0
                WHERE login_id IN ('SA001', 'A001', 'T2026001', '20230001')
                """);
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        if (!tableExists(tableName)) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
