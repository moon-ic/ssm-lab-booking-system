import request from '@/api/request'
import {
  mockApproveReservation,
  mockCancelReservation,
  mockCreateReservation,
  mockGetReservationDetail,
  mockListReservations
} from '@/mock/reservations'
import type { ApiResult } from '@/types/auth'
import type {
  ApproveReservationPayload,
  CreateReservationPayload,
  ReservationItem,
  ReservationListQuery,
  ReservationListResponse
} from '@/types/reservation'

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

export function listReservations(query: ReservationListQuery) {
  if (useMock) {
    return mockListReservations(getSessionToken(), query)
  }

  return unwrapResult(
    request.get<ApiResult<ReservationListResponse>>('/api/reservations', { params: query })
  )
}

export function getReservationDetail(reservationId: number) {
  if (useMock) {
    return mockGetReservationDetail(getSessionToken(), reservationId)
  }

  return unwrapResult(
    request.get<ApiResult<ReservationItem>>(`/api/reservations/${reservationId}`)
  )
}

export function createReservation(payload: CreateReservationPayload) {
  if (useMock) {
    return mockCreateReservation(getSessionToken(), payload)
  }

  return unwrapResult(
    request.post<ApiResult<ReservationItem>>('/api/reservations', payload)
  )
}

export function approveReservation(reservationId: number, payload: ApproveReservationPayload) {
  if (useMock) {
    return mockApproveReservation(getSessionToken(), reservationId, payload)
  }

  return unwrapResult(
    request.put<ApiResult<ReservationItem>>(`/api/reservations/${reservationId}/approve`, payload)
  )
}

export async function cancelReservation(reservationId: number) {
  if (useMock) {
    await mockCancelReservation(getSessionToken(), reservationId)
    return
  }

  await unwrapResult(
    request.put<ApiResult<null>>(`/api/reservations/${reservationId}/cancel`)
  )
}
