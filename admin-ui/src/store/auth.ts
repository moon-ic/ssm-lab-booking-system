import { reactive } from 'vue'
import { changePassword, getCurrentUser, login } from '@/api/auth'
import type {
  AuthSession,
  ChangePasswordRequest,
  CurrentUser,
  LoginRequest
} from '@/types/auth'

const AUTH_STORAGE_KEY = 'admin-auth-session'
const TOKEN_STORAGE_KEY = 'token'

interface AuthState {
  session: AuthSession | null
  currentUser: CurrentUser | null
  initialized: boolean
}

function readSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

function writeSession(session: AuthSession | null) {
  if (!session) {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    return
  }

  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
  localStorage.setItem(TOKEN_STORAGE_KEY, session.token)
}

const state = reactive<AuthState>({
  session: readSession(),
  currentUser: null,
  initialized: false
})

async function signIn(payload: LoginRequest) {
  const response = await login(payload)
  state.session = response
  state.currentUser = null
  writeSession(response)
  return response
}

async function ensureCurrentUser(force = false) {
  if (!state.session) {
    state.currentUser = null
    state.initialized = true
    return null
  }

  if (state.currentUser && !force) {
    state.initialized = true
    return state.currentUser
  }

  try {
    const user = await getCurrentUser(state.session.token)
    state.currentUser = user
    state.session = {
      ...state.session,
      firstLoginRequired: user.firstLoginRequired,
      userInfo: {
        ...state.session.userInfo,
        ...user
      }
    }
    writeSession(state.session)
    state.initialized = true
    return user
  } catch (error) {
    signOut()
    throw error
  }
}

async function updatePassword(payload: ChangePasswordRequest) {
  if (!state.session) {
    throw new Error('No active session')
  }

  await changePassword(state.session.token, payload)

  state.session = {
    ...state.session,
    firstLoginRequired: false,
    userInfo: {
      ...state.session.userInfo,
      firstLoginRequired: false
    }
  }
  writeSession(state.session)

  if (state.currentUser) {
    state.currentUser = {
      ...state.currentUser,
      firstLoginRequired: false
    }
  }
}

function signOut() {
  state.session = null
  state.currentUser = null
  state.initialized = true
  writeSession(null)
}

export function useAuthStore() {
  return {
    state,
    signIn,
    ensureCurrentUser,
    updatePassword,
    signOut
  }
}
