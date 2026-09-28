import api from './api'

export async function listTickets(params) {
  const { data } = await api.get('/tickets', { params })
  return data
}

export async function getTicket(id) {
  const { data } = await api.get(`/tickets/${id}`)
  return data
}

export async function createTicket(payload) {
  const { data } = await api.post('/tickets', payload)
  return data
}

export async function updateTicket(id, payload) {
  const { data } = await api.put(`/tickets/${id}`, payload)
  return data
}

export async function updateTicketStatus(id, status, note) {
  const { data } = await api.patch(`/tickets/${id}/status`, { status, note })
  return data
}

export async function assignTicket(id, agentId) {
  const { data } = await api.patch(`/tickets/${id}/assign`, { agentId })
  return data
}

export async function updateTicketPriority(id, priority) {
  const { data } = await api.patch(`/tickets/${id}/priority`, { priority })
  return data
}

export async function escalateTicket(id, note) {
  const { data } = await api.patch(`/tickets/${id}/escalate`, { note })
  return data
}

export async function suggestedAgents(id) {
  const { data } = await api.get(`/tickets/${id}/suggested-agents`)
  return data
}

export async function deleteTicket(id) {
  await api.delete(`/tickets/${id}`)
}

export async function addComment(ticketId, message) {
  const { data } = await api.post(`/tickets/${ticketId}/comments`, { message })
  return data
}

export async function rateTicket(ticketId, stars, feedback) {
  const { data } = await api.post(`/tickets/${ticketId}/rating`, { stars, feedback })
  return data
}

export async function uploadAttachment(ticketId, file) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post(`/tickets/${ticketId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function listCategories() {
  const { data } = await api.get('/categories')
  return data
}
