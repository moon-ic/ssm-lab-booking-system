import { flushPromises, mount } from '@vue/test-utils'
import MenuConfigView from '@/views/MenuConfigView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listMenuConfigsMock, listIconsMock, listPermissionsMock } = vi.hoisted(() => ({
  listMenuConfigsMock: vi.fn(),
  listIconsMock: vi.fn(),
  listPermissionsMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/menu-config', () => ({
  listMenuConfigs: listMenuConfigsMock,
  listIcons: listIconsMock,
  updateMenuConfig: vi.fn()
}))

vi.mock('@/api/roles', () => ({
  listPermissions: listPermissionsMock
}))

describe('MenuConfigView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads menu registry and displays menu detail', async () => {
    listMenuConfigsMock.mockResolvedValue([
      {
        menuId: 205,
        menuName: 'Devices',
        path: '/devices',
        icon: 'Monitor',
        permissionCode: 'device:view'
      }
    ])
    listIconsMock.mockResolvedValue(['Monitor', 'Setting'])
    listPermissionsMock.mockResolvedValue([
      {
        permissionId: 4,
        permissionCode: 'device:view',
        permissionName: 'View devices',
        type: 'ACTION'
      }
    ])

    const wrapper = mount(MenuConfigView, {
      global: {
        stubs: elementPlusStubs,
        directives: {
          loading: {}
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Menu and icon configuration')
    expect(wrapper.text()).toContain('Devices')
    expect(wrapper.text()).toContain('/devices')
    expect(wrapper.text()).toContain('device:view')
  })
})
