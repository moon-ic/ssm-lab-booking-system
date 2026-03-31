import type {
  ChangePasswordRequest,
  CurrentUser,
  LoginRequest,
  LoginResponse,
  UserInfo
} from '@/types/auth'
import { getCurrentUserIdFromToken, readMockUsers, wait, writeMockUsers } from '@/mock/userStore'

function toUserInfo(user: ReturnType<typeof readMockUsers>[number]): UserInfo {
  return {
    userId: user.userId,
    name: user.name,
    account: user.account,
    roleCode: user.roleCode,
    status: user.status,
    firstLoginRequired: user.firstLoginRequired
  }
}

function toCurrentUser(user: ReturnType<typeof readMockUsers>[number]): CurrentUser {
  return {
    ...toUserInfo(user),
    jobNoOrStudentNo: user.loginId,
    creditScore: user.creditScore
  }
}

export async function mockLogin(payload: LoginRequest): Promise<LoginResponse> {
  await wait()

  const users = readMockUsers()
  const user = users.find((item) => !item.deleted && item.loginId === payload.loginId)

  if (!user || user.password !== payload.password) {
    throw new Error('Incorrect account or password')
  }

  if (user.status !== 'ENABLED') {
    throw new Error('This account is disabled')
  }

  return {
    token: `mock-${user.userId}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
    firstLoginRequired: user.firstLoginRequired,
    userInfo: toUserInfo(user)
  }
}

export async function mockGetCurrentUser(token: string): Promise<CurrentUser> {
  await wait(180)

  const userId = getCurrentUserIdFromToken(token)
  const user = readMockUsers().find((item) => !item.deleted && item.userId === userId)

  if (!user) {
    throw new Error('Session expired')
  }

  return toCurrentUser(user)
}

export async function mockChangePassword(token: string, payload: ChangePasswordRequest): Promise<void> {
  await wait(260)

  const users = readMockUsers()
  const userId = getCurrentUserIdFromToken(token)
  const user = users.find((item) => !item.deleted && item.userId === userId)

  if (!user) {
    throw new Error('Session expired')
  }

  if (user.password !== payload.oldPassword) {
    throw new Error('Old password is incorrect')
  }

  if (payload.newPassword.trim().length < 8) {
    throw new Error('New password must be at least 8 characters')
  }

  user.password = payload.newPassword.trim()
  user.firstLoginRequired = false
  writeMockUsers(users)
}
