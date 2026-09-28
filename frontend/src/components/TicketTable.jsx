import { useNavigate } from 'react-router-dom'
import StatusBadge from './StatusBadge'
import PriorityBadge from './PriorityBadge'
import SlaBadge from './SlaBadge'
import { formatDateTime } from '../utils/format'

export default function TicketTable({ tickets, sortBy, sortDir, onSort }) {
  const navigate = useNavigate()

  const sortIcon = (field) => {
    if (sortBy !== field) return 'bi-arrow-down-up text-secondary opacity-25'
    return sortDir === 'asc' ? 'bi-sort-up' : 'bi-sort-down'
  }

  const header = (field, label) => (
    <th style={{ cursor: 'pointer', userSelect: 'none' }} onClick={() => onSort(field)}>
      {label} <i className={`bi ${sortIcon(field)} ms-1`} />
    </th>
  )

  return (
    <div className="table-responsive">
      <table className="table its-table align-middle mb-0">
        <thead>
          <tr>
            <th>Ticket</th>
            <th>Subject</th>
            <th>Requester</th>
            <th>Agent</th>
            {header('status', 'Status')}
            {header('priority', 'Priority')}
            <th>SLA</th>
            {header('createdAt', 'Created')}
          </tr>
        </thead>
        <tbody>
          {tickets.length === 0 && (
            <tr>
              <td colSpan={8} className="text-center text-secondary py-4">
                No tickets match the current filters.
              </td>
            </tr>
          )}
          {tickets.map((ticket) => (
            <tr
              key={ticket.id}
              className="its-clickable-row"
              onClick={() => navigate(`/tickets/${ticket.id}`)}
            >
              <td className="font-monospace small">{ticket.ticketNumber}</td>
              <td className="text-truncate" style={{ maxWidth: 260 }}>
                <div className="fw-semibold text-truncate">{ticket.subject}</div>
                <div className="text-secondary small">{ticket.category?.name}</div>
              </td>
              <td className="small">{ticket.createdBy?.fullName}</td>
              <td className="small">{ticket.assignedAgent?.fullName || <span className="text-secondary">Unassigned</span>}</td>
              <td><StatusBadge status={ticket.status} /></td>
              <td><PriorityBadge priority={ticket.priority} /></td>
              <td className="small"><SlaBadge slaStatus={ticket.slaStatus} slaRemainingMinutes={ticket.slaRemainingMinutes} /></td>
              <td className="small text-secondary">{formatDateTime(ticket.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
