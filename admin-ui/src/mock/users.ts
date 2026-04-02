import { getCurrentUserIdFromToken, nextMockUserId, readMockUsers, wait, writeMockUsers } from '@/mock/userStore'
import type {
  CreateAdminPayload,
  CreateStudentPayload,
  CreateTeacherPayload,
  ResetPasswordPayload,
  UpdateUserStatusPayload,
  UserListItem,
  UserListQuery,
  UserListResponse,
  UserRoleCode
} from '@/types/user'

function getCurrentUser(token: string) {
  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)

  if (!user) {
    throw new Error('Session expired')
  }

  return user
}

function visibleTo(currentRole: UserRoleCode, currentUserId: number, target: ReturnType<typeof readMockUsers>[number]) {
  if (target.deleted) {
    return false
  }

  switch (currentRole) {
    case 'SUPER_ADMIN':
      return target.roleCode !== 'SUPER_ADMIN' || target.userId === currentUserId
    case 'ADMIN':
      return target.roleCode === 'TEACHER' || target.roleCode === 'STUDENT'
    case 'TEACHER':
      return target.roleCode === 'STUDENT' && target.managerId === currentUserId
    default:
      return false
  }
}

function toUserView(user: ReturnType<typeof readMockUsers>[number]): UserListItem {
  return {
    userId: user.userId,
    name: user.name,
    account: user.account,
    jobNoOrStudentNo: user.loginId,
    roleCode: user.roleCode,
    creditScore: user.creditScore,
    status: user.status,
    phone: user.phone,
    managerId: user.managerId,
    firstLoginRequired: user.firstLoginRequired
  }
}

function containsKeyword(user: ReturnType<typeof readMockUsers>[number], keyword: string) {
  const normalized = keyword.trim().toLowerCase()
  return (
    user.name.toLowerCase().includes(normalized) ||
    user.account.toLowerCase().includes(normalized) ||
    user.loginId.toLowerCase().includes(normalized)
  )
}

function assertCanCreate(roleCode: UserRoleCode) {
  if (!['SUPER_ADMIN', 'ADMIN', 'TEACHER'].includes(roleCode)) {
    throw new Error('You do not have permission to create users')
  }
}

function assertCanManage(currentRole: UserRoleCode) {
  if (!['SUPER_ADMIN', 'ADMIN'].includes(currentRole)) {
    throw new Error('You do not have permission to manage this action')
  }
}

function assertVisible(token: string, userId: number) {
  const currentUser = getCurrentUser(token)
  const target = readMockUsers().find((item) => !item.deleted && item.userId === userId)

  if (!target) {
    throw new Error('User not found')
  }

  if (!visibleTo(currentUser.roleCode, currentUser.userId, target)) {
    throw new Error('No permission to access this user')
  }

  return { currentUser, target }
}

export async function mockListUsers(token: string, query: UserListQuery): Promise<UserListResponse> {
  await wait()

  const currentUser = getCurrentUser(token)
  const filtered = readMockUsers()
    .filter((item) => visibleTo(currentUser.roleCode, currentUser.userId, item))
    .filter((item) => !query.keyword || containsKeyword(item, query.keyword))
    .filter((item) => !query.roleCode || item.roleCode === query.roleCode)
    .filter((item) => !query.status || item.status === query.status)
    .sort((a, b) => a.userId - b.userId)

  const pageNum = query.pageNum || 1
  const pageSize = query.pageSize || 10
  const fromIndex = Math.min((pageNum - 1) * pageSize, filtered.length)
  const toIndex = Math.min(fromIndex + pageSize, filtered.length)

  return {
    list: filtered.slice(fromIndex, toIndex).map(toUserView),
    pageNum,
    pageSize,
    total: filtered.length
  }
}

export async function mockGetUserDetail(token: string, userId: number) {
  await wait(180)
  const { target } = assertVisible(token, userId)
  return toUserView(target)
}

export async function mockCreateAdmin(token: string, payload: CreateAdminPayload) {
  await wait()

  const currentUser = getCurrentUser(token)
  if (currentUser.roleCode !== 'SUPER_ADMIN') {
    throw new Error('Only super admin can create admins')
  }

  return createUser(currentUser.userId, 'ADMIN', payload.name, payload.account, payload.account, payload.phone)
}

export async function mockCreateTeacher(token: string, payload: CreateTeacherPayload) {
  await wait()

  const currentUser = getCurrentUser(token)
  if (currentUser.roleCode !== 'ADMIN') {
    throw new Error('Only admin can create teachers')
  }

  return createUser(currentUser.userId, 'TEACHER', payload.name, payload.jobNo, payload.jobNo, payload.phone)
}

export async function mockCreateStudent(token: string, payload: CreateStudentPayload) {
  await wait()

  const currentUser = getCurrentUser(token)
  if (currentUser.roleCode !== 'TEACHER') {
    throw new Error('Only teacher can create students')
  }

  return createUser(currentUser.userId, 'STUDENT', payload.name, payload.studentNo, payload.studentNo, payload.phone)
}

function createUser(
  managerId: number,
  roleCode: UserRoleCode,
  name: string,
  account: string,
  loginId: string,
  phone?: string
) {
  const users = readMockUsers()
  const duplicated = users.some(
    (item) => !item.deleted && (item.account === account || item.loginId === loginId)
  )

  if (duplicated) {
    throw new Error('Account or login ID already exists')
  }

  const record = {
    userId: nextMockUserId(users),
    name,
    account,
    loginId,
    password: '0000',
    roleCode,
    status: 'ENABLED' as const,
    creditScore: 100,
    firstLoginRequired: true,
    phone,
    managerId,
    deleted: false
  }

  users.push(record)
  writeMockUsers(users)
  return toUserView(record)
}

export async function mockUpdateUserStatus(token: string, userId: number, payload: UpdateUserStatusPayload) {
  await wait(220)

  const { currentUser, target } = assertVisible(token, userId)
  assertCanManage(currentUser.roleCode)

  target.status = payload.status
  writeMockUsers(readMockUsers().map((item) => (item.userId === target.userId ? target : item)))
}

export async function mockResetPassword(token: string, userId: number, payload: ResetPasswordPayload) {
  await wait(220)

  const { currentUser, target } = assertVisible(token, userId)
  assertCanManage(currentUser.roleCode)

  target.password = payload.newPassword
  target.firstLoginRequired = true
  writeMockUsers(readMockUsers().map((item) => (item.userId === target.userId ? target : item)))
}

export async function mockDeleteStudent(token: string, userId: number) {
  await wait(200)

  const { currentUser, target } = assertVisible(token, userId)
  if (currentUser.roleCode !== 'TEACHER' || target.roleCode !== 'STUDENT') {
    throw new Error('Only teacher can delete owned students')
  }

  target.deleted = true
  target.status = 'DISABLED'
  writeMockUsers(readMockUsers().map((item) => (item.userId === target.userId ? target : item)))
}
