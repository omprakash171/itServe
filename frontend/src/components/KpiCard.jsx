export default function KpiCard({ label, value, icon, tone = 'text-dark', subtitle }) {
  return (
    <div className="its-card its-kpi p-3 h-100">
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <div className="kpi-label">{label}</div>
          <div className={`kpi-value mt-1 ${tone}`}>{value ?? '-'}</div>
          {subtitle && <div className="text-secondary small mt-1">{subtitle}</div>}
        </div>
        {icon && <i className={`bi ${icon} fs-4 text-secondary opacity-50`} />}
      </div>
    </div>
  )
}
