import request from '@/api/request'
import { mockImportDevice } from '@/mock/devices'
import type { ApiResult } from '@/types/auth'
import type { DeviceItem } from '@/types/device'

const useMock = import.meta.env.VITE_ENABLE_MOCK !== 'false'

function getSessionToken() {
  const raw = localStorage.getItem('admin-auth-session')
  if (!raw) {
    throw new Error('No active session')
  }

  try {
    return (JSON.parse(raw) as { token: string }).token
  } catch {
    throw new Error('Invalid session')
  }
}

async function unwrapResult<T>(promise: Promise<{ data: ApiResult<T> }>) {
  const response = await promise
  if (response.data.code !== 0) {
    throw new Error(response.data.message || 'Request failed')
  }
  return response.data.data
}

export async function importDevice(payload: {
  deviceName: string
  category?: string
  location?: string
  description?: string
  image: File
}) {
  if (useMock) {
    return mockImportDevice(getSessionToken(), {
      deviceName: payload.deviceName,
      category: payload.category,
      location: payload.location,
      description: payload.description,
      imageName: payload.image.name
    })
  }

  const formData = new FormData()
  formData.append('deviceName', payload.deviceName)
  if (payload.category) {
    formData.append('category', payload.category)
  }
  if (payload.location) {
    formData.append('location', payload.location)
  }
  if (payload.description) {
    formData.append('description', payload.description)
  }
  formData.append('image', payload.image)

  return unwrapResult(
    request.post<ApiResult<DeviceItem>>('/api/device-imports', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  )
}
