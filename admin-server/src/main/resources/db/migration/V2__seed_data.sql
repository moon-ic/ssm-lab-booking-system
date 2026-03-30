INSERT INTO sys_user (user_id, name, account, login_id, phone, role_code, status, credit_score, password, first_login_required, manager_id, deleted) VALUES
    (1, 'System Admin', 'superadmin', 'SA001', '13800000000', 'SUPER_ADMIN', 'ENABLED', 100, '000000', 0, NULL, 0),
    (2, 'Device Admin', 'admin01', 'A001', '13800000001', 'ADMIN', 'ENABLED', 100, '000000', 0, 1, 0),
    (3, 'Teacher Li', 'teacher01', 'T2026001', '13800000002', 'TEACHER', 'ENABLED', 100, '000000', 0, 2, 0),
    (4, 'Student Wang', 'student01', '20230001', '13800000003', 'STUDENT', 'ENABLED', 100, '000000', 0, 3, 0);

INSERT INTO sys_role (role_id, role_name, role_code, remark) VALUES
    (1, 'Super Admin', 'SUPER_ADMIN', 'Built-in role'),
    (2, 'Admin', 'ADMIN', 'Built-in role'),
    (3, 'Teacher', 'TEACHER', 'Built-in role'),
    (4, 'Student', 'STUDENT', 'Built-in role');

INSERT INTO sys_permission (permission_id, permission_code, permission_name, type) VALUES
    (1, 'device:view', 'View Devices', 'ACTION'),
    (2, 'reservation:approve', 'Approve Reservations', 'ACTION'),
    (3, 'repair:update', 'Update Repairs', 'ACTION'),
    (4, 'user:manage', 'Manage Users', 'ACTION'),
    (201, 'menu:dashboard', 'Dashboard Menu', 'MENU'),
    (202, 'menu:device', 'Device Menu', 'MENU'),
    (203, 'menu:user', 'User Menu', 'MENU');

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4),
    (2, 1), (2, 2), (2, 3),
    (3, 1), (3, 2),
    (4, 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 201), (1, 202), (1, 203),
    (2, 201), (2, 202),
    (3, 201),
    (4, 201);

INSERT INTO sys_menu (menu_id, menu_name, path, icon, permission_code) VALUES
    (201, 'Dashboard', '/dashboard', 'HomeFilled', 'menu:dashboard'),
    (202, 'Device Management', '/device', 'Monitor', 'device:view'),
    (203, 'User Management', '/user', 'UserFilled', 'user:manage');

INSERT INTO sys_icon (icon_name, sort_order) VALUES
    ('HomeFilled', 1),
    ('Monitor', 2),
    ('UserFilled', 3),
    ('Calendar', 4),
    ('Setting', 5),
    ('Tools', 6);

INSERT INTO lab_device (device_id, device_name, device_code, category, status, location, image_url, description) VALUES
    (1001, 'Projector A', 'EQ-2026-0001', 'Projection', 'AVAILABLE', 'Lab 101', '/assets/devices/projector-a.png', 'Portable projector'),
    (1002, 'Camera B', 'EQ-2026-0002', 'Photography', 'RESERVED', 'Lab 102', '/assets/devices/camera-b.png', 'Course recording camera'),
    (1003, 'Oscilloscope C', 'EQ-2026-0003', 'Electronics', 'REPAIRING', 'Lab 101', '/assets/devices/oscilloscope-c.png', 'Electronics lab tester');
