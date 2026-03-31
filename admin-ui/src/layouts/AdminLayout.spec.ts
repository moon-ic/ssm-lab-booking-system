import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const { replaceMock, signOutMock, confirmMock } = vi.hoisted(() => ({
  replaceMock: vi.fn(),
  signOutMock: vi.fn(),
  confirmMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    path: '/devices'
  }),
  useRouter: () => ({
    replace: replaceMock
  })
}))

vi.mock('element-plus', () => ({
  ElMessageBox: {
    confirm: confirmMock
  }
}))

vi.mock('@/router/access', () => ({
  getAccessibleMenuItems: vi.fn(() => [
    {
      path: '/dashboard',
      title: 'Dashboard',
      description: 'Overview'
    },
    {
      path: '/devices',
      title: 'Devices',
      description: 'Inventory'
    }
  ])
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        firstLoginRequired: true,
        userInfo: {
          name: 'Admin Chen',
          roleCode: 'ADMIN'
        }
      },
      currentUser: {
        name: 'Admin Chen',
        roleCode: 'ADMIN'
      }
    }),
    signOut: signOutMock
  })
}))

describe('AdminLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    confirmMock.mockResolvedValue(undefined)
  })

  it('renders role-aware navigation and signs out through the router', async () => {
    const wrapper = mount(AdminLayout, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to" class="router-link-stub"><slot /></a>'
          },
          RouterView: {
            template: '<div class="router-view-stub">page</div>'
          },
          ForcePasswordDialog: {
            template: '<div class="force-password-dialog-stub" />'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('Lab Admin')
    expect(wrapper.text()).toContain('Admin Chen')
    expect(wrapper.text()).toContain('ADMIN')
    expect(wrapper.text()).toContain('First login is still pending')
    expect(wrapper.findAll('.router-link-stub')).toHaveLength(2)
    expect(wrapper.find('.nav-item.is-active').text()).toContain('Devices')

    await wrapper.find('.logout-button').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalled()
    expect(signOutMock).toHaveBeenCalled()
    expect(replaceMock).toHaveBeenCalledWith('/login')
  })
})
