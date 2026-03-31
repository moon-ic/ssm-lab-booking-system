import {
  mockDeviceDamageStatistics,
  mockHotDevices,
  mockStatisticsOverview,
  mockUserViolationStatistics
} from '@/mock/statistics'

describe('statistics mock', () => {
  const adminToken = 'mock-2'

  beforeEach(() => {
    localStorage.setItem('mock-auth-users', JSON.stringify([
      { userId: 2, name: 'Admin Chen', account: 'admin_chen', loginId: 'A001', password: '000000', roleCode: 'ADMIN', status: 'ENABLED', creditScore: 100, firstLoginRequired: false, deleted: false },
      { userId: 3, name: 'Teacher Li', account: 'teacher01', loginId: 'T2026001', password: '000000', roleCode: 'TEACHER', status: 'ENABLED', creditScore: 96, firstLoginRequired: false, managerId: 2, deleted: false },
      { userId: 4, name: 'Student Wang', account: 'student01', loginId: '20230001', password: '000000', roleCode: 'STUDENT', status: 'ENABLED', creditScore: 88, firstLoginRequired: false, managerId: 3, deleted: false }
    ]))

    localStorage.setItem('mock-device-items', JSON.stringify([
      { deviceId: 1001, deviceName: 'Camera', deviceCode: 'EQ-1001', category: 'Camera', status: 'AVAILABLE', location: 'Lab A', imageUrl: '', description: '' },
      { deviceId: 1002, deviceName: 'Projector', deviceCode: 'EQ-1002', category: 'Projector', status: 'REPAIRING', location: 'Lab B', imageUrl: '', description: '' }
    ]))

    localStorage.setItem('mock-borrow-record-items', JSON.stringify([
      { recordId: 9001, reservationId: 7001, userId: 4, userName: 'Student Wang', deviceId: 1001, deviceName: 'Camera', status: 'BORROWING', pickupTime: '2026-03-29 09:00:00', expectedReturnTime: '2026-04-01 09:00:00' },
      { recordId: 9002, reservationId: 7002, userId: 4, userName: 'Student Wang', deviceId: 1001, deviceName: 'Camera', status: 'RETURNED', pickupTime: '2026-03-20 09:00:00', expectedReturnTime: '2026-03-22 09:00:00', returnTime: '2026-03-23 09:00:00', deviceCondition: 'BROKEN' },
      { recordId: 9003, reservationId: 7003, userId: 4, userName: 'Student Wang', deviceId: 1002, deviceName: 'Projector', status: 'OVERDUE', pickupTime: '2026-03-30 09:00:00', expectedReturnTime: '2026-03-30 18:00:00' }
    ]))

    localStorage.setItem('mock-reservation-items', JSON.stringify([
      { reservationId: 7001, deviceId: 1001, deviceName: 'Camera', applicantId: 4, applicantName: 'Student Wang', startTime: '2026-04-02 09:00:00', endTime: '2026-04-03 09:00:00', purpose: 'shooting', status: 'PENDING' }
    ]))

    localStorage.setItem('mock-repair-items', JSON.stringify([
      { repairId: 5001, deviceId: 1002, deviceName: 'Projector', applicantId: 4, applicantName: 'Student Wang', description: 'lens issue', status: 'PENDING', createdAt: '2026-03-30 10:00:00', updatedAt: '2026-03-31 10:00:00' }
    ]))
  })

  it('builds overview counts from existing module data', async () => {
    const overview = await mockStatisticsOverview(adminToken)

    expect(overview.deviceTotal).toBe(2)
    expect(overview.availableDeviceTotal).toBe(1)
    expect(overview.borrowingTotal).toBe(2)
    expect(overview.pendingReservationTotal).toBe(1)
    expect(overview.pendingRepairTotal).toBe(1)
  })

  it('computes rankings by scope', async () => {
    const hotDevices = await mockHotDevices(adminToken, { rankScope: 'MONTH', topN: 5 })
    const damagedDevices = await mockDeviceDamageStatistics(adminToken, { rankScope: 'MONTH', topN: 5 })
    const violationUsers = await mockUserViolationStatistics(adminToken, { rankScope: 'MONTH', topN: 5 })

    expect(hotDevices[0].deviceId).toBe(1001)
    expect(hotDevices[0].borrowCount).toBe(2)

    expect(damagedDevices[0].deviceId).toBe(1002)
    expect(damagedDevices[0].damageCount).toBeGreaterThanOrEqual(1)

    expect(violationUsers[0].userId).toBe(4)
    expect(violationUsers[0].violationCount).toBe(2)
  })

  it('rejects statistics access for non-admin roles', async () => {
    await expect(mockStatisticsOverview('mock-4')).rejects.toThrow('Only admin or super admin can view statistics')
  })
})
