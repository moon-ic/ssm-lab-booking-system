import request from '@/api/request'
import {
  mockCreateRepair,
  mockGetRepairDetail,
  mockListRepairs,
  mockUpdateRepairStatus
} from '@/mock/repairs'
import type { ApiResult } from '@/types/auth'
import type {
  CreateRepairPayload,
  RepairItem,
  RepairListQuery,
  RepairListResponse,
  UpdateRepairStatusPayload
} from '@/types/repair'

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

export function listRepairs(query: RepairListQuery) {
  if (useMock) {
    return mockListRepairs(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<RepairListResponse>>('/api/repairs', { params: query })
  )
}

export function getRepairDetail(repairId: number) {
  if (useMock) {
    return mockGetRepairDetail(getSessionToken(), repairId)
  }

  return unwrapResult(
    request.get<ApiResult<RepairItem>>(`/api/repairs/${repairId}`)
  )
}

export function createRepair(payload: CreateRepairPayload) {
  if (useMock) {
    return mockCreateRepair(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<RepairItem>>('/api/repairs', payload)
  )
}

export function updateRepairStatus(repairId: number, payload: UpdateRepairStatusPayload) {
  if (useMock) {
    return mockUpdateRepairStatus(getSessionToken(), repairId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<RepairItem>>(`/api/repairs/${repairId}/status`, payload)
  )
}
