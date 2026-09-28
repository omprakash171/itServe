import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchDashboard } from '../services/dashboardService'
import AdminDashboard from '../components/dashboard/AdminDashboard'
import AgentDashboard from '../components/dashboard/AgentDashboard'
import EmployeeDashboard from '../components/dashboard/EmployeeDashboard'

export default function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchDashboard()
      .then((result) => active && setData(result))
      .catch(() => active && setError('Unable to load dashboard data. Please refresh.'))
    return () => {
      active = false
    }
  }, [])

  if (error) {
    return <div className="alert alert-danger">{error}</div>
  }
  if (!data) {
    return (
      <div className="text-secondary py-5 text-center">
        <div className="spinner-border spinner-border-sm me-2" />
        Loading dashboard...
      </div>
    )
  }

  if (user.role === 'ADMIN') {
    return <AdminDashboard data={data} />
  }
  if (user.role === 'SUPPORT_AGENT') {
    return <AgentDashboard data={data} />
  }
  return <EmployeeDashboard data={data} />
}
