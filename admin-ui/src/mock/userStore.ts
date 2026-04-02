import type { UserRoleCode, UserStatus } from '@/types/user'

export interface MockUserRecord {
  userId: number
  name: string
  account: string
  loginId: string
  password: string
  roleCode: UserRoleCode
  status: UserStatus
  creditScore: number
  firstLoginRequired: boolean
  phone?: string
  managerId?: number | null
  deleted?: boolean
}

const MOCK_USERS_STORAGE_KEY = 'mock-auth-users'

const defaultUsers: MockUserRecord[] = [
  {
    userId: 1,
    name: 'Super Admin',
    account: 'superadmin',
    loginId: 'SA001',
    password: '0000',
    roleCode: 'SUPER_ADMIN',
    status: 'ENABLED',
    creditScore: 100,
    firstLoginRequired: false,
    phone: '13800000001',
    managerId: null,
    deleted: false
  },
  {
    userId: 2,
    name: 'Admin Chen',
    account: 'admin01',
    loginId: 'A001',
    password: '0000',
    roleCode: 'ADMIN',
    status: 'ENABLED',
    creditScore: 100,
    firstLoginRequired: false,
    phone: '13800000002',
    managerId: 1,
    deleted: false
  },
  {
    userId: 3,
    name: 'Teacher Li',
    account: 'teacher01',
    loginId: 'T2026001',
    password: '0000',
    roleCode: 'TEACHER',
    status: 'ENABLED',
    creditScore: 96,
    firstLoginRequired: false,
    phone: '13800000003',
    managerId: 2,
    deleted: false
  },
  {
    userId: 4,
    name: 'Student Wang',
    account: 'student01',
    loginId: '20230001',
    password: '0000',
    roleCode: 'STUDENT',
    status: 'ENABLED',
    creditScore: 88,
    firstLoginRequired: false,
    phone: '13800000004',
    managerId: 3,
    deleted: false
  },
  {
    userId: 5,
    name: 'Student Zhao',
    account: 'student02',
    loginId: '20230002',
    password: '0000',
    roleCode: 'STUDENT',
    status: 'ENABLED',
    creditScore: 91,
    firstLoginRequired: true,
    phone: '13800000005',
    managerId: 3,
    deleted: false
  },
  {
    userId: 6,
    name: 'Disabled Student',
    account: 'disabled',
    loginId: 'disabled',
    password: 'password123',
    roleCode: 'STUDENT',
    status: 'DISABLED',
    creditScore: 72,
    firstLoginRequired: false,
    phone: '13800000006',
    managerId: 3,
    deleted: false
  }
]

export function readMockUsers(): MockUserRecord[] {
  const raw = localStorage.getItem(MOCK_USERS_STORAGE_KEY)
  if (!raw) {
    localStorage.setItem(MOCK_USERS_STORAGE_KEY, JSON.stringify(defaultUsers))
    return structuredClone(defaultUsers)
  }

  try {
    return JSON.parse(raw) as MockUserRecord[]
  } catch {
    localStorage.setItem(MOCK_USERS_STORAGE_KEY, JSON.stringify(defaultUsers))
    return structuredClone(defaultUsers)
  }
}

export function writeMockUsers(users: MockUserRecord[]) {
  localStorage.setItem(MOCK_USERS_STORAGE_KEY, JSON.stringify(users))
}

export function nextMockUserId(users: MockUserRecord[]) {
  return users.reduce((max, item) => Math.max(max, item.userId), 0) + 1
}

export function getCurrentUserIdFromToken(token: string) {
  const [, id] = token.split('-')
  return Number(id)
}

export function wait(ms = 320) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}
