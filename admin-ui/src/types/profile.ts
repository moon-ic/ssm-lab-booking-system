import type { CurrentUser } from '@/types/auth'

export type BorrowStatus = 'PICKUP_PENDING' | 'BORROWING' | 'RETURNED' | 'OVERDUE'
export type ConfirmStatus = 'UNCONFIRMED' | 'CONFIRMED'
export type NotificationType =
  | 'FIRST_LOGIN_PASSWORD_CHANGE'
  | 'PASSWORD_RESET'
  | 'RESERVATION_EXPIRED'
  | 'BORROW_OVERDUE'
  | 'ABOUT_TO_EXPIRE_REMINDER'
  | 'OVERDUE_REMINDER'

export interface BorrowRecordItem {
  recordId: number
  reservationId: number
  userId: number
  deviceId: number
  deviceName: string
  status: BorrowStatus
  pickupTime: string
  expectedReturnTime: string
  returnTime?: string
  deviceCondition?: string
}

export interface MessageItem {
  messageId: number
  type: NotificationType
  title: string
  content: string
  relatedBizId?: number
  confirmStatus: ConfirmStatus
  createdAt: string
  confirmedAt?: string
}

export interface PageResult<T> {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
}

export interface BorrowRecordQuery {
  status?: BorrowStatus
  pageNum: number
  pageSize: number
}

export interface MessageQuery {
  confirmStatus?: ConfirmStatus
  type?: NotificationType
  pageNum: number
  pageSize: number
}

export interface ProfileSummary extends CurrentUser {}
