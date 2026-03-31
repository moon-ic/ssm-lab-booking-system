export type UserRoleCode = 'SUPER_ADMIN' | 'ADMIN' | 'TEACHER' | 'STUDENT'
export type UserStatus = 'ENABLED' | 'DISABLED'

export interface UserListItem {
  userId: number
  name: string
  account: string
  jobNoOrStudentNo: string
  roleCode: UserRoleCode
  creditScore: number
  status: UserStatus
  phone?: string
  managerId?: number | null
  firstLoginRequired: boolean
}

export interface UserListResponse {
  list: UserListItem[]
  pageNum: number
  pageSize: number
  total: number
}

export interface UserListQuery {
  keyword?: string
  roleCode?: UserRoleCode
  status?: UserStatus
  pageNum: number
  pageSize: number
}

export interface CreateAdminPayload {
  name: string
  account: string
  phone?: string
}

export interface CreateTeacherPayload {
  name: string
  jobNo: string
  phone?: string
}

export interface CreateStudentPayload {
  name: string
  studentNo: string
  phone?: string
}

export interface UpdateUserStatusPayload {
  status: UserStatus
}

export interface ResetPasswordPayload {
  newPassword: string
}
