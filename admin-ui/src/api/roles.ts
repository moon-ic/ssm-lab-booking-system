import request from '@/api/request'
import {
  mockAssignPermissions,
  mockCreateRole,
  mockGetRoleDetail,
  mockListMenus,
  mockListPermissions,
  mockListRoles,
  mockUpdateRole
} from '@/mock/roles'
import type { ApiResult } from '@/types/auth'
import type {
  AssignPermissionsPayload,
  MenuItemOption,
  PermissionItem,
  RoleItem,
  SaveRolePayload
} from '@/types/role'

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

export function listRoles() {
  if (useMock) {
    return mockListRoles(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<RoleItem[]>>('/api/roles')
  )
}

export function getRoleDetail(roleId: number) {
  if (useMock) {
    return mockGetRoleDetail(getSessionToken(), roleId)
  }

  return unwrapResult(
    request.get<ApiResult<RoleItem>>(`/api/roles/${roleId}`)
  )
}

export function createRole(payload: SaveRolePayload) {
  if (useMock) {
    return mockCreateRole(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<RoleItem>>('/api/roles', payload)
  )
}

export function updateRole(roleId: number, payload: SaveRolePayload) {
  if (useMock) {
    return mockUpdateRole(getSessionToken(), roleId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<RoleItem>>(`/api/roles/${roleId}`, payload)
  )
}

export function assignPermissions(roleId: number, payload: AssignPermissionsPayload) {
  if (useMock) {
    return mockAssignPermissions(getSessionToken(), roleId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<RoleItem>>(`/api/roles/${roleId}/permissions`, payload)
  )
}

export function listPermissions(type?: string) {
  if (useMock) {
    return mockListPermissions(getSessionToken(), type)
  }

  return unwrapResult(
    request.get<ApiResult<PermissionItem[]>>('/api/permissions', {
      params: {
        type
      }
    })
  )
}

export function listMenus() {
  if (useMock) {
    return mockListMenus(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<MenuItemOption[]>>('/api/menus')
  )
}
