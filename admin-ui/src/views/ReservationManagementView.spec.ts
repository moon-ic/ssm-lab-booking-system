import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import ReservationManagementView from '@/views/ReservationManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listReservationsMock, listDevicesMock } = vi.hoisted(() => ({
  listReservationsMock: vi.fn(),
  listDevicesMock: vi.fn()
}))

const authState = reactive({
  session: {
    userInfo: {
      roleCode: 'STUDENT',
      userId: 4
    }
  },
  currentUser: {
    roleCode: 'STUDENT',
    userId: 4
  }
})

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
    state: authState
  })
}))

describe('ReservationManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authState.session.userInfo.roleCode = 'STUDENT'
    authState.session.userInfo.userId = 4
    authState.currentUser.roleCode = 'STUDENT'
    authState.currentUser.userId = 4
  })

  it('loads reservations and exposes create action for students', async () => {
    listReservationsMock.mockResolvedValue({
      list: [
        {
          reservationId: 7001,
          applicantId: 4,
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

    expect(wrapper.text()).toContain('发起借用')
    expect(listReservationsMock).toHaveBeenCalled()
    expect(listDevicesMock).toHaveBeenCalled()
    expect(wrapper.find('.el-table-stub').text()).toContain('Flow Camera')
  })

  it('also exposes create action for teachers', async () => {
    authState.session.userInfo.roleCode = 'TEACHER'
    authState.session.userInfo.userId = 3
    authState.currentUser.roleCode = 'TEACHER'
    authState.currentUser.userId = 3

    listReservationsMock.mockResolvedValue({
      list: [],
      total: 0
    })
    listDevicesMock.mockResolvedValue({
      list: []
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

    expect(wrapper.text()).toContain('发起借用')
  })
})
