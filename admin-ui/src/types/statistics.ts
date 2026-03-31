export type RankScope = 'TOTAL' | 'HALF_YEAR' | 'MONTH'

export interface StatisticsQuery {
  rankScope?: RankScope
  topN?: number
  startDate?: string
  endDate?: string
}

export interface StatisticsOverview {
  deviceTotal: number
  availableDeviceTotal: number
  borrowingTotal: number
  pendingReservationTotal: number
  pendingRepairTotal: number
}

export interface HotDeviceStatItem {
  deviceId: number
  deviceName: string
  deviceCode: string
  imageUrl?: string
  borrowCount: number
  rankScope: RankScope
}

export interface DamageDeviceStatItem {
  deviceId: number
  deviceName: string
  deviceCode: string
  imageUrl?: string
  category: string
  damageCount: number
  status: string
  rankScope: RankScope
}

export interface UserViolationStatItem {
  userId: number
  name: string
  jobNoOrStudentNo: string
  overdueCount: number
  damageCount: number
  violationCount: number
  rankScope: RankScope
}
