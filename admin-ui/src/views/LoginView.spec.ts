import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import LoginView from '@/views/LoginView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const replaceMock = vi.fn()
const signInMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: replaceMock
  }),
  useRoute: () => ({
    query: {}
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: null,
      currentUser: null,
      initialized: false
    }),
    signIn: signInMock
  })
}))

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders demo shortcuts and fills the login form', async () => {
    signInMock.mockResolvedValue({
      firstLoginRequired: false,
      userInfo: {
        name: 'Admin'
      }
    })

    const wrapper = mount(LoginView, {
      global: {
        stubs: elementPlusStubs
      }
    })

    expect(wrapper.text()).toContain('Equipment Borrowing Admin')
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThanOrEqual(4)

    await buttons[0].trigger('click')
    const inputs = wrapper.findAll('input')
    expect((inputs[0].element as HTMLInputElement).value).toBe('admin')
    expect((inputs[1].element as HTMLInputElement).value).toBe('000000')

    await buttons[3].trigger('click')
    await flushPromises()

    expect(signInMock).toHaveBeenCalled()
    expect(replaceMock).toHaveBeenCalledWith('/')
  })
})
