import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import TicketTable from '../components/TicketTable'
import * as ticketService from '../services/ticketService'
import * as dashboardService from '../services/dashboardService'
import { apiErrorMessage } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { ACTIVE_STATUSES } from '../utils/format'

const STATUSES = [...ACTIVE_STATUSES, 'RESOLVED', 'CLOSED']
const PAGE_SIZE = 10

export default function Tickets() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [categories, setCategories] = useState([])
  const [agents, setAgents] = useState([])
  const [page, setPage] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const filters = {
    search: searchParams.get('search') || '',
    status: searchParams.get('status') || '',
    priority: searchParams.get('priority') || '',
    categoryId: searchParams.get('categoryId') || '',
    assignedTo: searchParams.get('assignedTo') || '',
    sla: searchParams.get('sla') || '',
  }
  const pageIdx = Number(searchParams.get('page') || 0)
  const sortBy = searchParams.get('sortBy') || 'createdAt'
  const sortDir = searchParams.get('sortDir') || 'desc'

  const setParam = (key, value) => {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(key, value)
    } else {
      next.delete(key)
    }
    if (key !== 'page') {
      next.delete('page')
    }
    setSearchParams(next)
  }

  useEffect(() => {
    ticketService.listCategories().then(setCategories).catch(() => {})
    if (user.role !== 'EMPLOYEE') {
      dashboardService.listAgents().then(setAgents).catch(() => {})
    }
  }, [user.role])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = { page: pageIdx, size: PAGE_SIZE, sortBy, sortDir }
      Object.entries(filters).forEach(([key, value]) => {
        if (value) params[key] = value
      })
      const result = await ticketService.listTickets(params)
      setPage(result)
    } catch (err) {
      setError(apiErrorMessage(err, 'Unable to load tickets. Please try again.'))
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams.toString(), user.role])

  useEffect(() => {
    load()
  }, [load])

  const handleSort = (field) => {
    if (sortBy === field) {
      setParam('sortDir', sortDir === 'asc' ? 'desc' : 'asc')
    } else {
      const next = new URLSearchParams(searchParams)
      next.set('sortBy', field)
      next.set('sortDir', 'desc')
      setSearchParams(next)
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h5 className="mb-0">Tickets</h5>
          <div className="text-secondary small">
            {user.role === 'EMPLOYEE' ? 'Your support requests' : 'Search, filter and manage service desk tickets'}
          </div>
        </div>
        {(user.role === 'EMPLOYEE' || user.role === 'ADMIN') && (
          <Link to="/tickets/new" className="btn btn-primary btn-sm">
            <i className="bi bi-plus-lg me-1" />Raise New Ticket
          </Link>
        )}
      </div>

      <div className="its-card p-3 mb-3">
        <div className="row g-2">
          <div className="col-md-4">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white"><i className="bi bi-search" /></span>
              <input
                className="form-control"
                placeholder="Search ticket ID, subject, person..."
                defaultValue={filters.search}
                onKeyDown={(event) => event.key === 'Enter' && setParam('search', event.target.value)}
                onBlur={(event) => {
                  if (event.target.value !== filters.search) setParam('search', event.target.value)
                }}
              />
            </div>
          </div>
          <div className="col-6 col-md">
            <select className="form-select form-select-sm" value={filters.status} onChange={(e) => setParam('status', e.target.value)}>
              <option value="">All statuses</option>
              {STATUSES.map((status) => (
                <option key={status} value={status}>{status.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md">
            <select className="form-select form-select-sm" value={filters.priority} onChange={(e) => setParam('priority', e.target.value)}>
              <option value="">All priorities</option>
              {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md">
            <select className="form-select form-select-sm" value={filters.categoryId} onChange={(e) => setParam('categoryId', e.target.value)}>
              <option value="">All categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </div>
          {user.role !== 'EMPLOYEE' && (
            <div className="col-6 col-md">
              <select className="form-select form-select-sm" value={filters.assignedTo} onChange={(e) => setParam('assignedTo', e.target.value)}>
                <option value="">Any agent</option>
                <option value="unassigned">Unassigned</option>
                <option value="me">Assigned to me</option>
                {agents.map((agent) => (
                  <option key={agent.id} value={agent.id}>{agent.fullName}</option>
                ))}
              </select>
            </div>
          )}
          <div className="col-6 col-md">
            <select className="form-select form-select-sm" value={filters.sla} onChange={(e) => setParam('sla', e.target.value)}>
              <option value="">Any SLA</option>
              <option value="breached">Breached</option>
              <option value="at_risk">At risk</option>
            </select>
          </div>
        </div>
      </div>

      <div className="its-card">
        {error && <div className="alert alert-danger m-3 mb-0 small">{error}</div>}
        {loading && !page ? (
          <div className="text-secondary p-4 text-center">
            <span className="spinner-border spinner-border-sm me-2" />Loading tickets...
          </div>
        ) : (
          page && (
            <>
              <TicketTable tickets={page.content} sortBy={sortBy} sortDir={sortDir} onSort={handleSort} />
              <div className="d-flex justify-content-between align-items-center border-top px-3 py-2">
                <div className="text-secondary small">
                  {page.totalElements} ticket{page.totalElements === 1 ? '' : 's'} &middot; page {page.page + 1} of {Math.max(page.totalPages, 1)}
                </div>
                <div className="btn-group btn-group-sm">
                  <button className="btn btn-outline-secondary" disabled={pageIdx === 0} onClick={() => setParam('page', String(pageIdx - 1))}>
                    <i className="bi bi-chevron-left" /> Prev
                  </button>
                  <button
                    className="btn btn-outline-secondary"
                    disabled={pageIdx + 1 >= page.totalPages}
                    onClick={() => setParam('page', String(pageIdx + 1))}
                  >
                    Next <i className="bi bi-chevron-right" />
                  </button>
                </div>
              </div>
            </>
          )
        )}
      </div>
    </div>
  )
}
