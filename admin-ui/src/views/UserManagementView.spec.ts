import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import UserManagementView from '@/views/UserManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listUsersMock, getUserDetailMock } = vi.hoisted(() => ({
  listUsersMock: vi.fn(),
  getUserDetailMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('@/api/users', () => ({
  createAdmin: vi.fn(),
  createStudent: vi.fn(),
  createTeacher: vi.fn(),
  deleteStudent: vi.fn(),
  getUserDetail: getUserDetailMock,
  listUsers: listUsersMock,
  resetPassword: vi.fn(),
  updateUserStatus: vi.fn()
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        userInfo: {
          roleCode: 'SUPER_ADMIN'
        }
      },
      currentUser: {
        roleCode: 'SUPER_ADMIN'
      }
    })
  })
}))

describe('UserManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getUserDetailMock.mockResolvedValue(null)
  })

  it('loads users and exposes create action for super admin', async () => {
    listUsersMock.mockResolvedValue({
      list: [
        {
          userId: 2,
          name: 'Admin Chen',
          account: 'admin_chen',
          jobNoOrStudentNo: 'A001',
          roleCode: 'ADMIN',
          creditScore: 100,
          status: 'ENABLED',
          firstLoginRequired: false
        }
      ],
      total: 1
    })

    const wrapper = mount(UserManagementView, {
      global: {
        stubs: elementPlusStubs,
        directives: {
          loading: {}
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('User Module')
    expect(wrapper.text()).toContain('Create User')
    expect(listUsersMock).toHaveBeenCalled()
    expect(wrapper.find('.el-table-stub').text()).toContain('Admin Chen')
  })
})
