import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { reactive } from 'vue'
import DeviceManagementView from '@/views/DeviceManagementView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { listDevicesMock, createDeviceMock, deleteDeviceMock, confirmMock } = vi.hoisted(() => ({
  listDevicesMock: vi.fn(),
  createDeviceMock: vi.fn(),
  deleteDeviceMock: vi.fn(),
  confirmMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    confirm: confirmMock
  }
}))

vi.mock('@/api/devices', () => ({
  createDevice: createDeviceMock,
  deleteDevice: deleteDeviceMock,
  deviceStatusOptions: () => ['AVAILABLE', 'BORROWED', 'DISABLED'],
  getDeviceDetail: vi.fn(),
  listDevices: listDevicesMock,
  updateDevice: vi.fn(),
  updateDeviceStatus: vi.fn()
}))

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    state: reactive({
      session: {
        userInfo: {
          roleCode: 'SUPER_ADMIN'
        }
      },
      currentUser: {
        roleCode: 'SUPER_ADMIN'
      }
    })
  })
}))

function mountView() {
  return mount(DeviceManagementView, {
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

describe('DeviceManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listDevicesMock.mockResolvedValue({
      list: [
        {
          deviceId: 1001,
          deviceName: 'Flow Camera',
          deviceCode: 'EQ-2026-1888',
          category: 'Camera',
          location: 'Room 201',
          status: 'AVAILABLE',
          imageUrl: '',
          description: '4K camera'
        }
      ],
      total: 1
    })
    createDeviceMock.mockResolvedValue({
      deviceId: 1002
    })
    deleteDeviceMock.mockResolvedValue(undefined)
    confirmMock.mockResolvedValue('confirm')
  })

  it('loads devices and submits a create request for admin users', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Device Module')
    expect(wrapper.text()).toContain('Create Device')
    expect(wrapper.find('.el-table-stub').text()).toContain('Flow Camera')

    await findButton(wrapper, 'Create Device')?.trigger('click')

    const inputs = wrapper.findAll('input')
    await inputs[1].setValue('New Projector')
    await inputs[2].setValue('EQ-2026-9001')
    await inputs[3].setValue('Projector')
    await inputs[4].setValue('Room 305')
    await inputs[5].setValue('https://example.com/projector.png')
    await inputs[6].setValue('Ceiling projector')

    await findButton(wrapper, 'Save')?.trigger('click')
    await flushPromises()

    expect(createDeviceMock).toHaveBeenCalledWith({
      category: 'Projector',
      description: 'Ceiling projector',
      deviceCode: 'EQ-2026-9001',
      deviceName: 'New Projector',
      imageUrl: 'https://example.com/projector.png',
      location: 'Room 305'
    })
    expect(listDevicesMock).toHaveBeenCalledTimes(2)
  })

  it('deletes a device for super admins after confirmation', async () => {
    const wrapper = mountView()
    await flushPromises()

    await findButton(wrapper, '删除')?.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalled()
    expect(deleteDeviceMock).toHaveBeenCalledWith(1001)
    expect(listDevicesMock).toHaveBeenCalledTimes(2)
  })
})
