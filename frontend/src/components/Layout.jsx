import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import NotificationBell from './NotificationBell'
import { roleLabel } from '../utils/format'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: 'bi-grid-1x2', end: true },
  { to: '/tickets', label: 'Tickets', icon: 'bi-ticket-detailed' },
  { to: '/tickets/new', label: 'Raise Ticket', icon: 'bi-plus-circle', roles: ['EMPLOYEE', 'ADMIN'] },
  { to: '/knowledge-base', label: 'Knowledge Base', icon: 'bi-journal-text' },
  { to: '/users', label: 'Users', icon: 'bi-people', roles: ['ADMIN'] },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const visibleItems = NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(user.role))

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = user.fullName
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <div className="d-flex" style={{ minHeight: '100vh' }}>
      <aside className="its-sidebar d-none d-lg-flex flex-column p-3">
        <div className="brand px-2 mb-4">
          ITServe
          <small>Raise. Resolve. Track. Improve.</small>
        </div>
        <nav className="nav flex-column">
          <div className="nav-section">Service Desk</div>
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <i className={`bi ${item.icon}`} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto px-2">
          <div className="text-secondary small">Signed in as</div>
          <div className="text-white fw-semibold small">{user.fullName}</div>
          <div className="text-secondary small mb-2">{roleLabel(user.role)}</div>
        </div>
      </aside>

      <div className="flex-grow-1 d-flex flex-column" style={{ minWidth: 0 }}>
        <header className="its-topbar d-flex justify-content-between align-items-center px-3 px-lg-4 py-2">
          <div className="d-lg-none brand fw-bold">ITServe</div>
          <div className="d-none d-lg-block text-secondary small">
            Enterprise IT Service Desk
          </div>
          <div className="d-flex align-items-center gap-3">
            <NotificationBell />
            <div className="dropdown">
              <button
                className="btn btn-light d-flex align-items-center gap-2 rounded-pill border"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                <span className="rounded-circle bg-dark text-white d-inline-flex align-items-center justify-content-center" style={{ width: 28, height: 28, fontSize: '0.75rem' }}>
                  {initials}
                </span>
                <span className="d-none d-md-inline small fw-semibold">{user.fullName}</span>
                <i className="bi bi-chevron-down small" />
              </button>
              <ul className="dropdown-menu dropdown-menu-end shadow-sm">
                <li>
                  <NavLink to="/profile" className="dropdown-item">
                    <i className="bi bi-person me-2" />My Profile
                  </NavLink>
                </li>
                <li>
                  <hr className="dropdown-divider" />
                </li>
                <li>
                  <button className="dropdown-item text-danger" onClick={handleLogout}>
                    <i className="bi bi-box-arrow-right me-2" />Logout
                  </button>
                </li>
              </ul>
            </div>
          </div>
        </header>

        <nav className="d-lg-none bg-white border-bottom px-2 py-1 d-flex gap-1 overflow-auto">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `btn btn-sm text-nowrap ${isActive ? 'btn-primary' : 'btn-light'}`
              }
            >
              <i className={`bi ${item.icon} me-1`} />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main className="p-3 p-lg-4 flex-grow-1">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
