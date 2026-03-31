import { canAccessRoute, getAccessibleMenuItems, type AppRouteMeta } from '@/router/access'

describe('router access helpers', () => {
  it('filters visible menus by role', () => {
    const studentMenus = getAccessibleMenuItems('STUDENT').map((item) => item.path)
    const superAdminMenus = getAccessibleMenuItems('SUPER_ADMIN').map((item) => item.path)

    expect(studentMenus).toContain('/')
    expect(studentMenus).toContain('/profile')
    expect(studentMenus).not.toContain('/roles')
    expect(studentMenus).not.toContain('/menu-config')

    expect(superAdminMenus).toContain('/roles')
    expect(superAdminMenus).toContain('/statistics')
    expect(superAdminMenus).toContain('/menu-config')
  })

  it('allows routes without roles and blocks missing role when required', () => {
    const openMeta: AppRouteMeta = {
      title: 'Dashboard',
      description: 'Open route'
    }
    const protectedMeta: AppRouteMeta = {
      title: 'Roles',
      description: 'Protected route',
      roles: ['SUPER_ADMIN']
    }

    expect(canAccessRoute(openMeta)).toBe(true)
    expect(canAccessRoute(protectedMeta)).toBe(false)
    expect(canAccessRoute(protectedMeta, 'ADMIN')).toBe(false)
    expect(canAccessRoute(protectedMeta, 'SUPER_ADMIN')).toBe(true)
  })
})
