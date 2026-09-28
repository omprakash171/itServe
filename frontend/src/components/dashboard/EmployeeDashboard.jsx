import { Link } from 'react-router-dom'
import KpiCard from '../KpiCard'
import PriorityBadge from '../PriorityBadge'
import SlaBadge from '../SlaBadge'
import StatusBadge from '../StatusBadge'
import { formatDateTime } from '../../utils/format'

export default function EmployeeDashboard({ data }) {
  const kpis = data.kpis

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="mb-0">My Dashboard</h5>
          <div className="text-secondary small">Track your IT requests and raise new ones</div>
        </div>
        <Link to="/tickets/new" className="btn btn-primary btn-sm">
          <i className="bi bi-plus-lg me-1" />Raise New Ticket
        </Link>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-6 col-xl">
          <KpiCard label="My Open Tickets" value={kpis.myOpenTickets} icon="bi-folder2-open" tone="text-primary" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="Waiting for Me" value={kpis.myPendingTickets} icon="bi-hourglass-split" tone="text-warning" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="Resolved" value={kpis.myResolvedTickets} icon="bi-check2-circle" tone="text-success" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="Closed" value={kpis.myClosedTickets} icon="bi-archive" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="Total Raised" value={kpis.totalTickets} icon="bi-ticket-detailed" />
        </div>
      </div>

      <div className="its-card p-3">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <div className="fw-semibold small">Recent Tickets</div>
          <Link to="/tickets" className="small text-decoration-none">View all</Link>
        </div>
        <div className="table-responsive">
          <table className="table table-sm its-table align-middle mb-0">
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Subject</th>
                <th>Status</th>
                <th>Priority</th>
                <th>SLA</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {data.recentTickets.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center text-secondary py-4">
                    You haven't raised any tickets yet.
                  </td>
                </tr>
              )}
              {data.recentTickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td className="font-monospace small">
                    <Link to={`/tickets/${ticket.id}`} className="text-decoration-none">
                      {ticket.ticketNumber}
                    </Link>
                  </td>
                  <td className="text-truncate" style={{ maxWidth: 260 }}>{ticket.subject}</td>
                  <td><StatusBadge status={ticket.status} /></td>
                  <td><PriorityBadge priority={ticket.priority} /></td>
                  <td className="small">
                    <SlaBadge slaStatus={ticket.slaStatus} slaRemainingMinutes={ticket.slaRemainingMinutes} />
                  </td>
                  <td className="small text-secondary">{formatDateTime(ticket.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
