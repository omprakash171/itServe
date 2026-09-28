import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastProvider'
import { apiErrorMessage } from '../services/api'
import { changePassword, fetchMe } from '../services/authService'
import { formatDate, roleLabel } from '../utils/format'

const ROLE_BADGES = {
  ADMIN: 'bg-dark text-white',
  SUPPORT_AGENT: 'bg-primary-subtle text-primary-emphasis border border-primary-subtle',
  EMPLOYEE: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
}

export default function Profile() {
  const { user, setUser } = useAuth()
  const toast = useToast()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const handleChangePassword = async (e) => {
    e.preventDefault()
    setFormError('')
    if (newPassword.length < 8) {
      setFormError('New password must be at least 8 characters.')
      return
    }
    if (newPassword !== confirmPassword) {
      setFormError('New passwords do not match.')
      return
    }
    setSaving(true)
    try {
      await changePassword(currentPassword, newPassword)
      const fresh = await fetchMe()
      setUser(fresh)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      toast.show('Password updated.')
    } catch (err) {
      setFormError(apiErrorMessage(err, 'Unable to change password.'))
    } finally {
      setSaving(false)
    }
  }

  if (!user) return null

  return (
    <div style={{ maxWidth: 860 }}>
      <h1 className="h4 mb-3">My Profile</h1>

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="card its-card">
            <div className="card-body">
              <div className="d-flex align-items-center gap-3 mb-4">
                <div
                  className="d-flex align-items-center justify-content-center rounded-circle bg-primary text-white fw-bold"
                  style={{ width: 56, height: 56, fontSize: '1.25rem' }}
                >
                  {user.fullName
                    .split(' ')
                    .map((part) => part[0])
                    .slice(0, 2)
                    .join('')}
                </div>
                <div>
                  <div className="fw-semibold">{user.fullName}</div>
                  <span className={`badge its-badge ${ROLE_BADGES[user.role]}`}>{roleLabel(user.role)}</span>
                </div>
              </div>
              <dl className="row mb-0 small">
                <dt className="col-sm-4 text-secondary fw-normal">Email</dt>
                <dd className="col-sm-8">{user.email}</dd>
                <dt className="col-sm-4 text-secondary fw-normal">Department</dt>
                <dd className="col-sm-8">{user.department || '-'}</dd>
                {user.skills?.length > 0 && (
                  <>
                    <dt className="col-sm-4 text-secondary fw-normal">Skills</dt>
                    <dd className="col-sm-8">
                      {user.skills.map((skill) => (
                        <span key={skill} className="badge its-badge bg-light text-dark border me-1">
                          {skill}
                        </span>
                      ))}
                    </dd>
                  </>
                )}
                <dt className="col-sm-4 text-secondary fw-normal">Member since</dt>
                <dd className="col-sm-8">{formatDate(user.createdAt)}</dd>
              </dl>
            </div>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="card its-card">
            <div className="card-header bg-white fw-semibold small">Change Password</div>
            <div className="card-body">
              <form onSubmit={handleChangePassword}>
                {formError && <div className="alert alert-danger py-2 small">{formError}</div>}
                <div className="mb-3">
                  <label className="form-label small fw-semibold">Current password</label>
                  <input
                    type="password"
                    className="form-control form-control-sm"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                  />
                </div>
                <div className="mb-3">
                  <label className="form-label small fw-semibold">New password</label>
                  <input
                    type="password"
                    className="form-control form-control-sm"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                  <div className="form-text">Minimum 8 characters.</div>
                </div>
                <div className="mb-3">
                  <label className="form-label small fw-semibold">Confirm new password</label>
                  <input
                    type="password"
                    className="form-control form-control-sm"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                </div>
                <button type="submit" className="btn btn-sm btn-primary w-100" disabled={saving}>
                  {saving ? 'Updating…' : 'Update Password'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
