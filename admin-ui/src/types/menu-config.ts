export interface MenuConfigItem {
  menuId: number
  menuName: string
  path: string
  icon: string
  permissionCode: string
}

export interface UpdateMenuConfigPayload {
  menuName: string
  path: string
  icon: string
  permissionCode: string
}
