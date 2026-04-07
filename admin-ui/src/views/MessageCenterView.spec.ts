import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { reactive } from 'vue'
import MessageCenterView from '@/views/MessageCenterView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listMessagesMock, unconfirmedSummaryMock, confirmMessageMock, listMyMessagesMock, confirmMyMessageMock } = vi.hoisted(() => ({
  listMessagesMock: vi.fn(),
  unconfirmedSummaryMock: vi.fn(),
  confirmMessageMock: vi.fn(),
  listMyMessagesMock: vi.fn(),
  confirmMyMessageMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/messages', () => ({
  confirmMessage: confirmMessageMock,
  listMessages: listMessagesMock,
  unconfirmedSummary: unconfirmedSummaryMock
}))

vi.mock('@/api/profile', () => ({
  listMyMessages: listMyMessagesMock,
  confirmMyMessage: confirmMyMessageMock
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        userInfo: {
          roleCode: 'ADMIN'
        }
      },
      currentUser: {
        roleCode: 'ADMIN'
      }
    })
  })
}))

function mountView() {
  return mount(MessageCenterView, {
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

describe('MessageCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMessagesMock.mockResolvedValue({
      list: [
        {
          messageId: 3001,
          title: '预约即将到期',
          type: 'ABOUT_TO_EXPIRE_REMINDER',
          content: '请在截止前处理。',
          confirmStatus: 'UNCONFIRMED',
          createdAt: '2026-04-12 09:00'
        }
      ],
      total: 1
    })
    listMyMessagesMock.mockResolvedValue({
      list: [],
      total: 0
    })
    unconfirmedSummaryMock.mockResolvedValue({
      total: 6,
      aboutToExpireCount: 2,
      overdueCount: 1,
      firstLoginCount: 1
    })
    confirmMessageMock.mockResolvedValue(undefined)
    confirmMyMessageMock.mockResolvedValue(undefined)
  })

  it('loads message summary and confirms an unconfirmed message', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('未确认总数')
    expect(wrapper.text()).toContain('预约即将到期')

    await findButton(wrapper, '确认')?.trigger('click')
    await flushPromises()

    expect(confirmMessageMock).toHaveBeenCalledWith(3001)
    expect(listMessagesMock).toHaveBeenCalledTimes(2)
    expect(unconfirmedSummaryMock).toHaveBeenCalledTimes(2)
  })
})
