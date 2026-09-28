import { Link } from 'react-router-dom'
import KpiCard from '../KpiCard'
import PriorityBadge from '../PriorityBadge'
import SlaBadge from '../SlaBadge'
import StatusBadge from '../StatusBadge'

export default function AgentDashboard({ data }) {
  const kpis = data.kpis

  return (
    <div>
      <div className="mb-3">
        <h5 className="mb-0">Agent Dashboard</h5>
        <div className="text-secondary small">Welcome back, {data.agentName}</div>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-6 col-xl">
          <KpiCard label="My Open Tickets" value={kpis.myOpenTickets} icon="bi-folder2-open" tone="text-primary" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="In Progress" value={kpis.myInProgress} icon="bi-arrow-repeat" tone="text-warning" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="My Resolved" value={kpis.myResolved} icon="bi-check2-circle" tone="text-success" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="SLA At Risk" value={kpis.slaAtRisk} icon="bi-hourglass-split" tone="text-warning" />
        </div>
        <div className="col-6 col-xl">
          <KpiCard label="SLA Breached" value={kpis.slaBreached} icon="bi-exclamation-triangle" tone="text-danger" />
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-8">
          <div className="its-card p-3">
            <div className="fw-semibold small mb-2">Today's Tasks (ordered by SLA deadline)</div>
            <div className="table-responsive">
              <table className="table table-sm its-table align-middle mb-0">
                <thead>
                  <tr>
                    <th>Ticket</th>
                    <th>Subject</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>SLA</th>
                  </tr>
                </thead>
                <tbody>
                  {data.todaysTasks.length === 0 && (
                    <tr>
                      <td colSpan={5} className="text-center text-secondary py-4">
                        No active tickets assigned to you. Enjoy the calm!
                      </td>
                    </tr>
                  )}
                  {data.todaysTasks.map((task) => (
                    <tr key={task.id}>
                      <td className="font-monospace small">
                        <Link to={`/tickets/${task.id}`} className="text-decoration-none">
                          {task.ticketNumber}
                        </Link>
                      </td>
                      <td className="text-truncate" style={{ maxWidth: 240 }}>{task.subject}</td>
                      <td><PriorityBadge priority={task.priority} /></td>
                      <td><StatusBadge status={task.status} /></td>
                      <td className="small">
                        <SlaBadge slaStatus={task.slaStatus} slaRemainingMinutes={task.slaRemainingMinutes} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className="its-card p-3">
            <div className="fw-semibold small mb-2">Need a hand?</div>
            <p className="text-secondary small mb-2">
              Browse self-service guides for common issues, or check the unassigned queue for tickets you can pick up.
            </p>
            <div className="d-grid gap-2">
              <Link to="/tickets?assignedTo=unassigned" className="btn btn-sm btn-outline-primary">
                <i className="bi bi-inbox me-1" />Unassigned queue
              </Link>
              <Link to="/knowledge-base" className="btn btn-sm btn-outline-secondary">
                <i className="bi bi-journal-text me-1" />Knowledge Base
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
