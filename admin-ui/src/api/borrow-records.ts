import request from '@/api/request'
import {
  mockListBorrowRecords,
  mockListBorrowReminders,
  mockMarkBorrowRecordOverdue,
  mockPickupBorrowRecord,
  mockReturnBorrowRecord
} from '@/mock/borrow-records'
import type { ApiResult } from '@/types/auth'
import type {
  BorrowRecordItem,
  BorrowRecordListQuery,
  BorrowRecordListResponse,
  PickupPayload,
  ReturnPayload
} from '@/types/borrow-record'

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

export function listBorrowRecords(query: BorrowRecordListQuery) {
  if (useMock) {
    return mockListBorrowRecords(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<BorrowRecordListResponse>>('/api/borrow-records', { params: query })
  )
}

export function pickupBorrowRecord(recordId: number, payload: PickupPayload) {
  if (useMock) {
    return mockPickupBorrowRecord(getSessionToken(), recordId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<BorrowRecordItem>>(`/api/borrow-records/${recordId}/pickup`, payload)
  )
}

export function returnBorrowRecord(recordId: number, payload: ReturnPayload) {
  if (useMock) {
    return mockReturnBorrowRecord(getSessionToken(), recordId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<BorrowRecordItem>>(`/api/borrow-records/${recordId}/return`, payload)
  )
}

export async function markBorrowRecordOverdue(recordId: number) {
  if (useMock) {
    await mockMarkBorrowRecordOverdue(getSessionToken(), recordId)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>(`/api/borrow-records/${recordId}/overdue`)
  )
}

export function listBorrowReminders(type: 'ABOUT_TO_EXPIRE' | 'OVERDUE') {
  if (useMock) {
    return mockListBorrowReminders(getSessionToken(), type)
  }

  return unwrapResult(
    request.get<ApiResult<Array<BorrowRecordItem & { reminderType: string }>>>('/api/borrow-records/reminders', {
      params: { type }
    })
  )
}
