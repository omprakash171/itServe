import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

const STATUS_COLORS = {
  OPEN: '#2563eb',
  ASSIGNED: '#0891b2',
  IN_PROGRESS: '#d97706',
  WAITING_FOR_USER: '#64748b',
  RESOLVED: '#059669',
  CLOSED: '#334155',
  REOPENED: '#dc2626',
  ESCALATED: '#9f1239',
}

const PRIORITY_COLORS = {
  LOW: '#94a3b8',
  MEDIUM: '#0891b2',
  HIGH: '#d97706',
  CRITICAL: '#dc2626',
}

const CATEGORY_COLOR = '#2563eb'

export function StatusPieChart({ data }) {
  const chartData = data.filter((d) => d.value > 0)
  return (
    <ResponsiveContainer width="100%" height={240}>
      <PieChart>
        <Pie data={chartData} dataKey="value" nameKey="label" innerRadius={50} outerRadius={85} paddingAngle={2}>
          {chartData.map((entry) => (
            <Cell key={entry.label} fill={STATUS_COLORS[entry.label] || '#94a3b8'} />
          ))}
        </Pie>
        <Tooltip />
        <Legend iconSize={9} wrapperStyle={{ fontSize: '0.75rem' }} />
      </PieChart>
    </ResponsiveContainer>
  )
}

export function PriorityPieChart({ data }) {
  const chartData = data.filter((d) => d.value > 0)
  return (
    <ResponsiveContainer width="100%" height={240}>
      <PieChart>
        <Pie data={chartData} dataKey="value" nameKey="label" outerRadius={85} paddingAngle={2}>
          {chartData.map((entry) => (
            <Cell key={entry.label} fill={PRIORITY_COLORS[entry.label] || '#94a3b8'} />
          ))}
        </Pie>
        <Tooltip />
        <Legend iconSize={9} wrapperStyle={{ fontSize: '0.75rem' }} />
      </PieChart>
    </ResponsiveContainer>
  )
}

export function CategoryBarChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e4e7ec" />
        <XAxis dataKey="label" tick={{ fontSize: 11 }} interval={0} angle={-20} textAnchor="end" height={50} />
        <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
        <Tooltip />
        <Bar dataKey="value" name="Tickets" fill={CATEGORY_COLOR} radius={[3, 3, 0, 0]} maxBarSize={36} />
      </BarChart>
    </ResponsiveContainer>
  )
}

export function TimeSeriesChart({ created, resolved }) {
  const data = created.map((point, index) => ({
    label: point.label.slice(5),
    created: point.value,
    resolved: resolved[index]?.value ?? 0,
  }))
  return (
    <ResponsiveContainer width="100%" height={240}>
      <LineChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e4e7ec" />
        <XAxis dataKey="label" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
        <Tooltip />
        <Legend iconSize={9} wrapperStyle={{ fontSize: '0.75rem' }} />
        <Line type="monotone" dataKey="created" name="Created" stroke="#2563eb" strokeWidth={2} dot={false} />
        <Line type="monotone" dataKey="resolved" name="Resolved" stroke="#059669" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  )
}
