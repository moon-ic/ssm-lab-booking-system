import type { RouteRecordRaw } from 'vue-router'

export type AppRole = 'SUPER_ADMIN' | 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface AppRouteMeta {
  title: string
  description: string
  menu?: boolean
  roles?: AppRole[]
  allowFirstLogin?: boolean
}

export interface MenuItem {
  title: string
  path: string
  description: string
}

export const appRouteChildren: RouteRecordRaw[] = [
  {
    path: '',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: {
      title: '首页',
      description: '查看当前登录工作台的整体概览。',
      menu: true,
      allowFirstLogin: true
    } satisfies AppRouteMeta
  },
  {
    path: 'users',
    name: 'users',
    component: () => import('@/views/UserManagementView.vue'),
    meta: {
      title: '用户管理',
      description: '管理管理员、教师和学生账号。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER']
    } satisfies AppRouteMeta
  },
  {
    path: 'roles',
    name: 'roles',
    component: () => import('@/views/RolePermissionView.vue'),
    meta: {
      title: '角色权限',
      description: '配置角色、权限与菜单可见范围。',
      menu: true,
      roles: ['SUPER_ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'profile',
    name: 'profile',
    component: () => import('@/views/ProfileCenterView.vue'),
    meta: {
      title: '个人中心',
      description: '查看个人信息、借用记录和消息通知。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT'],
      allowFirstLogin: true
    } satisfies AppRouteMeta
  },
  {
    path: 'devices',
    name: 'devices',
    component: () => import('@/views/DeviceManagementView.vue'),
    meta: {
      title: '设备管理',
      description: '浏览并维护设备信息。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'device-imports',
    name: 'device-imports',
    component: () => import('@/views/DeviceImportView.vue'),
    meta: {
      title: '设备导入',
      description: '导入设备并登记图片类条目。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'reservations',
    name: 'reservations',
    component: () => import('@/views/ReservationManagementView.vue'),
    meta: {
      title: '预约管理',
      description: '按角色提交或审核预约记录。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'borrow-records',
    name: 'borrow-records',
    component: () => import('@/views/BorrowRecordManagementView.vue'),
    meta: {
      title: '借用记录',
      description: '跟踪领取、归还与逾期记录。',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'messages',
    name: 'messages',
    component: () => import('@/views/MessageCenterView.vue'),
    meta: {
      title: '消息中心',
      description: '查看通知消息和待确认事项。',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'repairs',
    name: 'repairs',
    component: () => import('@/views/RepairManagementView.vue'),
    meta: {
      title: '维修管理',
      description: '提交或处理维修申请。',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'statistics',
    name: 'statistics',
    component: () => import('@/views/StatisticsView.vue'),
    meta: {
      title: '统计分析',
      description: '查看概览数据与排行看板。',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'menu-config',
    name: 'menu-config',
    component: () => import('@/views/MenuConfigView.vue'),
    meta: {
      title: '菜单配置',
      description: '维护图标和菜单配置。',
      menu: true,
      roles: ['SUPER_ADMIN']
    } satisfies AppRouteMeta
  }
]

function hasRoleAccess(meta: AppRouteMeta | undefined, roleCode?: string) {
  if (!meta?.roles || meta.roles.length === 0) {
    return true
  }

  if (!roleCode) {
    return false
  }

  return meta.roles.includes(roleCode as AppRole)
}

export function getAccessibleMenuItems(roleCode?: string): MenuItem[] {
  return appRouteChildren
    .filter((route) => {
      const meta = route.meta as unknown as AppRouteMeta | undefined
      return meta?.menu && hasRoleAccess(meta, roleCode)
    })
    .map((route) => {
      const meta = route.meta as unknown as AppRouteMeta
      return {
        title: meta.title,
        path: route.path ? `/${route.path}` : '/',
        description: meta.description
      }
    })
}

export function canAccessRoute(meta: AppRouteMeta | undefined, roleCode?: string) {
  return hasRoleAccess(meta, roleCode)
}
