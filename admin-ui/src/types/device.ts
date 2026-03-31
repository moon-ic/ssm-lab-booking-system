export type DeviceStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'BORROWED'
  | 'REPAIRING'
  | 'DAMAGED'
  | 'DISABLED'

export interface DeviceItem {
  deviceId: number
  deviceName: string
  deviceCode: string
  category: string
  status: DeviceStatus
  location: string
  imageUrl?: string
  description?: string
}

export interface DeviceListQuery {
  keyword?: string
  category?: string
  status?: DeviceStatus
  pageNum: number
  pageSize: number
}

export interface DeviceListResponse {
  list: DeviceItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface SaveDevicePayload {
  deviceName: string
  deviceCode: string
  category: string
  location: string
  imageUrl?: string
  description?: string
}

export interface UpdateDeviceStatusPayload {
  status: DeviceStatus
}
