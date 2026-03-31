import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { reactive } from 'vue'
import BorrowRecordManagementView from '@/views/BorrowRecordManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listBorrowRecordsMock, listBorrowRemindersMock } = vi.hoisted(() => ({
  listBorrowRecordsMock: vi.fn(),
  listBorrowRemindersMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/borrow-records', () => ({
  listBorrowRecords: listBorrowRecordsMock,
  listBorrowReminders: listBorrowRemindersMock,
  markBorrowRecordOverdue: vi.fn(),
  pickupBorrowRecord: vi.fn(),
  returnBorrowRecord: vi.fn()
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
  return mount(BorrowRecordManagementView, {
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

describe('BorrowRecordManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listBorrowRecordsMock.mockResolvedValue({
      list: [
        {
          recordId: 9001,
          deviceName: 'Flow Camera',
          userName: 'Student Wang',
          status: 'BORROWING',
          pickupTime: '2026-04-10 09:00:00',
          expectedReturnTime: '2026-04-12 18:00:00',
          returnTime: null
        }
      ],
      total: 1
    })
    listBorrowRemindersMock.mockResolvedValue([
      {
        recordId: 9001,
        deviceName: 'Flow Camera',
        userName: 'Student Wang',
        status: 'BORROWING',
        expectedReturnTime: '2026-04-12 18:00:00',
        reminderType: 'ABOUT_TO_EXPIRE'
      }
    ])
  })

  it('loads borrow records and shows reminder drawer for admin users', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Borrow Module')
    expect(wrapper.text()).toContain('About To Expire')
    expect(wrapper.find('.el-table-stub').text()).toContain('Flow Camera')

    await findButton(wrapper, 'About To Expire')?.trigger('click')
    await flushPromises()

    expect(listBorrowRemindersMock).toHaveBeenCalledWith('ABOUT_TO_EXPIRE')
    expect(wrapper.text()).toContain('Student Wang')
    expect(wrapper.find('.el-drawer-stub').exists()).toBe(true)
  })
})
