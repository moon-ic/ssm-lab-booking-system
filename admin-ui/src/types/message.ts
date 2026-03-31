import type { ConfirmStatus, MessageItem, NotificationType } from '@/types/profile'

export interface MessageListQuery {
  userId?: number
  type?: NotificationType
  confirmStatus?: ConfirmStatus
  pageNum: number
  pageSize: number
}

export interface MessageListResponse {
  list: MessageItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface MessageSummary {
  total: number
  aboutToExpireCount: number
  overdueCount: number
  firstLoginCount: number
  passwordResetCount?: number
}
