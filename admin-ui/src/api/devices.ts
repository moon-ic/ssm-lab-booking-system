import request from '@/api/request'
import {
  deviceStatusOptions,
  mockCreateDevice,
  mockGetDeviceDetail,
  mockListDevices,
  mockUpdateDevice,
  mockUpdateDeviceStatus
} from '@/mock/devices'
import type { ApiResult } from '@/types/auth'
import type {
  DeviceItem,
  DeviceListQuery,
  DeviceListResponse,
  SaveDevicePayload,
  UpdateDeviceStatusPayload
} from '@/types/device'

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

export function listDevices(query: DeviceListQuery) {
  if (useMock) {
    return mockListDevices(query)
  }

  return unwrapResult(
    request.get<ApiResult<DeviceListResponse>>('/api/devices', { params: query })
  )
}

export function getDeviceDetail(deviceId: number) {
  if (useMock) {
    return mockGetDeviceDetail(deviceId)
  }

  return unwrapResult(
    request.get<ApiResult<DeviceItem>>(`/api/devices/${deviceId}`)
  )
}

export function createDevice(payload: SaveDevicePayload) {
  if (useMock) {
    return mockCreateDevice(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<DeviceItem>>('/api/devices', payload)
  )
}

export function updateDevice(deviceId: number, payload: SaveDevicePayload) {
  if (useMock) {
    return mockUpdateDevice(getSessionToken(), deviceId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<DeviceItem>>(`/api/devices/${deviceId}`, payload)
  )
}

export async function updateDeviceStatus(deviceId: number, payload: UpdateDeviceStatusPayload) {
  if (useMock) {
    await mockUpdateDeviceStatus(getSessionToken(), deviceId, payload)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>(`/api/devices/${deviceId}/status`, payload)
  )
}

export { deviceStatusOptions }
