import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import type {
  DeviceItem,
  DeviceListQuery,
  DeviceListResponse,
  DeviceStatus,
  SaveDevicePayload,
  UpdateDeviceStatusPayload
} from '@/types/device'

const DEVICES_STORAGE_KEY = 'mock-device-items'

const defaultDevices: DeviceItem[] = [
  {
    deviceId: 1001,
    deviceName: 'Canon EOS Camera',
    deviceCode: 'EQ-2026-1001',
    category: 'Camera',
    status: 'AVAILABLE',
    location: 'Lab A',
    imageUrl: 'https://dummyimage.com/240x160/e2e8f0/0f172a&text=Camera',
    description: 'Portable DSLR camera for media and event projects.'
  },
  {
    deviceId: 1002,
    deviceName: 'Projector A',
    deviceCode: 'EQ-2026-1002',
    category: 'Projector',
    status: 'BORROWED',
    location: 'Room 301',
    imageUrl: 'https://dummyimage.com/240x160/cbd5e1/0f172a&text=Projector',
    description: 'Ceiling projector used for classroom demonstrations.'
  },
  {
    deviceId: 1003,
    deviceName: 'Microscope Set',
    deviceCode: 'EQ-2026-1003',
    category: 'Microscope',
    status: 'REPAIRING',
    location: 'Lab C',
    imageUrl: 'https://dummyimage.com/240x160/dbeafe/0f172a&text=Microscope',
    description: 'Precision microscope set for biology experiments.'
  }
]

function readDevices() {
  const raw = localStorage.getItem(DEVICES_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(DEVICES_STORAGE_KEY, JSON.stringify(defaultDevices))
    return structuredClone(defaultDevices)
  }

  try {
    return JSON.parse(raw) as DeviceItem[]
  } catch {
    localStorage.setItem(DEVICES_STORAGE_KEY, JSON.stringify(defaultDevices))
    return structuredClone(defaultDevices)
  }
}

function writeDevices(devices: DeviceItem[]) {
  localStorage.setItem(DEVICES_STORAGE_KEY, JSON.stringify(devices))
}

function nextDeviceId(devices: DeviceItem[]) {
  return devices.reduce((max, item) => Math.max(max, item.deviceId), 0) + 1
}

function currentUserRole(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user.roleCode
}

function ensureAdmin(token: string) {
  const roleCode = currentUserRole(token)
  if (roleCode !== 'SUPER_ADMIN' && roleCode !== 'ADMIN') {
    throw new Error('Only admin or super admin can manage devices')
  }
}

export async function mockListDevices(query: DeviceListQuery): Promise<DeviceListResponse> {
  await wait()

  const filtered = readDevices()
    .filter((item) => !query.keyword || [item.deviceName, item.deviceCode, item.category, item.location, item.description ?? '']
      .join(' ')
      .toLowerCase()
      .includes(query.keyword.toLowerCase()))
    .filter((item) => !query.category || item.category === query.category)
    .filter((item) => !query.status || item.status === query.status)

  const pageNum = query.pageNum || 1
  const pageSize = query.pageSize || 10
  const fromIndex = Math.min((pageNum - 1) * pageSize, filtered.length)
  const toIndex = Math.min(fromIndex + pageSize, filtered.length)

  return {
    list: filtered.slice(fromIndex, toIndex),
    pageNum,
    pageSize,
    total: filtered.length
  }
}

export async function mockGetDeviceDetail(deviceId: number) {
  await wait(180)
  const device = readDevices().find((item) => item.deviceId === deviceId)
  if (!device) {
    throw new Error('Device not found')
  }
  return device
}

export async function mockCreateDevice(token: string, payload: SaveDevicePayload) {
  await wait()
  ensureAdmin(token)

  const devices = readDevices()
  if (devices.some((item) => item.deviceCode === payload.deviceCode)) {
    throw new Error('Device code already exists')
  }

  const device: DeviceItem = {
    deviceId: nextDeviceId(devices),
    deviceName: payload.deviceName,
    deviceCode: payload.deviceCode,
    category: payload.category,
    status: 'AVAILABLE',
    location: payload.location,
    imageUrl: payload.imageUrl,
    description: payload.description
  }

  devices.push(device)
  writeDevices(devices)
  return device
}

export async function mockUpdateDevice(token: string, deviceId: number, payload: SaveDevicePayload) {
  await wait()
  ensureAdmin(token)

  const devices = readDevices()
  const device = devices.find((item) => item.deviceId === deviceId)
  if (!device) {
    throw new Error('Device not found')
  }

  if (devices.some((item) => item.deviceId !== deviceId && item.deviceCode === payload.deviceCode)) {
    throw new Error('Device code already exists')
  }

  device.deviceName = payload.deviceName
  device.deviceCode = payload.deviceCode
  device.category = payload.category
  device.location = payload.location
  device.imageUrl = payload.imageUrl
  device.description = payload.description

  writeDevices(devices)
  return device
}

export async function mockUpdateDeviceStatus(token: string, deviceId: number, payload: UpdateDeviceStatusPayload) {
  await wait(180)
  ensureAdmin(token)

  const devices = readDevices()
  const device = devices.find((item) => item.deviceId === deviceId)
  if (!device) {
    throw new Error('Device not found')
  }

  device.status = payload.status
  writeDevices(devices)
}

export async function mockImportDevice(
  token: string,
  payload: {
    deviceName: string
    category?: string
    location?: string
    description?: string
    imageName: string
  }
) {
  await wait()
  ensureAdmin(token)

  if (!payload.deviceName.trim()) {
    throw new Error('Device name is required')
  }

  if (!payload.imageName.trim()) {
    throw new Error('Device image is required')
  }

  const devices = readDevices()
  const deviceId = nextDeviceId(devices)
  const safeImageName = payload.imageName.replace(/[^A-Za-z0-9._-]/g, '_')

  const device: DeviceItem = {
    deviceId,
    deviceName: payload.deviceName.trim(),
    deviceCode: `EQ-2026-${String(deviceId).padStart(4, '0')}`,
    category: payload.category?.trim() || 'Imported',
    status: 'AVAILABLE',
    location: payload.location?.trim() || 'TBD',
    imageUrl: `/uploads/devices/${deviceId}-${safeImageName}`,
    description: payload.description?.trim() || ''
  }

  devices.push(device)
  writeDevices(devices)
  return device
}

export function deviceStatusOptions(): DeviceStatus[] {
  return ['AVAILABLE', 'RESERVED', 'BORROWED', 'REPAIRING', 'DAMAGED', 'DISABLED']
}
