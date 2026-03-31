import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import StatisticsView from '@/views/StatisticsView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const {
  statisticsOverviewMock,
  hotDevicesMock,
  deviceDamageStatisticsMock,
  userViolationStatisticsMock
} = vi.hoisted(() => ({
  statisticsOverviewMock: vi.fn(),
  hotDevicesMock: vi.fn(),
  deviceDamageStatisticsMock: vi.fn(),
  userViolationStatisticsMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

vi.mock('@/api/statistics', () => ({
  deviceDamageStatistics: deviceDamageStatisticsMock,
  hotDevices: hotDevicesMock,
  statisticsOverview: statisticsOverviewMock,
  userViolationStatistics: userViolationStatisticsMock
}))

function mountView() {
  return mount(StatisticsView, {
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

describe('StatisticsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    statisticsOverviewMock.mockResolvedValue({
      deviceTotal: 50,
      availableDeviceTotal: 32,
      borrowingTotal: 6,
      pendingReservationTotal: 4,
      pendingRepairTotal: 3
    })
    hotDevicesMock.mockResolvedValue([
      {
        deviceId: 1001,
        deviceName: 'Flow Camera',
        deviceCode: 'EQ-2026-1888',
        borrowCount: 12
      }
    ])
    deviceDamageStatisticsMock.mockResolvedValue([
      {
        deviceId: 2001,
        deviceName: 'Studio Light',
        deviceCode: 'EQ-2026-2100',
        category: 'Lighting',
        damageCount: 4,
        status: 'REPAIRING'
      }
    ])
    userViolationStatisticsMock.mockResolvedValue([
      {
        userId: 3001,
        name: 'Student Wang',
        jobNoOrStudentNo: '20260001',
        overdueCount: 1,
        damageCount: 1,
        violationCount: 2
      }
    ])
  })

  it('loads overview and ranking data, then refreshes with the changed query', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Statistics Module')
    expect(wrapper.text()).toContain('Devices in system')
    expect(wrapper.text()).toContain('Flow Camera')
    expect(wrapper.text()).toContain('Student Wang')

    const selects = wrapper.findAll('select')
    await selects[0].setValue('MONTH')
    await selects[1].setValue('10')
    await findButton(wrapper, 'Refresh')?.trigger('click')
    await flushPromises()

    expect(hotDevicesMock).toHaveBeenLastCalledWith({
      rankScope: 'MONTH',
      topN: '10'
    })
    expect(deviceDamageStatisticsMock).toHaveBeenLastCalledWith({
      rankScope: 'MONTH',
      topN: '10'
    })
    expect(userViolationStatisticsMock).toHaveBeenLastCalledWith({
      rankScope: 'MONTH',
      topN: '10'
    })
  })
})
