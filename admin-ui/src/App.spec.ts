import { mount } from '@vue/test-utils'
import App from '@/App.vue'

describe('App', () => {
  it('renders the router outlet as the application shell', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          RouterView: {
            template: '<main class="router-view-stub">app route</main>'
          }
        }
      }
    })

    expect(wrapper.find('.router-view-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('app route')
  })
})
