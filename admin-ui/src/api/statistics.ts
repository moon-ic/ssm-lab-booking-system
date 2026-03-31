import request from '@/api/request'
import {
  mockDeviceDamageStatistics,
  mockHotDevices,
  mockStatisticsOverview,
  mockUserViolationStatistics
} from '@/mock/statistics'
import type { ApiResult } from '@/types/auth'
import type {
  DamageDeviceStatItem,
  HotDeviceStatItem,
  StatisticsOverview,
  StatisticsQuery,
  UserViolationStatItem
} from '@/types/statistics'

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

export function statisticsOverview() {
  if (useMock) {
    return mockStatisticsOverview(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<StatisticsOverview>>('/api/statistics/overview')
  )
}

export function hotDevices(query: StatisticsQuery) {
  if (useMock) {
    return mockHotDevices(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<HotDeviceStatItem[]>>('/api/statistics/devices/hot', { params: query })
  )
}

export function deviceDamageStatistics(query: StatisticsQuery) {
  if (useMock) {
    return mockDeviceDamageStatistics(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<DamageDeviceStatItem[]>>('/api/statistics/devices/damage', { params: query })
  )
}

export function userViolationStatistics(query: StatisticsQuery) {
  if (useMock) {
    return mockUserViolationStatistics(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<UserViolationStatItem[]>>('/api/statistics/users/violations', { params: query })
  )
}
