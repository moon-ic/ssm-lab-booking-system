CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    account VARCHAR(100) NOT NULL,
    login_id VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    role_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    credit_score INT NOT NULL,
    password VARCHAR(100) NOT NULL,
    first_login_required TINYINT(1) NOT NULL DEFAULT 1,
    manager_id BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_account ON sys_user(account);
CREATE UNIQUE INDEX uk_sys_user_login_id ON sys_user(login_id);

CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    remark VARCHAR(255)
);

CREATE UNIQUE INDEX uk_sys_role_code ON sys_role(role_code);

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id BIGINT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL
);

CREATE UNIQUE INDEX uk_sys_permission_code ON sys_permission(permission_code);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT PRIMARY KEY,
    menu_name VARCHAR(100) NOT NULL,
    path VARCHAR(255) NOT NULL,
    icon VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_icon (
    icon_name VARCHAR(100) PRIMARY KEY,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS lab_device (
    device_id BIGINT PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    device_code VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    image_url VARCHAR(255) NULL,
    description VARCHAR(255)
);

CREATE UNIQUE INDEX uk_lab_device_code ON lab_device(device_code);

CREATE TABLE IF NOT EXISTS lab_reservation (
    reservation_id BIGINT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reviewer_id BIGINT NULL,
    review_comment VARCHAR(255) NULL,
    created_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS lab_borrow_record (
    record_id BIGINT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    pickup_time DATETIME NULL,
    expected_return_time DATETIME NULL,
    return_time DATETIME NULL,
    device_condition VARCHAR(50) NULL
);

CREATE TABLE IF NOT EXISTS lab_repair (
    repair_id BIGINT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    comment VARCHAR(255) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS lab_notification (
    notification_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    related_biz_type VARCHAR(50) NULL,
    related_biz_id BIGINT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    read_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS lab_task_execution_log (
    log_id BIGINT PRIMARY KEY,
    task_code VARCHAR(100) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT NULL,
    summary VARCHAR(500) NULL,
    error_message VARCHAR(500) NULL,
    result_snapshot_json TEXT NULL
);
