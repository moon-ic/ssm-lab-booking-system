import { useAuthStore } from '@/store/auth'
import type { AuthSession, CurrentUser, LoginResponse } from '@/types/auth'
import { vi } from 'vitest'

const { loginMock, getCurrentUserMock, changePasswordMock } = vi.hoisted(() => ({
  loginMock: vi.fn<(_: { loginId: string; password: string }) => Promise<LoginResponse>>(),
  getCurrentUserMock: vi.fn<(_: string) => Promise<CurrentUser>>(),
  changePasswordMock: vi.fn<(_: string, __: { oldPassword: string; newPassword: string }) => Promise<void>>()
}))

vi.mock('@/api/auth', () => ({
  login: loginMock,
  getCurrentUser: getCurrentUserMock,
  changePassword: changePasswordMock
}))

describe('auth store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const authStore = useAuthStore()
    authStore.state.session = null
    authStore.state.currentUser = null
    authStore.state.initialized = false
  })

  it('signs in and persists session', async () => {
    const session: AuthSession = {
      token: 'token-1',
      tokenType: 'Bearer',
      expiresIn: 7200,
      firstLoginRequired: true,
      userInfo: {
        userId: 1,
        name: 'Super Admin',
        account: 'admin',
        roleCode: 'SUPER_ADMIN',
        status: 'ENABLED',
        firstLoginRequired: true
      }
    }
    loginMock.mockResolvedValue(session)

    const authStore = useAuthStore()
    await authStore.signIn({ loginId: 'admin', password: '000000' })

    expect(authStore.state.session?.token).toBe('token-1')
    expect(JSON.parse(localStorage.getItem('admin-auth-session') ?? '{}').token).toBe('token-1')
  })

  it('hydrates current user and updates first-login flag after password change', async () => {
    localStorage.setItem('admin-auth-session', JSON.stringify({
      token: 'token-4',
      tokenType: 'Bearer',
      expiresIn: 7200,
      firstLoginRequired: true,
      userInfo: {
        userId: 4,
        name: 'Student Wang',
        account: 'student01',
        roleCode: 'STUDENT',
        status: 'ENABLED',
        firstLoginRequired: true
      }
    } satisfies AuthSession))

    const authStore = useAuthStore()
    authStore.state.session = JSON.parse(localStorage.getItem('admin-auth-session') ?? '{}') as AuthSession

    getCurrentUserMock.mockResolvedValue({
      userId: 4,
      name: 'Student Wang',
      account: 'student01',
      roleCode: 'STUDENT',
      status: 'ENABLED',
      firstLoginRequired: true,
      jobNoOrStudentNo: '20230001',
      creditScore: 90
    })

    await authStore.ensureCurrentUser(true)
    expect(authStore.state.currentUser?.jobNoOrStudentNo).toBe('20230001')

    changePasswordMock.mockResolvedValue()
    await authStore.updatePassword({ oldPassword: '000000', newPassword: 'password123' })

    expect(authStore.state.session?.firstLoginRequired).toBe(false)
    expect(authStore.state.currentUser?.firstLoginRequired).toBe(false)
  })

  it('signs out and clears storage', () => {
    const authStore = useAuthStore()
    authStore.state.session = {
      token: 'token-1',
      tokenType: 'Bearer',
      expiresIn: 7200,
      firstLoginRequired: false,
      userInfo: {
        userId: 1,
        name: 'Super Admin',
        account: 'admin',
        roleCode: 'SUPER_ADMIN',
        status: 'ENABLED',
        firstLoginRequired: false
      }
    }
    localStorage.setItem('admin-auth-session', JSON.stringify(authStore.state.session))

    authStore.signOut()

    expect(authStore.state.session).toBeNull()
    expect(localStorage.getItem('admin-auth-session')).toBeNull()
  })
})
