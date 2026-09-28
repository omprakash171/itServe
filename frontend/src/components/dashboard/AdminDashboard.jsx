import KpiCard from '../KpiCard'
import { StatusPieChart, PriorityPieChart, CategoryBarChart, TimeSeriesChart } from './Charts'

export default function AdminDashboard({ data }) {
  const kpis = data.kpis

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="mb-0">System Dashboard</h5>
          <div className="text-secondary small">Live metrics across the service desk</div>
        </div>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="Total Tickets" value={kpis.totalTickets} icon="bi-ticket-detailed" />
        </div>
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="Open" value={kpis.openTickets} icon="bi-folder2-open" tone="text-primary" />
        </div>
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="In Progress" value={kpis.inProgress} icon="bi-arrow-repeat" tone="text-warning" />
        </div>
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="Resolved" value={kpis.resolved} icon="bi-check2-circle" tone="text-success" />
        </div>
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="SLA Breached" value={kpis.slaBreached} icon="bi-exclamation-triangle" tone="text-danger" />
        </div>
        <div className="col-6 col-md-4 col-xl-2">
          <KpiCard label="Critical" value={kpis.criticalTickets} icon="bi-exclamation-octagon" tone="text-danger" />
        </div>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-6 col-md-3">
          <KpiCard label="SLA Compliance" value={`${kpis.slaCompliancePct}%`} icon="bi-speedometer2" />
        </div>
        <div className="col-6 col-md-3">
          <KpiCard label="Avg Resolution" value={`${kpis.avgResolutionHours}h`} icon="bi-clock-history" />
        </div>
        <div className="col-6 col-md-3">
          <KpiCard label="Avg First Response" value={`${kpis.avgFirstResponseHours}h`} icon="bi-chat-left-dots" />
        </div>
        <div className="col-6 col-md-3">
          <KpiCard
            label="Avg Rating"
            value={kpis.avgRating == null ? '-' : `${kpis.avgRating} / 5`}
            icon="bi-star"
            tone="text-warning"
          />
        </div>
      </div>

      {data.incidentTrends?.length > 0 && (
        <div className="its-card border-warning mb-3">
          <div className="border-bottom border-warning-subtle px-3 py-2 fw-semibold small text-warning-emphasis">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            Potential Incidents (trend detection)
          </div>
          <div className="p-3">
            {data.incidentTrends.map((trend) => (
              <div key={trend.category} className="small mb-1">
                <span className="fw-semibold">{trend.category}</span> &mdash; {trend.message.split('. ')[1] || trend.message}
                <span className="text-secondary ms-2">
                  ({trend.previousCount} &rarr; {trend.currentCount} tickets, +{trend.increasePct}%)
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="row g-3 mb-3">
        <div className="col-lg-4">
          <div className="its-card p-3 h-100">
            <div className="fw-semibold small mb-2">Tickets by Status</div>
            <StatusPieChart data={data.byStatus} />
          </div>
        </div>
        <div className="col-lg-4">
          <div className="its-card p-3 h-100">
            <div className="fw-semibold small mb-2">Tickets by Priority</div>
            <PriorityPieChart data={data.byPriority} />
          </div>
        </div>
        <div className="col-lg-4">
          <div className="its-card p-3 h-100">
            <div className="fw-semibold small mb-2">Tickets by Category</div>
            <CategoryBarChart data={data.byCategory} />
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="its-card p-3 h-100">
            <div className="fw-semibold small mb-2">Created vs Resolved (last 14 days)</div>
            <TimeSeriesChart created={data.createdOverTime} resolved={data.resolvedOverTime} />
          </div>
        </div>
        <div className="col-lg-5">
          <div className="its-card p-3 h-100">
            <div className="fw-semibold small mb-2">Agent Workload</div>
            <div className="table-responsive">
              <table className="table table-sm its-table mb-0">
                <thead>
                  <tr>
                    <th>Agent</th>
                    <th className="text-center">Active</th>
                    <th className="text-center">In Prog.</th>
                    <th className="text-center">Resolved</th>
                    <th className="text-center">Breached</th>
                  </tr>
                </thead>
                <tbody>
                  {data.agentWorkload.map((agent) => (
                    <tr key={agent.agentId}>
                      <td>
                        <div className="fw-semibold">{agent.fullName}</div>
                        <div className="text-secondary" style={{ fontSize: '0.72rem' }}>
                          {agent.skills.join(', ')}
                        </div>
                      </td>
                      <td className="text-center">{agent.assigned}</td>
                      <td className="text-center">{agent.inProgress}</td>
                      <td className="text-center">{agent.resolved}</td>
                      <td className="text-center">
                        {agent.breached > 0 ? (
                          <span className="badge text-bg-danger">{agent.breached}</span>
                        ) : (
                          '0'
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
