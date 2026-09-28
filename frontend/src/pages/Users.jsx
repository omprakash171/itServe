import { useCallback, useEffect, useState } from 'react'
import { useToast } from '../components/ToastProvider'
import { apiErrorMessage } from '../services/api'
import { createUser, deactivateUser, listUsers, updateUser } from '../services/dashboardService'
import { formatDate, roleLabel } from '../utils/format'

const ROLES = ['ADMIN', 'SUPPORT_AGENT', 'EMPLOYEE']

const ROLE_BADGES = {
  ADMIN: 'bg-dark text-white',
  SUPPORT_AGENT: 'bg-primary-subtle text-primary-emphasis border border-primary-subtle',
  EMPLOYEE: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
}

const EMPTY_FORM = {
  fullName: '',
  email: '',
  password: '',
  role: 'EMPLOYEE',
  department: '',
  skills: '',
  active: true,
}

export default function Users() {
  const toast = useToast()
  const [users, setUsers] = useState([])
  const [roleFilter, setRoleFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalUser, setModalUser] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setUsers(await listUsers(roleFilter || undefined))
    } catch (err) {
      setError(apiErrorMessage(err, 'Unable to load users.'))
    } finally {
      setLoading(false)
    }
  }, [roleFilter])

  useEffect(() => {
    load()
  }, [load])

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setFormErrors({})
    setModalUser('new')
  }

  const openEdit = (user) => {
    setForm({
      fullName: user.fullName,
      email: user.email,
      password: '',
      role: user.role,
      department: user.department || '',
      skills: (user.skills || []).join(', '),
      active: user.active,
    })
    setFormErrors({})
    setModalUser(user)
  }

  const setField = (name, value) => setForm((f) => ({ ...f, [name]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormErrors({})
    const skills = form.skills
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
    try {
      if (modalUser === 'new') {
        await createUser({
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          password: form.password,
          role: form.role,
          department: form.department.trim() || null,
          skills,
        })
        toast.show(`User ${form.email.trim()} created.`)
      } else {
        await updateUser(modalUser.id, {
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          department: form.department.trim() || null,
          skills,
          active: form.active,
        })
        toast.show('User updated.')
      }
      setModalUser(null)
      load()
    } catch (err) {
      const message = apiErrorMessage(err, 'Unable to save user.')
      if (err?.response?.data?.fieldErrors) {
        setFormErrors(err.response.data.fieldErrors)
      }
      setError(message)
      toast.show(message, 'danger')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (user) => {
    if (!window.confirm(`Deactivate ${user.fullName}? They will no longer be able to sign in.`)) return
    try {
      await deactivateUser(user.id)
      toast.show(`${user.fullName} deactivated.`, 'warning')
      load()
    } catch (err) {
      toast.show(apiErrorMessage(err, 'Unable to deactivate user.'), 'danger')
    }
  }

  const isEditing = modalUser !== null && modalUser !== 'new'

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <h1 className="h4 mb-1">User Management</h1>
          <p className="text-secondary mb-0 small">Create and manage admins, support agents and employees.</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={openCreate}>
          <i className="bi bi-person-plus me-1" />Add User
        </button>
      </div>

      <ul className="nav nav-pills mb-3 small">
        <li className="nav-item">
          <button className={`nav-link py-1 px-3 ${roleFilter === '' ? 'active' : ''}`} onClick={() => setRoleFilter('')}>
            All ({users.length})
          </button>
        </li>
        {ROLES.map((role) => (
          <li className="nav-item" key={role}>
            <button
              className={`nav-link py-1 px-3 ${roleFilter === role ? 'active' : ''}`}
              onClick={() => setRoleFilter(role)}
            >
              {roleLabel(role)}
            </button>
          </li>
        ))}
      </ul>

      {error && roleFilter === '' && <div className="alert alert-danger py-2 small">{error}</div>}

      <div className="card its-card">
        <div className="table-responsive">
          <table className="table its-table align-middle mb-0">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Department</th>
                <th>Skills</th>
                <th>Status</th>
                <th>Joined</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={8} className="text-center text-secondary py-4">
                    Loading users…
                  </td>
                </tr>
              )}
              {!loading && users.length === 0 && (
                <tr>
                  <td colSpan={8} className="text-center text-secondary py-4">
                    No users found.
                  </td>
                </tr>
              )}
              {!loading &&
                users.map((user) => (
                  <tr key={user.id} className={user.active ? '' : 'text-secondary'}>
                    <td className="fw-semibold">{user.fullName}</td>
                    <td>{user.email}</td>
                    <td>
                      <span className={`badge its-badge ${ROLE_BADGES[user.role]}`}>{roleLabel(user.role)}</span>
                    </td>
                    <td>{user.department || '-'}</td>
                    <td>
                      {user.skills?.length
                        ? user.skills.map((skill) => (
                            <span key={skill} className="badge its-badge bg-light text-dark border me-1">
                              {skill}
                            </span>
                          ))
                        : '-'}
                    </td>
                    <td>
                      {user.active ? (
                        <span className="badge its-badge bg-success-subtle text-success-emphasis border border-success-subtle">
                          Active
                        </span>
                      ) : (
                        <span className="badge its-badge bg-danger-subtle text-danger-emphasis border border-danger-subtle">
                          Inactive
                        </span>
                      )}
                    </td>
                    <td className="small">{formatDate(user.createdAt)}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-outline-secondary btn-sm me-2" onClick={() => openEdit(user)}>
                        <i className="bi bi-pencil me-1" />Edit
                      </button>
                      {user.active ? (
                        <button className="btn btn-outline-danger btn-sm" onClick={() => handleDeactivate(user)}>
                          Deactivate
                        </button>
                      ) : (
                        <button
                          className="btn btn-outline-success btn-sm"
                          onClick={async () => {
                            try {
                              await updateUser(user.id, {
                                fullName: user.fullName,
                                email: user.email,
                                department: user.department,
                                skills: user.skills,
                                active: true,
                              })
                              toast.show(`${user.fullName} reactivated.`)
                              load()
                            } catch (err) {
                              toast.show(apiErrorMessage(err, 'Unable to reactivate user.'), 'danger')
                            }
                          }}
                        >
                          Reactivate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {modalUser && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <form onSubmit={handleSubmit}>
                <div className="modal-header">
                  <h5 className="modal-title">{isEditing ? `Edit ${modalUser.fullName}` : 'Add User'}</h5>
                  <button type="button" className="btn-close" onClick={() => setModalUser(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Full name</label>
                    <input
                      className={`form-control form-control-sm ${formErrors.fullName ? 'is-invalid' : ''}`}
                      value={form.fullName}
                      onChange={(e) => setField('fullName', e.target.value)}
                      required
                    />
                    {formErrors.fullName && <div className="invalid-feedback d-block">{formErrors.fullName}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Email</label>
                    <input
                      type="email"
                      className={`form-control form-control-sm ${formErrors.email ? 'is-invalid' : ''}`}
                      value={form.email}
                      onChange={(e) => setField('email', e.target.value)}
                      required
                    />
                    {formErrors.email && <div className="invalid-feedback d-block">{formErrors.email}</div>}
                  </div>
                  {!isEditing && (
                    <div className="mb-3">
                      <label className="form-label small fw-semibold">Password</label>
                      <input
                        type="password"
                        className={`form-control form-control-sm ${formErrors.password ? 'is-invalid' : ''}`}
                        value={form.password}
                        onChange={(e) => setField('password', e.target.value)}
                        minLength={8}
                        required
                      />
                      <div className="form-text">Minimum 8 characters.</div>
                      {formErrors.password && <div className="invalid-feedback d-block">{formErrors.password}</div>}
                    </div>
                  )}
                  <div className="row g-3 mb-3">
                    <div className="col-sm-6">
                      <label className="form-label small fw-semibold">Role</label>
                      <select
                        className="form-select form-select-sm"
                        value={form.role}
                        onChange={(e) => setField('role', e.target.value)}
                        disabled={isEditing}
                      >
                        {ROLES.map((role) => (
                          <option key={role} value={role}>
                            {roleLabel(role)}
                          </option>
                        ))}
                      </select>
                      {isEditing && <div className="form-text">Role cannot be changed after creation.</div>}
                    </div>
                    <div className="col-sm-6">
                      <label className="form-label small fw-semibold">Department</label>
                      <input
                        className="form-control form-control-sm"
                        value={form.department}
                        onChange={(e) => setField('department', e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">
                      Skills <span className="text-secondary fw-normal">(comma-separated categories, for agents)</span>
                    </label>
                    <input
                      className="form-control form-control-sm"
                      value={form.skills}
                      onChange={(e) => setField('skills', e.target.value)}
                      placeholder="e.g. Network, VPN, Security"
                    />
                  </div>
                  {isEditing && (
                    <div className="form-check form-switch">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="user-active"
                        checked={form.active}
                        onChange={(e) => setField('active', e.target.checked)}
                      />
                      <label className="form-check-label small" htmlFor="user-active">
                        Active (can sign in)
                      </label>
                    </div>
                  )}
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setModalUser(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-sm btn-primary" disabled={saving}>
                    {saving ? 'Saving…' : isEditing ? 'Save Changes' : 'Create User'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
