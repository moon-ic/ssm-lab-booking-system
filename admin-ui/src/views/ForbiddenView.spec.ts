import { mount } from '@vue/test-utils'
import ForbiddenView from '@/views/ForbiddenView.vue'

describe('ForbiddenView', () => {
  it('renders access denied guidance and dashboard link', () => {
    const wrapper = mount(ForbiddenView, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a href="/"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('Access denied')
    expect(wrapper.text()).toContain('Back to dashboard')
    expect(wrapper.html()).toContain('href="/"')
  })
})
