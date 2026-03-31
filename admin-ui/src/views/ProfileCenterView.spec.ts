import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import ProfileCenterView from '@/views/ProfileCenterView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { getProfileMock, listMyBorrowRecordsMock, listMyMessagesMock, confirmMyMessageMock } = vi.hoisted(() => ({
  getProfileMock: vi.fn(),
  listMyBorrowRecordsMock: vi.fn(),
  listMyMessagesMock: vi.fn(),
  confirmMyMessageMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/profile', () => ({
  confirmMyMessage: confirmMyMessageMock,
  getProfile: getProfileMock,
  listMyBorrowRecords: listMyBorrowRecordsMock,
  listMyMessages: listMyMessagesMock
}))

function mountView() {
  return mount(ProfileCenterView, {
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

describe('ProfileCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProfileMock.mockResolvedValue({
      name: 'Student Wang',
      roleCode: 'STUDENT',
      account: 'student_wang',
      jobNoOrStudentNo: '20260001',
      creditScore: 98,
      status: 'ENABLED',
      firstLoginRequired: false
    })
    listMyBorrowRecordsMock.mockResolvedValue({
      list: [
        {
          recordId: 9001,
          deviceName: 'Flow Camera',
          status: 'BORROWING',
          pickupTime: '2026-04-10 09:00:00',
          expectedReturnTime: '2026-04-12 18:00:00',
          returnTime: null,
          deviceCondition: 'NORMAL'
        }
      ],
      total: 1
    })
    listMyMessagesMock.mockResolvedValue({
      list: [
        {
          messageId: 3001,
          title: 'Borrow overdue',
          type: 'BORROW_OVERDUE',
          content: 'Please return as soon as possible.',
          confirmStatus: 'UNCONFIRMED',
          createdAt: '2026-04-12 09:00:00'
        }
      ],
      total: 1
    })
    confirmMyMessageMock.mockResolvedValue(undefined)
  })

  it('loads personal data and confirms a personal message', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Profile Center')
    expect(wrapper.text()).toContain('Student Wang')
    expect(wrapper.text()).toContain('Flow Camera')
    expect(wrapper.text()).toContain('Borrow overdue')

    await findButton(wrapper, 'Confirm')?.trigger('click')
    await flushPromises()

    expect(confirmMyMessageMock).toHaveBeenCalledWith(3001)
    expect(listMyMessagesMock).toHaveBeenCalledTimes(2)
  })
})
