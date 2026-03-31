export interface RoleItem {
  roleId: number
  roleName: string
  roleCode: string
  remark?: string
  permissionIds: number[]
  menuIds: number[]
}

export interface PermissionItem {
  permissionId: number
  permissionCode: string
  permissionName: string
  type: 'MENU' | 'ACTION'
}

export interface MenuItemOption {
  menuId: number
  menuName: string
  path: string
  icon: string
  permissionCode: string
}

export interface SaveRolePayload {
  roleName: string
  roleCode: string
  remark?: string
}

export interface AssignPermissionsPayload {
  permissionIds: number[]
  menuIds: number[]
}
