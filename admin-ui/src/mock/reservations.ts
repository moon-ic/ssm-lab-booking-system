import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import { mockGetDeviceDetail, mockListDevices } from '@/mock/devices'
import { createPendingBorrowRecordFromReservation } from '@/mock/borrow-records'
import type {
  ApproveReservationPayload,
  CreateReservationPayload,
  ReservationItem,
  ReservationListQuery,
  ReservationListResponse
} from '@/types/reservation'

const RESERVATIONS_STORAGE_KEY = 'mock-reservation-items'

const defaultReservations: ReservationItem[] = [
  {
    reservationId: 7001,
    deviceId: 1001,
    deviceName: 'Canon EOS Camera',
    applicantId: 4,
    applicantName: 'Student Wang',
    startTime: '2026-04-02 09:00',
    endTime: '2026-04-04 18:00',
    purpose: 'Campus media recording',
    status: 'PENDING',
    createdAt: '2026-03-31 10:00'
  },
  {
    reservationId: 7002,
    deviceId: 1002,
    deviceName: 'Projector A',
    applicantId: 5,
    applicantName: 'Student Zhao',
    startTime: '2026-04-06 08:00',
    endTime: '2026-04-06 17:00',
    purpose: 'Presentation rehearsal',
    status: 'PICKUP_PENDING',
    createdAt: '2026-03-29 11:20',
    reviewComment: 'Approved for class use',
    reviewerId: 3,
    reviewerName: 'Teacher Li'
  }
]

function readReservations() {
  const raw = localStorage.getItem(RESERVATIONS_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(RESERVATIONS_STORAGE_KEY, JSON.stringify(defaultReservations))
    return structuredClone(defaultReservations)
  }

  try {
    return JSON.parse(raw) as ReservationItem[]
  } catch {
    localStorage.setItem(RESERVATIONS_STORAGE_KEY, JSON.stringify(defaultReservations))
    return structuredClone(defaultReservations)
  }
}

function writeReservations(items: ReservationItem[]) {
  localStorage.setItem(RESERVATIONS_STORAGE_KEY, JSON.stringify(items))
}

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function visibleTo(token: string, item: ReservationItem) {
  const user = currentUser(token)
  if (user.roleCode === 'SUPER_ADMIN' || user.roleCode === 'ADMIN') {
    return true
  }
  if (user.roleCode === 'TEACHER') {
    const applicant = readMockUsers().find((candidate) => candidate.userId === item.applicantId)
    return applicant?.roleCode === 'STUDENT' && applicant.managerId === user.userId
  }
  return item.applicantId === user.userId
}

function nextReservationId(items: ReservationItem[]) {
  return items.reduce((max, item) => Math.max(max, item.reservationId), 0) + 1
}

export async function mockListReservations(token: string, query: ReservationListQuery): Promise<ReservationListResponse> {
  await wait()

  const filtered = readReservations()
    .filter((item) => visibleTo(token, item))
    .filter((item) => !query.status || item.status === query.status)
    .filter((item) => !query.deviceId || item.deviceId === query.deviceId)
    .filter((item) => !query.applicantId || item.applicantId === query.applicantId)

  const pageNum = query.pageNum || 1
  const pageSize = query.pageSize || 10
  const fromIndex = Math.min((pageNum - 1) * pageSize, filtered.length)
  const toIndex = Math.min(fromIndex + pageSize, filtered.length)

  return {
    list: filtered.slice(fromIndex, toIndex),
    pageNum,
    pageSize,
    total: filtered.length
  }
}

export async function mockGetReservationDetail(token: string, reservationId: number) {
  await wait(180)
  const item = readReservations().find((entry) => entry.reservationId === reservationId)
  if (!item) {
    throw new Error('Reservation not found')
  }
  if (!visibleTo(token, item)) {
    throw new Error('No permission to access this reservation')
  }
  return item
}

export async function mockCreateReservation(token: string, payload: CreateReservationPayload) {
  await wait()
  const user = currentUser(token)
  if (user.roleCode !== 'STUDENT') {
    throw new Error('Only students can create reservations')
  }

  const device = await mockGetDeviceDetail(payload.deviceId)
  const items = readReservations()
  const item: ReservationItem = {
    reservationId: nextReservationId(items),
    deviceId: payload.deviceId,
    deviceName: device.deviceName,
    applicantId: user.userId,
    applicantName: user.name,
    startTime: payload.startTime,
    endTime: payload.endTime,
    purpose: payload.purpose,
    status: 'PENDING',
    createdAt: '2026-03-31 18:30'
  }
  items.push(item)
  writeReservations(items)
  return item
}

export async function mockApproveReservation(token: string, reservationId: number, payload: ApproveReservationPayload) {
  await wait(220)
  const user = currentUser(token)
  if (!['SUPER_ADMIN', 'ADMIN', 'TEACHER'].includes(user.roleCode)) {
    throw new Error('No permission to review reservations')
  }

  const items = readReservations()
  const item = items.find((entry) => entry.reservationId === reservationId)
  if (!item) {
    throw new Error('Reservation not found')
  }
  if (!visibleTo(token, item)) {
    throw new Error('No permission to review this reservation')
  }
  if (payload.action === 'REJECT' && !payload.comment?.trim()) {
    throw new Error('Reject comment is required')
  }

  if (payload.action === 'APPROVE') {
    if (user.roleCode === 'TEACHER') {
      if (item.status !== 'PENDING') {
        throw new Error('Teacher can only approve pending reservations')
      }
      item.status = 'APPROVED'
      item.reviewComment = payload.comment?.trim() || ''
      item.reviewerId = user.userId
      item.reviewerName = user.name
    } else {
      if (item.status !== 'APPROVED') {
        throw new Error('Admin can only approve teacher-approved reservations')
      }
      item.status = 'PICKUP_PENDING'
      item.reviewComment = payload.comment?.trim() || ''
      item.reviewerId = user.userId
      item.reviewerName = user.name
      createPendingBorrowRecordFromReservation(item)
    }
  } else {
    if (user.roleCode === 'TEACHER' && item.status !== 'PENDING') {
      throw new Error('Teacher can only reject pending reservations')
    }
    if (!['PENDING', 'APPROVED'].includes(item.status)) {
      throw new Error('Only pending reservations can be reviewed')
    }
    item.status = 'REJECTED'
    item.reviewComment = payload.comment?.trim() || ''
    item.reviewerId = user.userId
    item.reviewerName = user.name
  }
  writeReservations(items)
  return item
}

export async function mockCancelReservation(token: string, reservationId: number) {
  await wait(180)
  const user = currentUser(token)
  const items = readReservations()
  const item = items.find((entry) => entry.reservationId === reservationId)
  if (!item) {
    throw new Error('Reservation not found')
  }
  if (item.applicantId !== user.userId) {
    throw new Error('Only the applicant can cancel this reservation')
  }
  if (!['PENDING', 'APPROVED'].includes(item.status)) {
    throw new Error('Only pending reservations can be cancelled')
  }
  item.status = 'CANCELLED'
  writeReservations(items)
}
