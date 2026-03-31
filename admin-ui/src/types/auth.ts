export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  loginId: string
  password: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UserInfo {
  userId: number
  name: string
  account: string
  roleCode: string
  status: 'ENABLED' | 'DISABLED'
  firstLoginRequired: boolean
}

export interface CurrentUser extends UserInfo {
  jobNoOrStudentNo: string
  creditScore: number
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  firstLoginRequired: boolean
  userInfo: UserInfo
}

export interface AuthSession {
  token: string
  tokenType: string
  expiresIn: number
  firstLoginRequired: boolean
  userInfo: UserInfo
}
