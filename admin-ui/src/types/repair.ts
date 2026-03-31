export type RepairStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'UNREPAIRABLE'

export interface RepairItem {
  repairId: number
  deviceId: number
  deviceName: string
  applicantId: number
  applicantName: string
  description: string
  status: RepairStatus
  comment?: string
  createdAt: string
  updatedAt: string
}

export interface RepairListQuery {
  status?: RepairStatus
  deviceId?: number
  applicantId?: number
  pageNum: number
  pageSize: number
}

export interface RepairListResponse {
  list: RepairItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface CreateRepairPayload {
  deviceId: number
  description: string
}

export interface UpdateRepairStatusPayload {
  status: RepairStatus
  comment?: string
}
