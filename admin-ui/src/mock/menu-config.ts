import { getCurrentUserIdFromToken, wait } from '@/mock/userStore'
import type { MenuConfigItem, UpdateMenuConfigPayload } from '@/types/menu-config'

const MENU_CONFIG_STORAGE_KEY = 'mock-menu-config-items'

export const defaultMenuConfigs: MenuConfigItem[] = [
  { menuId: 201, menuName: 'Dashboard', path: '/', icon: 'HomeFilled', permissionCode: 'profile:view' },
  { menuId: 202, menuName: 'Users', path: '/users', icon: 'User', permissionCode: 'user:view' },
  { menuId: 203, menuName: 'Roles', path: '/roles', icon: 'Lock', permissionCode: 'role:manage' },
  { menuId: 204, menuName: 'Profile', path: '/profile', icon: 'Avatar', permissionCode: 'profile:view' },
  { menuId: 205, menuName: 'Devices', path: '/devices', icon: 'Monitor', permissionCode: 'device:view' },
  { menuId: 206, menuName: 'Device Imports', path: '/device-imports', icon: 'UploadFilled', permissionCode: 'device:import' },
  { menuId: 207, menuName: 'Reservations', path: '/reservations', icon: 'Calendar', permissionCode: 'reservation:view' },
  { menuId: 208, menuName: 'Borrow Records', path: '/borrow-records', icon: 'Tickets', permissionCode: 'borrow-record:view' },
  { menuId: 209, menuName: 'Messages', path: '/messages', icon: 'Bell', permissionCode: 'message:view' },
  { menuId: 210, menuName: 'Repairs', path: '/repairs', icon: 'Tools', permissionCode: 'repair:view' },
  { menuId: 211, menuName: 'Statistics', path: '/statistics', icon: 'PieChart', permissionCode: 'statistics:view' },
  { menuId: 212, menuName: 'Menu Config', path: '/menu-config', icon: 'Grid', permissionCode: 'menu:manage' }
]

const defaultIcons = [
  'HomeFilled',
  'User',
  'Lock',
  'Avatar',
  'Monitor',
  'UploadFilled',
  'Calendar',
  'Tickets',
  'Bell',
  'Tools',
  'PieChart',
  'Grid',
  'Setting',
  'Document',
  'Menu'
]

const validPermissionCodes = [
  'user:view',
  'role:manage',
  'menu:manage',
  'device:view',
  'device:import',
  'reservation:view',
  'reservation:approve',
  'borrow-record:view',
  'message:view',
  'repair:view',
  'statistics:view',
  'profile:view'
]

function ensureSuperAdmin(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  if (userId !== 1) {
    throw new Error('Only super admin can manage menu configuration')
  }
}

export function readMenuConfigs() {
  const raw = localStorage.getItem(MENU_CONFIG_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(MENU_CONFIG_STORAGE_KEY, JSON.stringify(defaultMenuConfigs))
    return structuredClone(defaultMenuConfigs)
  }

  try {
    return JSON.parse(raw) as MenuConfigItem[]
  } catch {
    localStorage.setItem(MENU_CONFIG_STORAGE_KEY, JSON.stringify(defaultMenuConfigs))
    return structuredClone(defaultMenuConfigs)
  }
}

function writeMenuConfigs(items: MenuConfigItem[]) {
  localStorage.setItem(MENU_CONFIG_STORAGE_KEY, JSON.stringify(items))
}

export async function mockListIcons(token: string) {
  await wait(120)
  ensureSuperAdmin(token)
  return defaultIcons
}

export async function mockListMenuConfigs(token: string) {
  await wait(160)
  ensureSuperAdmin(token)
  return readMenuConfigs().sort((left, right) => left.menuId - right.menuId)
}

export async function mockUpdateMenuConfig(token: string, menuId: number, payload: UpdateMenuConfigPayload) {
  await wait(220)
  ensureSuperAdmin(token)

  if (!defaultIcons.includes(payload.icon)) {
    throw new Error('Icon does not exist')
  }
  if (!validPermissionCodes.includes(payload.permissionCode)) {
    throw new Error('Permission code does not exist')
  }

  const items = readMenuConfigs()
  const item = items.find((entry) => entry.menuId === menuId)
  if (!item) {
    throw new Error('Menu not found')
  }
  if (items.some((entry) => entry.menuId !== menuId && entry.path === payload.path.trim())) {
    throw new Error('Menu path already exists')
  }

  item.menuName = payload.menuName.trim()
  item.path = payload.path.trim()
  item.icon = payload.icon
  item.permissionCode = payload.permissionCode.trim()
  writeMenuConfigs(items)
  return item
}
