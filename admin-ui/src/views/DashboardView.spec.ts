import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import DashboardView from '@/views/DashboardView.vue'

vi.mock('@/router/access', () => ({
  getAccessibleMenuItems: vi.fn(() => [
    { path: '/users', title: 'Users' },
    { path: '/devices', title: 'Devices' },
    { path: '/statistics', title: 'Statistics' }
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
    })
  })
}))

describe('DashboardView', () => {
  it('renders current user and visible menu summary', () => {
    const wrapper = mount(DashboardView)

    expect(wrapper.text()).toContain('Role-aware routing is now active')
    expect(wrapper.text()).toContain('Admin Chen')
    expect(wrapper.text()).toContain('ADMIN')
    expect(wrapper.text()).toContain('3 items')
    expect(wrapper.text()).toContain('Dashboard only until password change')
  })
})
