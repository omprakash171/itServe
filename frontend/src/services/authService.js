import api from './api'

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password })
  return data
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload)
  return data
}

export async function fetchMe() {
  const { data } = await api.get('/auth/me')
  return data
}

export async function changePassword(currentPassword, newPassword) {
  const { data } = await api.patch('/auth/password', { currentPassword, newPassword })
  return data
}
