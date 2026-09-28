import { formatSlaRemaining } from '../utils/format'

const CLASS_BY_STATUS = {
  OK: 'its-sla-ok',
  AT_RISK: 'its-sla-risk',
  BREACHED: 'its-sla-breached',
  MET: 'its-sla-met',
  NONE: 'text-secondary',
}

export default function SlaBadge({ slaStatus, slaRemainingMinutes }) {
  if (!slaStatus || slaStatus === 'NONE') {
    return <span className="text-secondary small">No SLA</span>
  }
  if (slaStatus === 'MET') {
    return <span className={CLASS_BY_STATUS.MET}>SLA met</span>
  }
  return (
    <span className={CLASS_BY_STATUS[slaStatus] || ''}>
      {slaStatus === 'BREACHED' && <i className="bi bi-exclamation-triangle-fill me-1" />}
      {formatSlaRemaining(slaRemainingMinutes)}
    </span>
  )
}
