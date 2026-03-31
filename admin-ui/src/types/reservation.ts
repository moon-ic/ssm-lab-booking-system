export type ReservationStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'PICKUP_PENDING'
  | 'EXPIRED'
  | 'CANCELLED'

export interface ReservationItem {
  reservationId: number
  deviceId: number
  deviceName: string
  applicantId: number
  applicantName: string
  startTime: string
  endTime: string
  purpose: string
  status: ReservationStatus
  createdAt?: string
  reviewComment?: string
  reviewerId?: number | null
  reviewerName?: string | null
}

export interface ReservationListQuery {
  status?: ReservationStatus
  deviceId?: number
  applicantId?: number
  pageNum: number
  pageSize: number
}

export interface ReservationListResponse {
  list: ReservationItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface CreateReservationPayload {
  deviceId: number
  startTime: string
  endTime: string
  purpose: string
}

export interface ApproveReservationPayload {
  action: 'APPROVE' | 'REJECT'
  comment?: string
}
