import axios from 'axios'

const apiBase = (import.meta.env.VITE_API_BASE ?? '').replace(/\/+$/, '')

const request = axios.create({
  baseURL: apiBase,
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  if (apiBase.endsWith('/api') && typeof config.url === 'string' && config.url.startsWith('/api/')) {
    config.url = config.url.slice('/api'.length)
  }

  return config
})

export default request
