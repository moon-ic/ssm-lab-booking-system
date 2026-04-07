import { getCurrentUserIdFromToken, wait } from "@/mock/userStore";
import type {
    AssignPermissionsPayload,
    MenuItemOption,
    PermissionItem,
    RoleItem,
    SaveRolePayload
} from "@/types/role";

const ROLES_STORAGE_KEY = "mock-role-items";

const defaultMenus: MenuItemOption[] = [
    { menuId: 201, menuName: "首页", path: "/", icon: "HomeFilled", permissionCode: "statistics:view" },
    { menuId: 202, menuName: "设备管理", path: "/devices", icon: "Monitor", permissionCode: "device:view" },
    { menuId: 203, menuName: "设备导入", path: "/device-imports", icon: "UploadFilled", permissionCode: "device:import" },
    { menuId: 204, menuName: "预约管理", path: "/reservations", icon: "Calendar", permissionCode: "reservation:view" },
    { menuId: 205, menuName: "维修管理", path: "/repairs", icon: "Tools", permissionCode: "repair:view" },
    { menuId: 206, menuName: "个人中心", path: "/profile", icon: "UserFilled", permissionCode: "profile:view" },
    { menuId: 207, menuName: "用户管理", path: "/users", icon: "User", permissionCode: "user:view" },
    { menuId: 208, menuName: "角色权限", path: "/roles", icon: "Lock", permissionCode: "role:manage" }
];

const defaultRoles: RoleItem[] = [
    {
        roleId: 1,
        roleName: "Super Admin",
        roleCode: "SUPER_ADMIN",
        remark: "Full platform access, role and permission management.",
        permissionIds: [1, 2, 4, 5, 6, 7, 9, 10, 11, 12],
        menuIds: [201, 202, 203, 204, 205, 206, 207, 208]
    },
    {
        roleId: 2,
        roleName: "Admin",
        roleCode: "ADMIN",
        remark: "Manage teachers, devices, reservations, repairs, and statistics.",
        permissionIds: [1, 4, 5, 6, 7, 9, 10, 11, 12],
        menuIds: [201, 202, 203, 204, 205, 206, 207]
    },
    {
        roleId: 3,
        roleName: "Teacher",
        roleCode: "TEACHER",
        remark: "Manage owned students and approve student reservations.",
        permissionIds: [4, 6, 7, 9, 10, 12],
        menuIds: [201, 202, 204, 205, 206]
    },
    {
        roleId: 4,
        roleName: "Student",
        roleCode: "STUDENT",
        remark: "Browse devices, reservations, repairs, and personal center.",
        permissionIds: [4, 7, 10, 12],
        menuIds: [201, 202, 204, 205, 206]
    }
];

const defaultPermissions: PermissionItem[] = [
    { permissionId: 1, permissionCode: "user:view", permissionName: "View users", type: "ACTION" },
    { permissionId: 2, permissionCode: "role:manage", permissionName: "Manage roles", type: "ACTION" },
    { permissionId: 4, permissionCode: "device:view", permissionName: "View devices", type: "ACTION" },
    { permissionId: 5, permissionCode: "device:import", permissionName: "Import devices", type: "ACTION" },
    { permissionId: 6, permissionCode: "reservation:approve", permissionName: "Approve reservations", type: "ACTION" },
    { permissionId: 7, permissionCode: "reservation:view", permissionName: "View reservations", type: "ACTION" },
    { permissionId: 9, permissionCode: "message:view", permissionName: "View message center", type: "ACTION" },
    { permissionId: 10, permissionCode: "repair:view", permissionName: "View repairs", type: "ACTION" },
    { permissionId: 11, permissionCode: "statistics:view", permissionName: "View statistics", type: "ACTION" },
    { permissionId: 12, permissionCode: "profile:view", permissionName: "View personal center", type: "ACTION" }
];

function readRoles() {
    const raw = localStorage.getItem(ROLES_STORAGE_KEY);
    if (!raw) {
        localStorage.setItem(ROLES_STORAGE_KEY, JSON.stringify(defaultRoles));
        return structuredClone(defaultRoles);
    }

    try {
        return JSON.parse(raw) as RoleItem[];
    } catch {
        localStorage.setItem(ROLES_STORAGE_KEY, JSON.stringify(defaultRoles));
        return structuredClone(defaultRoles);
    }
}

function writeRoles(roles: RoleItem[]) {
    localStorage.setItem(ROLES_STORAGE_KEY, JSON.stringify(roles));
}

function ensureSuperAdmin(token: string) {
    const userId = getCurrentUserIdFromToken(token);
    if (userId !== 1) {
        throw new Error("Only super admin can access role permission management");
    }
}

function nextRoleId(roles: RoleItem[]) {
    return roles.reduce((max, item) => Math.max(max, item.roleId), 0) + 1;
}

export async function mockListRoles(token: string) {
    await wait();
    ensureSuperAdmin(token);
    return readRoles().sort((a, b) => a.roleId - b.roleId);
}

export async function mockGetRoleDetail(token: string, roleId: number) {
    await wait(180);
    ensureSuperAdmin(token);
    const role = readRoles().find((item) => item.roleId === roleId);
    if (!role) {
        throw new Error("Role not found");
    }
    return role;
}

export async function mockCreateRole(token: string, payload: SaveRolePayload) {
    await wait();
    ensureSuperAdmin(token);

    const roles = readRoles();
    if (roles.some((item) => item.roleCode === payload.roleCode)) {
        throw new Error("Role code already exists");
    }

    const role: RoleItem = {
        roleId: nextRoleId(roles),
        roleName: payload.roleName,
        roleCode: payload.roleCode,
        remark: payload.remark,
        permissionIds: [],
        menuIds: []
    };

    roles.push(role);
    writeRoles(roles);
    return role;
}

export async function mockUpdateRole(token: string, roleId: number, payload: SaveRolePayload) {
    await wait();
    ensureSuperAdmin(token);

    const roles = readRoles();
    const role = roles.find((item) => item.roleId === roleId);
    if (!role) {
        throw new Error("Role not found");
    }

    if (roles.some((item) => item.roleId !== roleId && item.roleCode === payload.roleCode)) {
        throw new Error("Role code already exists");
    }

    role.roleName = payload.roleName;
    role.roleCode = payload.roleCode;
    role.remark = payload.remark;
    writeRoles(roles);
    return role;
}

export async function mockAssignPermissions(token: string, roleId: number, payload: AssignPermissionsPayload) {
    await wait(220);
    ensureSuperAdmin(token);

    const roles = readRoles();
    const role = roles.find((item) => item.roleId === roleId);
    if (!role) {
        throw new Error("Role not found");
    }

    role.permissionIds = [...payload.permissionIds];
    role.menuIds = [...payload.menuIds];
    writeRoles(roles);
    return role;
}

export async function mockListPermissions(token: string, type?: string) {
    await wait(120);
    ensureSuperAdmin(token);
    if (!type) {
        return defaultPermissions;
    }
    return defaultPermissions.filter((item) => item.type === type);
}

export async function mockListMenus(token: string) {
    await wait(120);
    ensureSuperAdmin(token);
    return structuredClone(defaultMenus);
}
