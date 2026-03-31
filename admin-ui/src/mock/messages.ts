import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import type { ConfirmStatus, MessageItem, NotificationType } from '@/types/profile'
import type { MessageListQuery, MessageListResponse, MessageSummary } from '@/types/message'

const MESSAGES_STORAGE_KEY = 'mock-message-items'

const defaultMessages: MessageItem[] = [
  {
    messageId: 10001,
    type: 'FIRST_LOGIN_PASSWORD_CHANGE',
    title: 'First login password change required',
    content: 'Please change your initial password before continuing to use business modules.',
    relatedBizId: 4,
    confirmStatus: 'UNCONFIRMED',
    createdAt: '2026-03-30 12:00:00'
  },
  {
    messageId: 10002,
    type: 'ABOUT_TO_EXPIRE_REMINDER',
    title: 'Borrow record due soon',
    content: 'Your Canon EOS Camera record will expire within 3 days.',
    relatedBizId: 9002,
    confirmStatus: 'UNCONFIRMED',
    createdAt: '2026-03-31 09:20:00'
  },
  {
    messageId: 10003,
    type: 'PASSWORD_RESET',
    title: 'Password reset notice',
    content: 'An administrator reset your password. Please sign in and change it promptly.',
    relatedBizId: 5,
    confirmStatus: 'CONFIRMED',
    createdAt: '2026-03-20 15:00:00',
    confirmedAt: '2026-03-20 16:10:00'
  },
  {
    messageId: 10004,
    type: 'OVERDUE_REMINDER',
    title: 'Borrow record overdue',
    content: 'Projector A has exceeded the expected return time. Please process it as soon as possible.',
    relatedBizId: 9001,
    confirmStatus: 'UNCONFIRMED',
    createdAt: '2026-03-31 13:00:00'
  }
]

function readMessages() {
  const raw = localStorage.getItem(MESSAGES_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(MESSAGES_STORAGE_KEY, JSON.stringify(defaultMessages))
    return structuredClone(defaultMessages)
  }

  try {
    return JSON.parse(raw) as MessageItem[]
  } catch {
    localStorage.setItem(MESSAGES_STORAGE_KEY, JSON.stringify(defaultMessages))
    return structuredClone(defaultMessages)
  }
}

function writeMessages(items: MessageItem[]) {
  localStorage.setItem(MESSAGES_STORAGE_KEY, JSON.stringify(items))
}

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function paginate(list: MessageItem[], pageNum: number, pageSize: number): MessageListResponse {
  const fromIndex = Math.min((pageNum - 1) * pageSize, list.length)
  const toIndex = Math.min(fromIndex + pageSize, list.length)

  return {
    list: list.slice(fromIndex, toIndex),
    pageNum,
    pageSize,
    total: list.length
  }
}

function filterByOwnership(userId: number, message: MessageItem) {
  if (message.type === 'FIRST_LOGIN_PASSWORD_CHANGE' || message.type === 'PASSWORD_RESET') {
    return message.relatedBizId === userId
  }
  return true
}

function filterMessages(items: MessageItem[], query: { type?: NotificationType; confirmStatus?: ConfirmStatus }) {
  return items
    .filter((item) => !query.type || item.type === query.type)
    .filter((item) => !query.confirmStatus || item.confirmStatus === query.confirmStatus)
}

export async function mockListMyMessages(token: string, query: MessageListQuery) {
  await wait(200)
  const user = currentUser(token)
  const list = filterMessages(
    readMessages().filter((item) => filterByOwnership(user.userId, item)),
    query
  )
  return paginate(list, query.pageNum, query.pageSize)
}

export async function mockConfirmMyMessage(token: string, messageId: number) {
  await wait(160)
  const user = currentUser(token)
  const items = readMessages()
  const message = items.find((item) => item.messageId === messageId)

  if (!message || !filterByOwnership(user.userId, message)) {
    throw new Error('Message not found')
  }

  message.confirmStatus = 'CONFIRMED'
  message.confirmedAt = '2026-03-31 18:00:00'
  writeMessages(items)

  return {
    ...message
  }
}

export async function mockListMessages(token: string, query: MessageListQuery) {
  await wait(220)
  const user = currentUser(token)
  if (user.roleCode === 'STUDENT') {
    throw new Error('Students cannot access the full message center')
  }

  let list = readMessages()
  list = filterMessages(list, query)

  if (query.userId) {
    list = list.filter((item) => filterByOwnership(query.userId!, item))
  }

  if (user.roleCode === 'TEACHER') {
    const ownedStudentIds = readMockUsers()
      .filter((item) => !item.deleted && item.roleCode === 'STUDENT' && item.managerId === user.userId)
      .map((item) => item.userId)
    list = list.filter((item) => {
      if (item.type === 'FIRST_LOGIN_PASSWORD_CHANGE' || item.type === 'PASSWORD_RESET') {
        return ownedStudentIds.includes(Number(item.relatedBizId))
      }
      return true
    })
  }

  return paginate(list, query.pageNum, query.pageSize)
}

export async function mockUnconfirmedSummary(token: string): Promise<MessageSummary> {
  await wait(120)
  const user = currentUser(token)
  const unread = readMessages().filter((item) => filterByOwnership(user.userId, item) && item.confirmStatus === 'UNCONFIRMED')

  return {
    total: unread.length,
    aboutToExpireCount: unread.filter((item) => item.type === 'ABOUT_TO_EXPIRE_REMINDER').length,
    overdueCount: unread.filter((item) => item.type === 'OVERDUE_REMINDER').length,
    firstLoginCount: unread.filter((item) => item.type === 'FIRST_LOGIN_PASSWORD_CHANGE').length,
    passwordResetCount: unread.filter((item) => item.type === 'PASSWORD_RESET').length
  }
}
