import api from './api'

export async function fetchDashboard() {
  const { data } = await api.get('/dashboard/me')
  return data
}

export async function listUsers(role) {
  const { data } = await api.get('/users', { params: role ? { role } : {} })
  return data
}

export async function listAgents() {
  const { data } = await api.get('/agents')
  return data
}

export async function createUser(payload) {
  const { data } = await api.post('/users', payload)
  return data
}

export async function updateUser(id, payload) {
  const { data } = await api.put(`/users/${id}`, payload)
  return data
}

export async function deactivateUser(id) {
  await api.delete(`/users/${id}`)
}

export async function listNotifications(page = 0, size = 20) {
  const { data } = await api.get('/notifications', { params: { page, size } })
  return data
}

export async function unreadCount() {
  const { data } = await api.get('/notifications/unread-count')
  return data.count
}

export async function markNotificationRead(id) {
  await api.patch(`/notifications/${id}/read`)
}

export async function markAllNotificationsRead() {
  await api.patch('/notifications/read-all')
}

export async function listArticles(search) {
  const { data } = await api.get('/articles', { params: search ? { search } : {} })
  return data
}

export async function getArticle(id) {
  const { data } = await api.get(`/articles/${id}`)
  return data
}

export async function createArticle(payload) {
  const { data } = await api.post('/articles', payload)
  return data
}

export async function updateArticle(id, payload) {
  const { data } = await api.put(`/articles/${id}`, payload)
  return data
}

export async function voteArticle(id, helpful) {
  const { data } = await api.post(`/articles/${id}/vote`, { helpful })
  return data
}

export async function listSlaRules() {
  const { data } = await api.get('/sla-rules')
  return data
}

export async function updateSlaRule(id, resolutionHours) {
  const { data } = await api.put(`/sla-rules/${id}`, { resolutionHours })
  return data
}
