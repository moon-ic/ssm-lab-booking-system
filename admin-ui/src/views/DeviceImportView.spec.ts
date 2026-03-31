import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { reactive } from 'vue'
import DeviceImportView from '@/views/DeviceImportView.vue'
import { elementPlusStubs } from '@/test/element-stubs'

const { importDeviceMock } = vi.hoisted(() => ({
  importDeviceMock: vi.fn()
}))
const createObjectURLMock = vi.fn(() => 'blob:preview')
const revokeObjectURLMock = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api/device-imports', () => ({
  importDevice: importDeviceMock
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
  return mount(DeviceImportView, {
    global: {
      stubs: elementPlusStubs
    }
  })
}

function findButton(wrapper: VueWrapper, text: string) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

describe('DeviceImportView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    importDeviceMock.mockResolvedValue({
      deviceId: 1001,
      deviceName: 'Flow Camera',
      deviceCode: 'EQ-2026-1888',
      status: 'AVAILABLE',
      location: 'Room 201',
      description: 'Imported from test'
    })
    vi.stubGlobal('URL', {
      createObjectURL: createObjectURLMock,
      revokeObjectURL: revokeObjectURLMock
    })
  })

  it('uploads a file and shows the import result', async () => {
    const wrapper = mountView()

    const file = new File(['binary'], 'camera.png', { type: 'image/png' })
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('Flow Camera')
    await inputs[1].setValue('Camera')
    await inputs[2].setValue('Room 201')
    await inputs[3].setValue('Imported from test')
    Object.defineProperty(inputs[4].element, 'files', {
      value: [file],
      configurable: true
    })
    await inputs[4].trigger('change')

    await findButton(wrapper, 'Import Device')?.trigger('click')
    await flushPromises()

    expect(importDeviceMock).toHaveBeenCalledWith({
      category: 'Camera',
      description: 'Imported from test',
      deviceName: 'Flow Camera',
      image: file,
      location: 'Room 201'
    })
    expect(createObjectURLMock).toHaveBeenCalledWith(file)
    expect(wrapper.text()).toContain('EQ-2026-1888')
  })
})
