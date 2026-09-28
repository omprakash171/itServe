const STATUS_LABELS = {
  OPEN: 'Open',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  WAITING_FOR_USER: 'Waiting for User',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  REOPENED: 'Reopened',
  ESCALATED: 'Escalated',
}

const STATUS_COLORS = {
  OPEN: 'bg-primary-subtle text-primary-emphasis border border-primary-subtle',
  ASSIGNED: 'bg-info-subtle text-info-emphasis border border-info-subtle',
  IN_PROGRESS: 'bg-warning-subtle text-warning-emphasis border border-warning-subtle',
  WAITING_FOR_USER: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
  RESOLVED: 'bg-success-subtle text-success-emphasis border border-success-subtle',
  CLOSED: 'bg-dark-subtle text-dark-emphasis border border-dark-subtle',
  REOPENED: 'bg-danger-subtle text-danger-emphasis border border-danger-subtle',
  ESCALATED: 'bg-danger text-white',
}

const PRIORITY_COLORS = {
  LOW: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
  MEDIUM: 'bg-info-subtle text-info-emphasis border border-info-subtle',
  HIGH: 'bg-warning-subtle text-warning-emphasis border border-warning-subtle',
  CRITICAL: 'bg-danger text-white',
}

export function statusLabel(status) {
  return STATUS_LABELS[status] || status
}

export function statusClass(status) {
  return STATUS_COLORS[status] || 'bg-secondary-subtle text-secondary-emphasis'
}

export function priorityClass(priority) {
  return PRIORITY_COLORS[priority] || 'bg-secondary-subtle text-secondary-emphasis'
}

export function formatDateTime(iso) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(iso) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function formatTime(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

/** "03h 42m remaining" / "BREACHED 2h ago" from a signed minute count. */
export function formatSlaRemaining(minutes) {
  if (minutes === null || minutes === undefined) return '-'
  const abs = Math.abs(minutes)
  const days = Math.floor(abs / 1440)
  const hours = Math.floor((abs % 1440) / 60)
  const mins = abs % 60
  const parts = []
  if (days > 0) parts.push(`${days}d`)
  if (hours > 0 || days > 0) parts.push(`${String(hours).padStart(2, '0')}h`)
  parts.push(`${String(mins).padStart(2, '0')}m`)
  const text = parts.join(' ')
  return minutes < 0 ? `Breached ${text} ago` : `${text} remaining`
}

export function roleLabel(role) {
  if (role === 'SUPPORT_AGENT') return 'Support Agent'
  if (role === 'ADMIN') return 'Admin'
  return 'Employee'
}

export const ACTIVE_STATUSES = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_USER', 'ESCALATED', 'REOPENED']
