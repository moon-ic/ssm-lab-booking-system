import { mockListBorrowRecords } from '@/mock/borrow-records'
import { mockListDevices } from '@/mock/devices'
import { mockListRepairs } from '@/mock/repairs'
import { mockListReservations } from '@/mock/reservations'
import { getCurrentUserIdFromToken, readMockUsers, wait } from '@/mock/userStore'
import type { BorrowRecordItem } from '@/types/borrow-record'
import type { DeviceItem } from '@/types/device'
import type { RepairItem } from '@/types/repair'
import type { ReservationItem } from '@/types/reservation'
import type {
  DamageDeviceStatItem,
  HotDeviceStatItem,
  RankScope,
  StatisticsOverview,
  StatisticsQuery,
  UserViolationStatItem
} from '@/types/statistics'

interface DateRange {
  start: Date
  endExclusive: Date
  scope: RankScope
}

function currentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user) {
    throw new Error('Session expired')
  }
  return user
}

function ensureAdmin(token: string) {
  const user = currentUser(token)
  if (!['SUPER_ADMIN', 'ADMIN'].includes(user.roleCode)) {
    throw new Error('Only admin or super admin can view statistics')
  }
}

async function loadAllDevices() {
  const response = await mockListDevices({
    pageNum: 1,
    pageSize: 1000
  })
  return response.list
}

async function loadAllBorrowRecords(token: string) {
  const response = await mockListBorrowRecords(token, {
    pageNum: 1,
    pageSize: 1000
  })
  return response.list
}

async function loadAllReservations(token: string) {
  const response = await mockListReservations(token, {
    pageNum: 1,
    pageSize: 1000
  })
  return response.list
}

async function loadAllRepairs(token: string) {
  const response = await mockListRepairs(token, {
    pageNum: 1,
    pageSize: 1000
  })
  return response.list
}

function parseDateTime(value?: string) {
  if (!value) {
    return null
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function resolveRange(query: StatisticsQuery): DateRange {
  const hasCustomRange = Boolean(query.startDate?.trim() || query.endDate?.trim())
  if (hasCustomRange) {
    const start = parseDateTime(`${query.startDate?.trim()} 00:00:00`)
    const end = parseDateTime(`${query.endDate?.trim()} 00:00:00`)

    if (!start || !end) {
      throw new Error('Custom date range requires both start and end dates')
    }
    if (end < start) {
      throw new Error('End date cannot be earlier than start date')
    }

    const endExclusive = new Date(end)
    endExclusive.setDate(endExclusive.getDate() + 1)

    return {
      start,
      endExclusive,
      scope: query.rankScope ?? 'TOTAL'
    }
  }

  const scope = query.rankScope ?? 'TOTAL'
  const today = new Date()
  const endExclusive = new Date(today)
  endExclusive.setDate(endExclusive.getDate() + 1)

  if (scope === 'TOTAL') {
    return {
      start: new Date('2000-01-01T00:00:00'),
      endExclusive: new Date('2999-12-31T23:59:59'),
      scope
    }
  }

  const start = new Date(today)
  if (scope === 'HALF_YEAR') {
    start.setMonth(start.getMonth() - 6)
  } else {
    start.setMonth(start.getMonth() - 1)
  }
  start.setHours(0, 0, 0, 0)

  return {
    start,
    endExclusive,
    scope
  }
}

function inRange(value: string | undefined, range: DateRange) {
  const date = parseDateTime(value)
  return Boolean(date && date >= range.start && date < range.endExclusive)
}

function firstDefinedDate(...values: Array<string | undefined>) {
  return values.find((value) => Boolean(value))
}

function normalizeTopN(topN?: number) {
  return !topN || topN < 1 ? 10 : topN
}

function isUserViolationStatItem(item: UserViolationStatItem | null): item is UserViolationStatItem {
  return item !== null
}

export async function mockStatisticsOverview(token: string): Promise<StatisticsOverview> {
  ensureAdmin(token)
  await wait(180)

  const [devices, borrowRecords, reservations, repairs] = await Promise.all([
    loadAllDevices(),
    loadAllBorrowRecords(token),
    loadAllReservations(token),
    loadAllRepairs(token)
  ])

  return {
    deviceTotal: devices.length,
    availableDeviceTotal: devices.filter((item) => item.status === 'AVAILABLE').length,
    borrowingTotal: borrowRecords.filter((item) => item.status === 'BORROWING' || item.status === 'OVERDUE').length,
    pendingReservationTotal: reservations.filter((item) => item.status === 'PENDING').length,
    pendingRepairTotal: repairs.filter((item) => item.status === 'PENDING').length
  }
}

export async function mockHotDevices(token: string, query: StatisticsQuery): Promise<HotDeviceStatItem[]> {
  ensureAdmin(token)
  await wait(220)

  const range = resolveRange(query)
  const topN = normalizeTopN(query.topN)
  const [devices, borrowRecords] = await Promise.all([loadAllDevices(), loadAllBorrowRecords(token)])

  const counts = new Map<number, number>()
  borrowRecords.forEach((record) => {
    const counted =
      inRange(record.pickupTime, range) ||
      inRange(record.expectedReturnTime, range) ||
      inRange(record.returnTime, range)

    if (!counted) {
      return
    }

    counts.set(record.deviceId, (counts.get(record.deviceId) ?? 0) + 1)
  })

  const items: HotDeviceStatItem[] = []
  Array.from(counts.entries()).forEach(([deviceId, borrowCount]) => {
    const device = devices.find((item) => item.deviceId === deviceId)
    if (!device) {
      return
    }

    items.push({
      deviceId: device.deviceId,
      deviceName: device.deviceName,
      deviceCode: device.deviceCode,
      imageUrl: device.imageUrl,
      borrowCount,
      rankScope: range.scope
    })
  })

  return items
    .sort((left, right) => right.borrowCount - left.borrowCount || left.deviceId - right.deviceId)
    .slice(0, topN)
}

function toDamageView(device: DeviceItem, repairs: RepairItem[], range: DateRange) {
  const damageCount = repairs
    .filter((item) => item.deviceId === device.deviceId)
    .filter((item) => inRange(item.createdAt, range) || inRange(item.updatedAt, range)).length

  const deviceCurrentlyDamaged = device.status === 'DAMAGED' || device.status === 'REPAIRING'
  if (damageCount === 0 && !deviceCurrentlyDamaged) {
    return null
  }

  return {
    deviceId: device.deviceId,
    deviceName: device.deviceName,
    deviceCode: device.deviceCode,
    imageUrl: device.imageUrl,
    category: device.category,
    damageCount: Math.max(1, damageCount),
    status: device.status,
    rankScope: range.scope
  } satisfies DamageDeviceStatItem
}

export async function mockDeviceDamageStatistics(token: string, query: StatisticsQuery): Promise<DamageDeviceStatItem[]> {
  ensureAdmin(token)
  await wait(220)

  const range = resolveRange(query)
  const topN = normalizeTopN(query.topN)
  const [devices, repairs] = await Promise.all([loadAllDevices(), loadAllRepairs(token)])

  const items: DamageDeviceStatItem[] = []
  devices.forEach((device) => {
    const view = toDamageView(device, repairs, range)
    if (view) {
      items.push(view)
    }
  })

  return items
    .sort((left, right) => right.damageCount - left.damageCount || left.deviceId - right.deviceId)
    .slice(0, topN)
}

function toViolationView(userId: number, borrowRecords: BorrowRecordItem[], range: DateRange) {
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)
  if (!user || user.roleCode !== 'STUDENT') {
    return null
  }

  const overdueCount = borrowRecords
    .filter((item) => item.userId === user.userId)
    .filter((item) => item.status === 'OVERDUE')
    .filter((item) => inRange(firstDefinedDate(item.expectedReturnTime, item.returnTime, item.pickupTime), range)).length

  const damageCount = borrowRecords
    .filter((item) => item.userId === user.userId)
    .filter((item) => item.deviceCondition && item.deviceCondition !== 'NORMAL')
    .filter((item) => inRange(firstDefinedDate(item.returnTime, item.expectedReturnTime, item.pickupTime), range)).length

  const violationCount = overdueCount + damageCount
  if (violationCount === 0) {
    return null
  }

  return {
    userId: user.userId,
    name: user.name,
    jobNoOrStudentNo: user.loginId,
    overdueCount,
    damageCount,
    violationCount,
    rankScope: range.scope
  } satisfies UserViolationStatItem
}

export async function mockUserViolationStatistics(token: string, query: StatisticsQuery): Promise<UserViolationStatItem[]> {
  ensureAdmin(token)
  await wait(220)

  const range = resolveRange(query)
  const topN = normalizeTopN(query.topN)
  const borrowRecords = await loadAllBorrowRecords(token)
  const studentIds = readMockUsers()
    .filter((item) => !item.deleted && item.roleCode === 'STUDENT')
    .map((item) => item.userId)

  return studentIds
    .map((userId) => toViolationView(userId, borrowRecords, range))
    .filter(isUserViolationStatItem)
    .sort((left, right) => right.violationCount - left.violationCount || left.userId - right.userId)
    .slice(0, topN)
}
