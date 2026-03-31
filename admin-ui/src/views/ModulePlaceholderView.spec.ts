import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import ModulePlaceholderView from '@/views/ModulePlaceholderView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    fullPath: '/lab-assets',
    meta: {
      title: 'Lab Assets',
      description: 'Equipment asset registry'
    }
  })
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        userInfo: {
          roleCode: 'TEACHER'
        }
      },
      currentUser: {
        roleCode: 'TEACHER'
      }
    })
  })
}))

describe('ModulePlaceholderView', () => {
  it('renders route meta and current role', () => {
    const wrapper = mount(ModulePlaceholderView)

    expect(wrapper.text()).toContain('Lab Assets')
    expect(wrapper.text()).toContain('Equipment asset registry')
    expect(wrapper.text()).toContain('/lab-assets')
    expect(wrapper.text()).toContain('TEACHER')
  })
})
