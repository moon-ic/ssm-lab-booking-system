export type BorrowStatus = 'PICKUP_PENDING' | 'BORROWING' | 'RETURNED' | 'OVERDUE'

export interface BorrowRecordItem {
  recordId: number
  reservationId: number
  userId: number
  userName: string
  deviceId: number
  deviceName: string
  status: BorrowStatus
  pickupTime?: string
  expectedReturnTime: string
  returnTime?: string
  deviceCondition?: string
}

export interface BorrowRecordListQuery {
  status?: BorrowStatus
  userId?: number
  deviceId?: number
  pageNum: number
  pageSize: number
}

export interface BorrowRecordListResponse {
  list: BorrowRecordItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface PickupPayload {
  pickupTime: string
}

export interface ReturnPayload {
  returnTime: string
  deviceCondition: string
}
