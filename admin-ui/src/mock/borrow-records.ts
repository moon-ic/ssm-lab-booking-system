import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import type {
  BorrowRecordItem,
  BorrowRecordListQuery,
  BorrowRecordListResponse,
  PickupPayload,
  ReturnPayload
} from '@/types/borrow-record'

const BORROW_RECORDS_STORAGE_KEY = 'mock-borrow-record-items'

const defaultBorrowRecords: BorrowRecordItem[] = [
  {
    recordId: 9001,
    reservationId: 7002,
    userId: 5,
    userName: 'Student Zhao',
    deviceId: 1002,
    deviceName: 'Projector A',
    status: 'PICKUP_PENDING',
    expectedReturnTime: '2026-04-06 17:00'
  },
  {
    recordId: 9002,
    reservationId: 6801,
    userId: 4,
    userName: 'Student Wang',
    deviceId: 1001,
    deviceName: 'Canon EOS Camera',
    status: 'BORROWING',
    pickupTime: '2026-03-28 09:30',
    expectedReturnTime: '2026-04-02 18:00'
  }
]

function readBorrowRecords() {
  const raw = localStorage.getItem(BORROW_RECORDS_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(BORROW_RECORDS_STORAGE_KEY, JSON.stringify(defaultBorrowRecords))
    return structuredClone(defaultBorrowRecords)
  }

  try {
    return JSON.parse(raw) as BorrowRecordItem[]
  } catch {
    localStorage.setItem(BORROW_RECORDS_STORAGE_KEY, JSON.stringify(defaultBorrowRecords))
    return structuredClone(defaultBorrowRecords)
  }
}

function writeBorrowRecords(items: BorrowRecordItem[]) {
  localStorage.setItem(BORROW_RECORDS_STORAGE_KEY, JSON.stringify(items))
}

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function visibleTo(token: string, item: BorrowRecordItem) {
  const user = currentUser(token)
  if (user.roleCode === 'SUPER_ADMIN' || user.roleCode === 'ADMIN') {
    return true
  }
  if (user.roleCode === 'TEACHER') {
    const applicant = readMockUsers().find((candidate) => candidate.userId === item.userId)
    return applicant?.roleCode === 'STUDENT' && applicant.managerId === user.userId
  }
  return item.userId === user.userId
}

function nextRecordId(items: BorrowRecordItem[]) {
  return items.reduce((max, item) => Math.max(max, item.recordId), 0) + 1
}

function formatCurrentMinute() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

export function createPendingBorrowRecordFromReservation(reservation: {
  reservationId: number
  applicantId: number
  applicantName: string
  deviceId: number
  deviceName: string
  endTime: string
}) {
  const items = readBorrowRecords()
  const existing = items.find((item) => item.reservationId === reservation.reservationId)
  if (existing) {
    return existing
  }

  const record: BorrowRecordItem = {
    recordId: nextRecordId(items),
    reservationId: reservation.reservationId,
    userId: reservation.applicantId,
    userName: reservation.applicantName,
    deviceId: reservation.deviceId,
    deviceName: reservation.deviceName,
    status: 'PICKUP_PENDING',
    expectedReturnTime: reservation.endTime
  }
  items.push(record)
  writeBorrowRecords(items)
  return record
}

export async function mockListBorrowRecords(token: string, query: BorrowRecordListQuery): Promise<BorrowRecordListResponse> {
  await wait()

  const filtered = readBorrowRecords()
    .filter((item) => visibleTo(token, item))
    .filter((item) => !query.status || item.status === query.status)
    .filter((item) => !query.userId || item.userId === query.userId)
    .filter((item) => !query.deviceId || item.deviceId === query.deviceId)

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

export async function mockPickupBorrowRecord(token: string, recordId: number, _payload: PickupPayload) {
  await wait(180)
  const user = currentUser(token)
  if (user.roleCode !== 'STUDENT') {
    throw new Error('Only students can confirm pickup')
  }

  const items = readBorrowRecords()
  const item = items.find((entry) => entry.recordId === recordId)
  if (!item) {
    throw new Error('Borrow record not found')
  }
  if (item.userId !== user.userId) {
    throw new Error('Only the owner can confirm pickup')
  }
  if (item.status !== 'PICKUP_PENDING') {
    throw new Error('Only pickup-pending records can be picked up')
  }

  item.status = 'BORROWING'
  item.pickupTime = formatCurrentMinute()
  writeBorrowRecords(items)
  return item
}

export async function mockReturnBorrowRecord(token: string, recordId: number, payload: ReturnPayload) {
  await wait(180)
  const user = currentUser(token)
  if (user.roleCode !== 'STUDENT') {
    throw new Error('Only students can return devices')
  }

  const items = readBorrowRecords()
  const item = items.find((entry) => entry.recordId === recordId)
  if (!item) {
    throw new Error('Borrow record not found')
  }
  if (item.userId !== user.userId) {
    throw new Error('Only the owner can return this device')
  }
  if (!['BORROWING', 'OVERDUE'].includes(item.status)) {
    throw new Error('Only active borrow records can be returned')
  }

  item.returnTime = payload.returnTime
  item.deviceCondition = payload.deviceCondition.toUpperCase()
  item.status = 'RETURNED'
  writeBorrowRecords(items)
  return item
}

export async function mockMarkBorrowRecordOverdue(token: string, recordId: number) {
  await wait(150)
  const user = currentUser(token)
  if (!['SUPER_ADMIN', 'ADMIN'].includes(user.roleCode)) {
    throw new Error('Only admin can mark overdue')
  }

  const items = readBorrowRecords()
  const item = items.find((entry) => entry.recordId === recordId)
  if (!item) {
    throw new Error('Borrow record not found')
  }
  item.status = 'OVERDUE'
  writeBorrowRecords(items)
}

export async function mockListBorrowReminders(token: string, type: 'ABOUT_TO_EXPIRE' | 'OVERDUE') {
  await wait(160)
  const user = currentUser(token)
  if (!['SUPER_ADMIN', 'ADMIN'].includes(user.roleCode)) {
    throw new Error('Only admin can view reminders')
  }

  return readBorrowRecords()
    .filter((item) => (type === 'ABOUT_TO_EXPIRE' ? item.status === 'BORROWING' : item.status === 'OVERDUE'))
    .map((item) => ({
      ...item,
      reminderType: type
    }))
}
