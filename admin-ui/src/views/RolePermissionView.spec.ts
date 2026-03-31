import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import RolePermissionView from '@/views/RolePermissionView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const {
  listRolesMock,
  listPermissionsMock,
  listMenusMock,
  getRoleDetailMock,
  createRoleMock
} = vi.hoisted(() => ({
  listRolesMock: vi.fn(),
  listPermissionsMock: vi.fn(),
  listMenusMock: vi.fn(),
  getRoleDetailMock: vi.fn(),
  createRoleMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/roles', () => ({
  assignPermissions: vi.fn(),
  createRole: createRoleMock,
  getRoleDetail: getRoleDetailMock,
  listMenus: listMenusMock,
  listPermissions: listPermissionsMock,
  listRoles: listRolesMock,
  updateRole: vi.fn()
}))

function mountView() {
  return mount(RolePermissionView, {
    global: {
      stubs: elementPlusStubs,
      directives: {
        loading: {}
      }
    }
  })
}

function findButton(wrapper: VueWrapper, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

describe('RolePermissionView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listRolesMock.mockResolvedValue([
      {
        roleId: 1,
        roleName: 'Admin',
        roleCode: 'ADMIN',
        remark: 'System admin',
        permissionIds: [1],
        menuIds: [201]
      }
    ])
    listPermissionsMock.mockResolvedValue([
      {
        permissionId: 1,
        permissionCode: 'user:view',
        permissionName: 'View users',
        type: 'ACTION'
      }
    ])
    listMenusMock.mockResolvedValue([
      {
        menuId: 201,
        menuName: 'Users',
        path: '/users',
        permissionCode: 'user:view'
      }
    ])
    getRoleDetailMock.mockResolvedValue({
      roleId: 1,
      roleName: 'Admin',
      roleCode: 'ADMIN',
      remark: 'System admin',
      permissionIds: [1],
      menuIds: [201]
    })
    createRoleMock.mockResolvedValue({
      roleId: 2,
      roleName: 'Auditor',
      roleCode: 'AUDITOR',
      remark: 'Review only',
      permissionIds: [],
      menuIds: []
    })
  })

  it('loads role detail and creates a new role', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Role Module')
    expect(wrapper.text()).toContain('Admin')
    expect(wrapper.text()).toContain('View users')

    await findButton(wrapper, 'New Role')?.trigger('click')

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('Auditor')
    await inputs[1].setValue('AUDITOR')
    await inputs[2].setValue('Review only')

    await findButton(wrapper, 'Save')?.trigger('click')
    await flushPromises()

    expect(createRoleMock).toHaveBeenCalledWith({
      roleCode: 'AUDITOR',
      roleName: 'Auditor',
      remark: 'Review only'
    })
    expect(listRolesMock).toHaveBeenCalledTimes(2)
    expect(getRoleDetailMock).toHaveBeenLastCalledWith(2)
  })
})
