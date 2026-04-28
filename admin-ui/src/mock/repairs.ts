import { mockGetDeviceDetail } from '@/mock/devices'
import { listMockBorrowRecords } from '@/mock/borrow-records'
import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import type {
  CreateRepairPayload,
  RepairItem,
  RepairListQuery,
  RepairListResponse,
  UpdateRepairStatusPayload
} from '@/types/repair'

const REPAIRS_STORAGE_KEY = 'mock-repair-items'

const defaultRepairs: RepairItem[] = [
  {
    repairId: 5001,
    deviceId: 1003,
    deviceName: 'Microscope Set',
    applicantId: 5,
    applicantName: 'Student Zhao',
    description: 'Focus ring is jammed during use.',
    status: 'PROCESSING',
    comment: 'Technician inspection in progress',
    createdAt: '2026-03-30 10:00:00',
    updatedAt: '2026-03-31 09:00:00'
  }
]

function readRepairs() {
  const raw = localStorage.getItem(REPAIRS_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(REPAIRS_STORAGE_KEY, JSON.stringify(defaultRepairs))
    return structuredClone(defaultRepairs)
  }

  try {
    return JSON.parse(raw) as RepairItem[]
  } catch {
    localStorage.setItem(REPAIRS_STORAGE_KEY, JSON.stringify(defaultRepairs))
    return structuredClone(defaultRepairs)
  }
}

function writeRepairs(items: RepairItem[]) {
  localStorage.setItem(REPAIRS_STORAGE_KEY, JSON.stringify(items))
}

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function visibleTo(token: string, item: RepairItem) {
  const user = currentUser(token)
  if (user.roleCode === 'SUPER_ADMIN' || user.roleCode === 'ADMIN') {
    return true
  }
  if (user.roleCode === 'TEACHER') {
    const applicant = readMockUsers().find((candidate) => candidate.userId === item.applicantId)
    return applicant?.roleCode === 'STUDENT' && applicant.managerId === user.userId
  }
  return item.applicantId === user.userId
}

function nextRepairId(items: RepairItem[]) {
  return items.reduce((max, item) => Math.max(max, item.repairId), 0) + 1
}

export async function mockListRepairs(token: string, query: RepairListQuery): Promise<RepairListResponse> {
  await wait()

  const filtered = readRepairs()
    .filter((item) => visibleTo(token, item))
    .filter((item) => !query.status || item.status === query.status)
    .filter((item) => !query.deviceId || item.deviceId === query.deviceId)
    .filter((item) => !query.applicantId || item.applicantId === query.applicantId)

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

export async function mockGetRepairDetail(token: string, repairId: number) {
  await wait(180)
  const item = readRepairs().find((entry) => entry.repairId === repairId)
  if (!item) {
    throw new Error('Repair request not found')
  }
  if (!visibleTo(token, item)) {
    throw new Error('No permission to access this repair')
  }
  return item
}

export async function mockCreateRepair(token: string, payload: CreateRepairPayload) {
  await wait()
  const user = currentUser(token)
  if (user.roleCode !== 'STUDENT') {
    throw new Error('Only students can submit repair requests')
  }

  const device = await mockGetDeviceDetail(payload.deviceId)
  const hasActiveBorrow = listMockBorrowRecords().some(
    (item) =>
      item.userId === user.userId &&
      item.deviceId === payload.deviceId &&
      (item.status === 'BORROWING' || item.status === 'OVERDUE')
  )
  if (!hasActiveBorrow) {
    throw new Error('Only currently borrowed devices can be submitted for repair')
  }

  const items = readRepairs()
  const activeExists = items.some(
    (item) => item.deviceId === payload.deviceId && item.status !== 'COMPLETED' && item.status !== 'UNREPAIRABLE'
  )
  if (activeExists) {
    throw new Error('This device already has an active repair request')
  }

  const repair: RepairItem = {
    repairId: nextRepairId(items),
    deviceId: payload.deviceId,
    deviceName: device.deviceName,
    applicantId: user.userId,
    applicantName: user.name,
    description: payload.description,
    status: 'PENDING',
    createdAt: '2026-03-31 19:00:00',
    updatedAt: '2026-03-31 19:00:00'
  }
  items.push(repair)
  writeRepairs(items)
  return repair
}

export async function mockUpdateRepairStatus(token: string, repairId: number, payload: UpdateRepairStatusPayload) {
  await wait(200)
  const user = currentUser(token)
  if (!['SUPER_ADMIN', 'ADMIN'].includes(user.roleCode)) {
    throw new Error('Only admin can update repair status')
  }

  const items = readRepairs()
  const item = items.find((entry) => entry.repairId === repairId)
  if (!item) {
    throw new Error('Repair request not found')
  }

  item.status = payload.status
  item.comment = payload.comment?.trim() || ''
  item.updatedAt = '2026-03-31 19:30:00'
  writeRepairs(items)
  return item
}
