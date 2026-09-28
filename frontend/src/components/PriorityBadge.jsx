import { priorityClass } from '../utils/format'

export default function PriorityBadge({ priority }) {
  return (
    <span className={`its-badge ${priorityClass(priority)}`}>
      {priority === 'CRITICAL' && <i className="bi bi-exclamation-octagon me-1" />}
      {priority}
    </span>
  )
}
