import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import ReservationManagementView from '@/views/ReservationManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listReservationsMock, listDevicesMock } = vi.hoisted(() => ({
  listReservationsMock: vi.fn(),
  listDevicesMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    prompt: vi.fn(),
    confirm: vi.fn()
  }
}))

vi.mock('@/api/reservations', () => ({
  approveReservation: vi.fn(),
  cancelReservation: vi.fn(),
  createReservation: vi.fn(),
  getReservationDetail: vi.fn(),
  listReservations: listReservationsMock
}))

vi.mock('@/api/devices', () => ({
  listDevices: listDevicesMock
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

describe('ReservationManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads reservations and exposes create reservation for students', async () => {
    listReservationsMock.mockResolvedValue({
      list: [
        {
          reservationId: 7001,
          deviceName: 'Flow Camera',
          applicantName: 'Student Wang',
          startTime: '2026-04-10 09:00',
          endTime: '2026-04-10 18:00',
          status: 'PENDING',
          purpose: 'shooting'
        }
      ],
      total: 1
    })
    listDevicesMock.mockResolvedValue({
      list: [
        {
          deviceId: 1001,
          deviceName: 'Flow Camera',
          deviceCode: 'EQ-2026-1888',
          status: 'AVAILABLE'
        }
      ]
    })

    const wrapper = mount(ReservationManagementView, {
      global: {
        stubs: elementPlusStubs,
        directives: {
          loading: {}
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('发起预约')
    expect(listReservationsMock).toHaveBeenCalled()
    expect(listDevicesMock).toHaveBeenCalled()
    expect(wrapper.find('.el-table-stub').text()).toContain('Flow Camera')
  })
})
