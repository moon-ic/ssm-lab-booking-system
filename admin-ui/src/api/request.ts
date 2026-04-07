import axios from 'axios'

const apiBase = (import.meta.env.VITE_API_BASE ?? '').replace(/\/+$/, '')
const assetBase = apiBase.endsWith('/api') ? apiBase.slice(0, -'/api'.length) : apiBase
const AUTH_STORAGE_KEY = 'admin-auth-session'

const request = axios.create({
  baseURL: apiBase,
  timeout: 10000
})

function readToken() {
  const directToken = localStorage.getItem('token')
  if (directToken) {
    return directToken
  }

  const rawSession = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!rawSession) {
    return null
  }

  try {
    return (JSON.parse(rawSession) as { token?: string }).token ?? null
  } catch {
    return null
  }
}

request.interceptors.request.use((config) => {
  const token = readToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  if (apiBase.endsWith('/api') && typeof config.url === 'string' && config.url.startsWith('/api/')) {
    config.url = config.url.slice('/api'.length)
  }

  return config
})

export function resolveApiAssetUrl(url?: string | null) {
  if (!url) {
    return url ?? undefined
  }

  if (/^(?:https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }

  if (!assetBase) {
    return url
  }

  return url.startsWith('/') ? `${assetBase}${url}` : `${assetBase}/${url}`
}

export function toApiAssetPath(url?: string | null) {
  if (!url || !assetBase) {
    return url ?? undefined
  }

  return url.startsWith(assetBase) ? url.slice(assetBase.length) || '/' : url
}

export default request
