import { changePassword, getCurrentUser, login } from '@/api/auth'
import { listBorrowRecords, listBorrowReminders, markBorrowRecordOverdue, pickupBorrowRecord, returnBorrowRecord } from '@/api/borrow-records'
import { importDevice } from '@/api/device-imports'
import { createDevice, getDeviceDetail, listDevices, updateDevice, updateDeviceStatus } from '@/api/devices'
import { confirmMessage, listMessages, unconfirmedSummary } from '@/api/messages'
import { confirmMyMessage, getProfile, listMyBorrowRecords, listMyMessages } from '@/api/profile'
import { createRepair, getRepairDetail, listRepairs, updateRepairStatus } from '@/api/repairs'
import { approveReservation, cancelReservation, createReservation, getReservationDetail, listReservations } from '@/api/reservations'
import { assignPermissions, createRole, getRoleDetail, listMenus, listPermissions, listRoles, updateRole } from '@/api/roles'
import { deviceDamageStatistics, hotDevices, statisticsOverview, userViolationStatistics } from '@/api/statistics'
import { createAdmin, createStudent, createTeacher, deleteStudent, getUserDetail, listUsers, resetPassword, updateUserStatus } from '@/api/users'

describe('api integration with mock backend', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('covers auth, user, and role assignment flows', async () => {
    const superAdminSession = await login({ loginId: 'SA001', password: '0000' })
    localStorage.setItem('admin-auth-session', JSON.stringify(superAdminSession))

    const me = await getCurrentUser(superAdminSession.token)
    expect(me.roleCode).toBe('SUPER_ADMIN')

    const admin = await createAdmin({ name: 'Admin B', account: 'admin_b', phone: '13800001111' })
    expect(admin.roleCode).toBe('ADMIN')

    const roles = await listRoles()
    expect(roles.length).toBeGreaterThan(0)

    const createdRole = await createRole({
      roleName: 'Assistant',
      roleCode: 'ASSISTANT',
      remark: 'test role'
    })
    expect(createdRole.roleCode).toBe('ASSISTANT')

    const updatedRole = await updateRole(createdRole.roleId, {
      roleName: 'Assistant Updated',
      roleCode: 'ASSISTANT',
      remark: 'updated'
    })
    expect(updatedRole.roleName).toBe('Assistant Updated')

    const permissions = await listPermissions()
    const menus = await listMenus()
    const assignedRole = await assignPermissions(updatedRole.roleId, {
      permissionIds: permissions.slice(0, 2).map((item) => item.permissionId),
      menuIds: menus.slice(0, 2).map((item) => item.menuId)
    })
    expect(assignedRole.permissionIds).toHaveLength(2)

    const roleDetail = await getRoleDetail(createdRole.roleId)
    expect(roleDetail.menuIds).toHaveLength(2)
  })

  it('covers admin, teacher, and student flows across users, devices, reservations, borrow records, repairs, profile, and messages', async () => {
    const superAdminSession = await login({ loginId: 'SA001', password: '0000' })
    localStorage.setItem('admin-auth-session', JSON.stringify(superAdminSession))
    const admin = await createAdmin({ name: 'Admin Flow', account: 'admin_flow', phone: '13800001222' })

    await resetPassword(4, { newPassword: '000000' })
    await updateUserStatus(3, { status: 'ENABLED' })

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: admin.account, password: '0000' })))
    const teacher = await createTeacher({ name: 'Teacher Flow', jobNo: 'T2026888', phone: '13800001333' })
    expect(teacher.roleCode).toBe('TEACHER')

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'admin_flow', password: '0000' })))
    const device = await createDevice({
      deviceName: 'Flow Camera',
      deviceCode: 'EQ-2026-1888',
      category: 'Camera',
      location: 'Lab 9',
      description: 'integration device'
    })
    expect((await getDeviceDetail(device.deviceId)).deviceCode).toBe('EQ-2026-1888')

    await updateDevice(device.deviceId, {
      deviceName: 'Flow Camera Updated',
      deviceCode: 'EQ-2026-1888',
      category: 'Camera',
      location: 'Lab 9',
      description: 'updated'
    })
    await updateDeviceStatus(device.deviceId, { status: 'AVAILABLE' })
    expect((await listDevices({ pageNum: 1, pageSize: 20 })).list.some((item) => item.deviceId === device.deviceId)).toBe(true)

    const imported = await importDevice({
      deviceName: 'Imported Device',
      category: 'Imported',
      location: 'Lab 10',
      description: 'imported by test',
      image: new File(['fake-image'], 'device.png', { type: 'image/png' })
    })
    expect(imported.status).toBe('AVAILABLE')

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'T2026888', password: '0000' })))
    const student = await createStudent({ name: 'Student Flow', studentNo: '20245555', phone: '13800001444' })
    expect(student.roleCode).toBe('STUDENT')
    expect((await getUserDetail(student.userId)).jobNoOrStudentNo).toBe('20245555')

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: '20245555', password: '0000' })))
    await changePassword(localStorage.getItem('admin-auth-session') ? JSON.parse(localStorage.getItem('admin-auth-session')!).token : '', {
      oldPassword: '0000',
      newPassword: 'password123'
    })

    const studentSession = await login({ loginId: '20245555', password: 'password123' })
    localStorage.setItem('admin-auth-session', JSON.stringify(studentSession))
    const reservation = await createReservation({
      deviceId: device.deviceId,
      startTime: '2026-04-10 09:00:00',
      endTime: '2026-04-10 18:00:00',
      purpose: 'integration reservation'
    })
    expect((await getReservationDetail(reservation.reservationId)).status).toBe('PENDING')

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'T2026888', password: '0000' })))
    const teacherApproved = await approveReservation(reservation.reservationId, { action: 'APPROVE', comment: 'teacher approved' })
    expect(teacherApproved.status).toBe('APPROVED')
    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'admin_flow', password: '0000' })))
    const approved = await approveReservation(reservation.reservationId, { action: 'APPROVE', comment: 'admin approved' })
    expect(approved.status).toBe('PICKUP_PENDING')
    expect((await listReservations({ pageNum: 1, pageSize: 20 })).list.some((item) => item.reservationId === reservation.reservationId)).toBe(true)

    localStorage.setItem('admin-auth-session', JSON.stringify(studentSession))
    const borrowList = await listBorrowRecords({ pageNum: 1, pageSize: 20 })
    const record = borrowList.list.find((item) => item.reservationId === reservation.reservationId)
    expect(record).toBeTruthy()
    if (!record) {
      throw new Error('Expected borrow record to exist')
    }
    await pickupBorrowRecord(record.recordId, {})
    await returnBorrowRecord(record.recordId, { returnTime: '2026-04-10 17:30', deviceCondition: 'BROKEN' })

    const repair = await createRepair({
      deviceId: device.deviceId,
      description: 'broken after return'
    })
    expect((await getRepairDetail(repair.repairId)).status).toBe('PENDING')
    expect((await listRepairs({ pageNum: 1, pageSize: 20 })).list.some((item) => item.repairId === repair.repairId)).toBe(true)

    const profile = await getProfile()
    expect(profile.roleCode).toBe('STUDENT')
    expect((await listMyBorrowRecords({ pageNum: 1, pageSize: 20 })).total).toBeGreaterThanOrEqual(0)

    const profileMessages = await listMyMessages({ pageNum: 1, pageSize: 20 })
    if (profileMessages.list.length > 0) {
      const confirmed = await confirmMyMessage(profileMessages.list[0].messageId)
      expect(confirmed.confirmStatus).toBe('CONFIRMED')
    }

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'admin_flow', password: '0000' })))
    const overdueTarget = (await listBorrowRecords({ pageNum: 1, pageSize: 20 })).list.find((item) => item.recordId === record.recordId)
    expect(overdueTarget).toBeTruthy()
    await markBorrowRecordOverdue(record.recordId)
    expect((await listBorrowReminders('OVERDUE')).some((item) => item.recordId === record.recordId)).toBe(true)

    const updatedRepair = await updateRepairStatus(repair.repairId, { status: 'PROCESSING', comment: 'sent for repair' })
    expect(updatedRepair.status).toBe('PROCESSING')

    const summary = await statisticsOverview()
    expect(summary.deviceTotal).toBeGreaterThan(0)
    expect((await hotDevices({ rankScope: 'MONTH', topN: 5 })).length).toBeGreaterThan(0)
    expect((await deviceDamageStatistics({ rankScope: 'MONTH', topN: 5 })).length).toBeGreaterThan(0)
    expect((await userViolationStatistics({ rankScope: 'MONTH', topN: 5 })).length).toBeGreaterThanOrEqual(0)

    const msgList = await listMessages({ pageNum: 1, pageSize: 20, userId: student.userId, confirmStatus: 'UNCONFIRMED' })
    expect(msgList.total).toBeGreaterThanOrEqual(0)
    const msgSummary = await unconfirmedSummary()
    expect(msgSummary.total).toBeGreaterThanOrEqual(0)
    if (msgList.list.length > 0) {
      const confirmed = await confirmMessage(msgList.list[0].messageId)
      expect(confirmed.confirmStatus).toBe('CONFIRMED')
    }

    localStorage.setItem('admin-auth-session', JSON.stringify(studentSession))
    const cancelledReservation = await createReservation({
      deviceId: imported.deviceId,
      startTime: '2026-04-12 09:00:00',
      endTime: '2026-04-12 12:00:00',
      purpose: 'cancel case'
    })
    await cancelReservation(cancelledReservation.reservationId)
    expect((await getReservationDetail(cancelledReservation.reservationId)).status).toBe('CANCELLED')

    localStorage.setItem('admin-auth-session', JSON.stringify(await login({ loginId: 'T2026888', password: '0000' })))
    await deleteStudent(student.userId)
    const teacherUsers = await listUsers({ pageNum: 1, pageSize: 50 })
    expect(teacherUsers.list.some((item) => item.userId === student.userId)).toBe(false)
  }, 30000)
})
