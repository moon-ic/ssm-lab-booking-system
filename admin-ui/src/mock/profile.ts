import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import { listMockBorrowRecords } from '@/mock/borrow-records'
import {
  mockConfirmMyMessage as mockConfirmMyMessageShared,
  mockListMyMessages as mockListMyMessagesShared
} from '@/mock/messages'
import type { BorrowRecordItem, BorrowRecordQuery, MessageQuery, PageResult, ProfileSummary } from '@/types/profile'

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
  const filtered = listMockBorrowRecords()
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
