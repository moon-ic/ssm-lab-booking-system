import { mockListMenuConfigs, mockUpdateMenuConfig } from '@/mock/menu-config'
import { mockListMenus } from '@/mock/roles'

describe('menu config mock', () => {
  const superAdminToken = 'mock-1'

  it('lists menus for super admin and propagates updates to role menu reads', async () => {
    const originalMenus = await mockListMenuConfigs(superAdminToken)
    expect(originalMenus.length).toBeGreaterThan(5)

    await mockUpdateMenuConfig(superAdminToken, 205, {
      menuName: 'Device Center',
      path: '/device-center',
      icon: 'Setting',
      permissionCode: 'device:view'
    })

    const updatedMenus = await mockListMenuConfigs(superAdminToken)
    const roleMenus = await mockListMenus(superAdminToken)
    const updated = updatedMenus.find((item) => item.menuId === 205)
    const roleView = roleMenus.find((item) => item.menuId === 205)

    expect(updated?.menuName).toBe('Device Center')
    expect(roleView?.path).toBe('/device-center')
    expect(roleView?.icon).toBe('Setting')
  })

  it('rejects invalid icon or permission code', async () => {
    await expect(mockUpdateMenuConfig(superAdminToken, 201, {
      menuName: 'Broken Config',
      path: '/broken-config',
      icon: 'NotAnIcon',
      permissionCode: 'profile:view'
    })).rejects.toThrow('Icon does not exist')

    await expect(mockUpdateMenuConfig(superAdminToken, 201, {
      menuName: 'Broken Config',
      path: '/broken-config',
      icon: 'HomeFilled',
      permissionCode: 'not:exists'
    })).rejects.toThrow('Permission code does not exist')
  })
})
