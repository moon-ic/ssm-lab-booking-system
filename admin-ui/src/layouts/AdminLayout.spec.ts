import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const { replaceMock, pushMock, signOutMock, confirmMock, unconfirmedSummaryMock } = vi.hoisted(() => ({
  replaceMock: vi.fn(),
  pushMock: vi.fn(),
  signOutMock: vi.fn(),
  confirmMock: vi.fn(),
  unconfirmedSummaryMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    path: '/devices',
    fullPath: '/devices',
    meta: {
      title: '设备管理',
      description: '浏览并维护设备信息。'
    }
  }),
  useRouter: () => ({
    replace: replaceMock,
    push: pushMock
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
      title: '首页',
      description: '概览'
    },
    {
      path: '/devices',
      title: '设备管理',
      description: '设备库存'
    }
  ]),
  findRouteMetaByPath: vi.fn()
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

vi.mock('@/api/messages', () => ({
  unconfirmedSummary: unconfirmedSummaryMock
}))

describe('AdminLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    confirmMock.mockResolvedValue(undefined)
    unconfirmedSummaryMock.mockResolvedValue({
      total: 3
    })
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

    await flushPromises()

    expect(wrapper.text()).toContain('实验室后台')
    expect(wrapper.text()).toContain('Admin Chen')
    expect(wrapper.text()).toContain('ADMIN')
    expect(wrapper.text()).toContain('设备管理')
    expect(wrapper.text()).toContain('浏览并维护设备信息。')
    expect(wrapper.text()).toContain('当前仍处于首次登录状态')
    expect(wrapper.findAll('.router-link-stub')).toHaveLength(2)
    expect(wrapper.find('.nav-item.is-active').text()).toContain('设备管理')
    expect(wrapper.find('.message-dot').exists()).toBe(true)
    expect(wrapper.find('.message-button').attributes('aria-label')).toBe('消息中心')

    await wrapper.find('.message-button').trigger('click')
    expect(pushMock).toHaveBeenCalledWith('/messages')

    await wrapper.find('.logout-button').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalled()
    expect(signOutMock).toHaveBeenCalled()
    expect(replaceMock).toHaveBeenCalledWith('/login')
  })
})
