import request from '@/api/request'
import { mockListIcons, mockListMenuConfigs, mockUpdateMenuConfig } from '@/mock/menu-config'
import type { ApiResult } from '@/types/auth'
import type { MenuConfigItem, UpdateMenuConfigPayload } from '@/types/menu-config'

const useMock = import.meta.env.VITE_ENABLE_MOCK !== 'false'

function getSessionToken() {
  const raw = localStorage.getItem('admin-auth-session')
  if (!raw) {
    throw new Error('No active session')
  }

  try {
    return (JSON.parse(raw) as { token: string }).token
  } catch {
    throw new Error('Invalid session')
  }
}

async function unwrapResult<T>(promise: Promise<{ data: ApiResult<T> }>) {
  const response = await promise
  if (response.data.code !== 0) {
    throw new Error(response.data.message || 'Request failed')
  }
  return response.data.data
}

export function listIcons() {
  if (useMock) {
    return mockListIcons(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<string[]>>('/api/icons')
  )
}

export function listMenuConfigs() {
  if (useMock) {
    return mockListMenuConfigs(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<MenuConfigItem[]>>('/api/menus')
  )
}

export function updateMenuConfig(menuId: number, payload: UpdateMenuConfigPayload) {
  if (useMock) {
    return mockUpdateMenuConfig(getSessionToken(), menuId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<MenuConfigItem>>(`/api/menus/${menuId}`, payload)
  )
}
