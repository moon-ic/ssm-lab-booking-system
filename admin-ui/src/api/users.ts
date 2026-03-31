import request from '@/api/request'
import {
  mockCreateAdmin,
  mockCreateStudent,
  mockCreateTeacher,
  mockDeleteStudent,
  mockGetUserDetail,
  mockListUsers,
  mockResetPassword,
  mockUpdateUserStatus
} from '@/mock/users'
import type { ApiResult } from '@/types/auth'
import type {
  CreateAdminPayload,
  CreateStudentPayload,
  CreateTeacherPayload,
  ResetPasswordPayload,
  UpdateUserStatusPayload,
  UserListItem,
  UserListQuery,
  UserListResponse
} from '@/types/user'

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

export function listUsers(query: UserListQuery) {
  if (useMock) {
    return mockListUsers(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<UserListResponse>>('/api/users', { params: query })
  )
}

export function getUserDetail(userId: number) {
  if (useMock) {
    return mockGetUserDetail(getSessionToken(), userId)
  }

  return unwrapResult(
    request.get<ApiResult<UserListItem>>(`/api/users/${userId}`)
  )
}

export function createAdmin(payload: CreateAdminPayload) {
  if (useMock) {
    return mockCreateAdmin(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<UserListItem>>('/api/users/admins', payload)
  )
}

export function createTeacher(payload: CreateTeacherPayload) {
  if (useMock) {
    return mockCreateTeacher(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<UserListItem>>('/api/users/teachers', payload)
  )
}

export function createStudent(payload: CreateStudentPayload) {
  if (useMock) {
    return mockCreateStudent(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<UserListItem>>('/api/users/students', payload)
  )
}

export async function updateUserStatus(userId: number, payload: UpdateUserStatusPayload) {
  if (useMock) {
    await mockUpdateUserStatus(getSessionToken(), userId, payload)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>(`/api/users/${userId}/status`, payload)
  )
}

export async function resetPassword(userId: number, payload: ResetPasswordPayload) {
  if (useMock) {
    await mockResetPassword(getSessionToken(), userId, payload)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>(`/api/users/${userId}/reset-password`, payload)
  )
}

export async function deleteStudent(userId: number) {
  if (useMock) {
    await mockDeleteStudent(getSessionToken(), userId)
    return
  }

  await unwrapResult(
    request.delete<ApiResult<null>>(`/api/users/students/${userId}`)
  )
}
