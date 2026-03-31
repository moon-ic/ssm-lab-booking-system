import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import {
  mockConfirmMyMessage as mockConfirmMyMessageShared,
  mockListMyMessages as mockListMyMessagesShared
} from '@/mock/messages'
import type { BorrowRecordItem, BorrowRecordQuery, MessageQuery, PageResult, ProfileSummary } from '@/types/profile'

const borrowRecords: BorrowRecordItem[] = [
  {
    recordId: 9001,
    reservationId: 8001,
    userId: 4,
    deviceId: 1001,
    deviceName: 'Canon EOS Camera',
    status: 'BORROWING',
    pickupTime: '2026-03-28 09:30:00',
    expectedReturnTime: '2026-04-02 18:00:00',
    deviceCondition: 'Good'
  },
  {
    recordId: 9002,
    reservationId: 8002,
    userId: 4,
    deviceId: 1002,
    deviceName: 'Projector A',
    status: 'RETURNED',
    pickupTime: '2026-03-15 10:00:00',
    expectedReturnTime: '2026-03-20 18:00:00',
    returnTime: '2026-03-19 15:20:00',
    deviceCondition: 'Normal'
  },
  {
    recordId: 9003,
    reservationId: 8003,
    userId: 5,
    deviceId: 1003,
    deviceName: 'Microscope Set',
    status: 'PICKUP_PENDING',
    pickupTime: '2026-04-01 08:00:00',
    expectedReturnTime: '2026-04-05 18:00:00'
  }
]

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function paginate<T>(list: T[], pageNum: number, pageSize: number): PageResult<T> {
  const fromIndex = Math.min((pageNum - 1) * pageSize, list.length)
  const toIndex = Math.min(fromIndex + pageSize, list.length)

  return {
    list: list.slice(fromIndex, toIndex),
    pageNum,
    pageSize,
    total: list.length
  }
}

export async function mockGetProfile(token: string): Promise<ProfileSummary> {
  await wait(180)
  const user = currentUser(token)

  return {
    userId: user.userId,
    name: user.name,
    account: user.account,
    jobNoOrStudentNo: user.loginId,
    roleCode: user.roleCode,
    status: user.status,
    creditScore: user.creditScore,
    firstLoginRequired: user.firstLoginRequired
  }
}

export async function mockListMyBorrowRecords(token: string, query: BorrowRecordQuery) {
  await wait(220)
  const user = currentUser(token)
  const filtered = borrowRecords
    .filter((item) => item.userId === user.userId)
    .filter((item) => !query.status || item.status === query.status)

  return paginate(filtered, query.pageNum, query.pageSize)
}

export async function mockListMyMessages(token: string, query: MessageQuery) {
  return mockListMyMessagesShared(token, query)
}

export async function mockConfirmMyMessage(token: string, messageId: number) {
  return mockConfirmMyMessageShared(token, messageId)
}
