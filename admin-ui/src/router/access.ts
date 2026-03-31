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
      title: 'Dashboard',
      description: 'Overview of the current authenticated workspace.',
      menu: true,
      allowFirstLogin: true
    } satisfies AppRouteMeta
  },
  {
    path: 'users',
    name: 'users',
    component: () => import('@/views/UserManagementView.vue'),
    meta: {
      title: 'Users',
      description: 'Manage admin, teacher, and student accounts.',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER']
    } satisfies AppRouteMeta
  },
  {
    path: 'roles',
    name: 'roles',
    component: () => import('@/views/RolePermissionView.vue'),
    meta: {
      title: 'Roles',
      description: 'Configure roles, permissions, and menu visibility.',
      menu: true,
      roles: ['SUPER_ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'profile',
    name: 'profile',
    component: () => import('@/views/ProfileCenterView.vue'),
    meta: {
      title: 'Profile',
      description: 'Review personal info, borrow records, and messages.',
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
      title: 'Devices',
      description: 'Browse and maintain device information.',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'device-imports',
    name: 'device-imports',
    component: () => import('@/views/DeviceImportView.vue'),
    meta: {
      title: 'Device Imports',
      description: 'Import devices and register image-based entries.',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'reservations',
    name: 'reservations',
    component: () => import('@/views/ReservationManagementView.vue'),
    meta: {
      title: 'Reservations',
      description: 'Review or submit reservation records by role.',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'borrow-records',
    name: 'borrow-records',
    component: () => import('@/views/BorrowRecordManagementView.vue'),
    meta: {
      title: 'Borrow Records',
      description: 'Track pickup, return, and overdue records.',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'messages',
    name: 'messages',
    component: () => import('@/views/MessageCenterView.vue'),
    meta: {
      title: 'Messages',
      description: 'Review notifications and confirmation items.',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'repairs',
    name: 'repairs',
    component: () => import('@/views/RepairManagementView.vue'),
    meta: {
      title: 'Repairs',
      description: 'Submit or process repair requests.',
      menu: true,
      roles: ['ADMIN', 'TEACHER', 'STUDENT']
    } satisfies AppRouteMeta
  },
  {
    path: 'statistics',
    name: 'statistics',
    component: () => import('@/views/StatisticsView.vue'),
    meta: {
      title: 'Statistics',
      description: 'Access overview and ranking dashboards.',
      menu: true,
      roles: ['SUPER_ADMIN', 'ADMIN']
    } satisfies AppRouteMeta
  },
  {
    path: 'menu-config',
    name: 'menu-config',
    component: () => import('@/views/MenuConfigView.vue'),
    meta: {
      title: 'Menu Config',
      description: 'Maintain icon and menu configuration.',
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
