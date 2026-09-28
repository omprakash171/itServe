import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('itserve_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config.url.includes('/auth/login')) {
      localStorage.removeItem('itserve_token')
      localStorage.removeItem('itserve_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function apiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  if (data?.message) {
    if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      return Object.entries(data.fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('. ')
    }
    return data.message
  }
  if (error?.code === 'ERR_NETWORK') {
    return 'Cannot reach the server. Please check that the backend is running.'
  }
  return fallback
}

export default api
