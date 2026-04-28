import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { reactive } from 'vue'
import RepairManagementView from '@/views/RepairManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listRepairsMock, listDevicesMock, createRepairMock, listMyBorrowRecordsMock } = vi.hoisted(() => ({
  listRepairsMock: vi.fn(),
  listDevicesMock: vi.fn(),
  createRepairMock: vi.fn(),
  listMyBorrowRecordsMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    prompt: vi.fn()
  }
}))

vi.mock('@/api/devices', () => ({
  listDevices: listDevicesMock
}))

vi.mock('@/api/profile', () => ({
  listMyBorrowRecords: listMyBorrowRecordsMock
}))

vi.mock('@/api/repairs', () => ({
  createRepair: createRepairMock,
  getRepairDetail: vi.fn(),
  listRepairs: listRepairsMock,
  updateRepairStatus: vi.fn()
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        userInfo: {
          roleCode: 'STUDENT'
        }
      },
      currentUser: {
        roleCode: 'STUDENT'
      }
    })
  })
}))

function mountView() {
  return mount(RepairManagementView, {
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

describe('RepairManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listRepairsMock.mockResolvedValue({
      list: [
        {
          repairId: 6101,
          deviceName: 'Flow Camera',
          applicantName: 'Student Wang',
          status: 'PENDING',
          createdAt: '2026-04-12 10:00:00',
          updatedAt: '2026-04-12 10:00:00',
          description: 'Lens issue'
        }
      ],
      total: 1
    })
    listDevicesMock.mockResolvedValue({
      list: [
        {
          deviceId: 1001,
          deviceName: 'Flow Camera',
          deviceCode: 'EQ-2026-1888'
        },
        {
          deviceId: 1002,
          deviceName: 'Idle Projector',
          deviceCode: 'EQ-2026-1999'
        }
      ]
    })
    listMyBorrowRecordsMock.mockResolvedValue({
      list: [
        {
          recordId: 9001,
          reservationId: 7001,
          userId: 4,
          deviceId: 1001,
          deviceName: 'Flow Camera',
          status: 'BORROWING',
          expectedReturnTime: '2026-04-13 18:00:00'
        }
      ]
    })
    createRepairMock.mockResolvedValue({
      repairId: 6102
    })
  })

  it('loads repairs and submits a new repair request for students', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('维修模块')
    expect(wrapper.text()).toContain('发起报修')
    expect(wrapper.find('.el-table-stub').text()).toContain('Lens issue')

    await findButton(wrapper, '发起报修')?.trigger('click')

    const select = wrapper.findAll('select')[1]
    const descriptionInput = wrapper.findAll('input').at(0)

    await select.setValue('1001')
    await descriptionInput?.setValue('Screen keeps flickering')
    await findButton(wrapper, '提交')?.trigger('click')
    await flushPromises()

    expect(createRepairMock).toHaveBeenCalledWith({
      description: 'Screen keeps flickering',
      deviceId: '1001'
    })
    expect(listMyBorrowRecordsMock).toHaveBeenCalled()
    expect(listRepairsMock).toHaveBeenCalledTimes(2)
  })
})
