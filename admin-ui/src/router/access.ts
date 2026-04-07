import type { RouteRecordRaw } from "vue-router";

export type AppRole = "SUPER_ADMIN" | "ADMIN" | "TEACHER" | "STUDENT";

export interface AppRouteMeta {
    title: string;
    menu?: boolean;
    roles?: AppRole[];
    allowFirstLogin?: boolean;
}

export interface MenuItem {
    title: string;
    path: string;
}

export const appRouteChildren: RouteRecordRaw[] = [
    {
        path: "",
        name: "dashboard",
        component: () => import("@/views/StatisticsView.vue"),
        meta: {
            title: "首页",
            menu: true,
            allowFirstLogin: true
        } satisfies AppRouteMeta
    },
    {
        path: "devices",
        name: "devices",
        component: () => import("@/views/DeviceManagementView.vue"),
        meta: {
            title: "设备管理",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT"]
        } satisfies AppRouteMeta
    },
    {
        path: "device-imports",
        name: "device-imports",
        component: () => import("@/views/DeviceImportView.vue"),
        meta: {
            title: "设备导入",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN"]
        } satisfies AppRouteMeta
    },
    {
        path: "reservations",
        name: "reservations",
        component: () => import("@/views/ReservationManagementView.vue"),
        meta: {
            title: "预约管理",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT"]
        } satisfies AppRouteMeta
    },
    {
        path: "messages",
        name: "messages",
        component: () => import("@/views/MessageCenterView.vue"),
        meta: {
            title: "消息中心",
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT"]
        } satisfies AppRouteMeta
    },
    {
        path: "repairs",
        name: "repairs",
        component: () => import("@/views/RepairManagementView.vue"),
        meta: {
            title: "维修管理",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT"]
        } satisfies AppRouteMeta
    },
    {
        path: "profile",
        name: "profile",
        component: () => import("@/views/ProfileCenterView.vue"),
        meta: {
            title: "个人中心",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT"],
            allowFirstLogin: true
        } satisfies AppRouteMeta
    },
    {
        path: "users",
        name: "users",
        component: () => import("@/views/UserManagementView.vue"),
        meta: {
            title: "用户管理",
            menu: true,
            roles: ["SUPER_ADMIN", "ADMIN", "TEACHER"]
        } satisfies AppRouteMeta
    },
    {
        path: "roles",
        name: "roles",
        component: () => import("@/views/RolePermissionView.vue"),
        meta: {
            title: "角色权限",
            menu: true,
            roles: ["SUPER_ADMIN"]
        } satisfies AppRouteMeta
    }
];

function hasRoleAccess(meta: AppRouteMeta | undefined, roleCode?: string) {
    if (!meta?.roles || meta.roles.length === 0) {
        return true;
    }

    if (!roleCode) {
        return false;
    }

    return meta.roles.includes(roleCode as AppRole);
}

export function getAccessibleMenuItems(roleCode?: string): MenuItem[] {
    return appRouteChildren
        .filter((route) => {
            const meta = route.meta as unknown as AppRouteMeta | undefined;
            return meta?.menu && hasRoleAccess(meta, roleCode);
        })
        .map((route) => {
            const meta = route.meta as unknown as AppRouteMeta;
            return {
                title: meta.title,
                path: route.path ? `/${route.path}` : "/"
            };
        });
}

export function findRouteMetaByPath(path: string) {
    const normalizedPath = path.replace(/\/+$/, "") || "/";
    const route = appRouteChildren.find((item) => {
        const routePath = item.path ? `/${item.path}` : "/";
        return routePath === normalizedPath;
    });

    return route?.meta as unknown as AppRouteMeta | undefined;
}

export function canAccessRoute(meta: AppRouteMeta | undefined, roleCode?: string) {
    return hasRoleAccess(meta, roleCode);
}
