import request from '@/api/request'
import {
  mockConfirmMyMessage,
  mockGetProfile,
  mockListMyBorrowRecords,
  mockListMyMessages
} from '@/mock/profile'
import type { ApiResult } from '@/types/auth'
import type {
  BorrowRecordItem,
  BorrowRecordQuery,
  MessageItem,
  MessageQuery,
  PageResult,
  ProfileSummary
} from '@/types/profile'

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

export function getProfile() {
  if (useMock) {
    return mockGetProfile(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<ProfileSummary>>('/api/profile')
  )
}

export function listMyBorrowRecords(query: BorrowRecordQuery) {
  if (useMock) {
    return mockListMyBorrowRecords(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<PageResult<BorrowRecordItem>>>('/api/profile/borrow-records', {
      params: query
    })
  )
}

export function listMyMessages(query: MessageQuery) {
  if (useMock) {
    return mockListMyMessages(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<PageResult<MessageItem>>>('/api/profile/messages', {
      params: query
    })
  )
}

export function confirmMyMessage(messageId: number) {
  if (useMock) {
    return mockConfirmMyMessage(getSessionToken(), messageId)
  }

  return unwrapResult(
    request.put<ApiResult<{ messageId: number; confirmStatus: string; confirmedAt: string }>>(
      `/api/profile/messages/${messageId}/confirm`
    )
  )
}
