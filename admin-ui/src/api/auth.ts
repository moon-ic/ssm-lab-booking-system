import request from '@/api/request'
import { mockChangePassword, mockGetCurrentUser, mockLogin } from '@/mock/auth'
import type {
  ApiResult,
  ChangePasswordRequest,
  CurrentUser,
  LoginRequest,
  LoginResponse
} from '@/types/auth'

const useMock = import.meta.env.VITE_ENABLE_MOCK !== 'false'

async function unwrapResult<T>(promise: Promise<{ data: ApiResult<T> }>) {
  const response = await promise
  if (response.data.code !== 0) {
    throw new Error(response.data.message || '请求失败')
  }
  return response.data.data
}

export function login(payload: LoginRequest) {
  if (useMock) {
    return mockLogin(payload)
  }

  return unwrapResult(
    request.post<ApiResult<LoginResponse>>('/api/auth/login', payload)
  )
}

export function getCurrentUser(token: string) {
  if (useMock) {
    return mockGetCurrentUser(token)
  }

  return unwrapResult(
    request.get<ApiResult<CurrentUser>>('/api/auth/me')
  )
}

export async function changePassword(token: string, payload: ChangePasswordRequest) {
  if (useMock) {
    await mockChangePassword(token, payload)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>('/api/auth/password', payload)
  )
}
