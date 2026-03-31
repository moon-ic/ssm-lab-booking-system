import request from '@/api/request'
import {
  mockConfirmMyMessage,
  mockListMessages,
  mockUnconfirmedSummary
} from '@/mock/messages'
import type { ApiResult } from '@/types/auth'
import type { MessageItem } from '@/types/profile'
import type { MessageListQuery, MessageListResponse, MessageSummary } from '@/types/message'

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

export function listMessages(query: MessageListQuery) {
  if (useMock) {
    return mockListMessages(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<MessageListResponse>>('/api/messages', { params: query })
  )
}

export function unconfirmedSummary() {
  if (useMock) {
    return mockUnconfirmedSummary(getSessionToken())
  }

  return unwrapResult(
    request.get<ApiResult<MessageSummary>>('/api/messages/unconfirmed-summary')
  )
}

export function confirmMessage(messageId: number) {
  if (useMock) {
    return mockConfirmMyMessage(getSessionToken(), messageId)
  }

  return unwrapResult(
    request.put<ApiResult<MessageItem>>(`/api/messages/${messageId}/confirm`)
  )
}
