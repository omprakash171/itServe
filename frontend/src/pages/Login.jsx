import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { apiErrorMessage } from '../services/api'

const DEMO_ACCOUNTS = [
  { role: 'Admin', email: 'admin@itserve.com' },
  { role: 'Agent', email: 'agent1@itserve.com' },
  { role: 'Employee', email: 'employee1@itserve.com' },
]

export default function Login() {
  const { isAuthenticated, login, register } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({
    email: '',
    password: '',
    fullName: '',
    department: '',
  })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (!form.email.trim() || !form.password) {
      setError('Email and password are required.')
      return
    }
    if (mode === 'register') {
      if (form.fullName.trim().length < 2) {
        setError('Please enter your full name.')
        return
      }
      if (form.password.length < 8) {
        setError('Password must be at least 8 characters.')
        return
      }
    }

    setSubmitting(true)
    try {
      if (mode === 'login') {
        await login(form.email.trim(), form.password)
      } else {
        await register({
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          password: form.password,
          department: form.department.trim() || null,
        })
      }
      navigate('/')
    } catch (err) {
      setError(apiErrorMessage(err, mode === 'login' ? 'Login failed. Please try again.' : 'Registration failed. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="its-login-wrapper justify-content-center p-3">
      <div className="its-login-card its-card p-4">
        <div className="mb-4">
          <div className="fw-bold fs-4">
            <i className="bi bi-life-preserver me-2 text-primary" />
            ITServe
          </div>
          <div className="text-secondary small">Enterprise IT Service Desk &mdash; Raise. Resolve. Track. Improve.</div>
        </div>

        <ul className="nav nav-pills mb-3 w-100">
          <li className="nav-item flex-fill">
            <button
              className={`nav-link w-100 ${mode === 'login' ? 'active' : ''}`}
              onClick={() => { setMode('login'); setError('') }}
            >
              Sign In
            </button>
          </li>
          <li className="nav-item flex-fill">
            <button
              className={`nav-link w-100 ${mode === 'register' ? 'active' : ''}`}
              onClick={() => { setMode('register'); setError('') }}
            >
              Register
            </button>
          </li>
        </ul>

        {error && <div className="alert alert-danger py-2 small">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          {mode === 'register' && (
            <>
              <div className="mb-3">
                <label className="form-label small fw-semibold">Full name</label>
                <input className="form-control" name="fullName" value={form.fullName} onChange={handleChange} placeholder="Jane Doe" />
              </div>
              <div className="mb-3">
                <label className="form-label small fw-semibold">Department</label>
                <input className="form-control" name="department" value={form.department} onChange={handleChange} placeholder="Finance" />
              </div>
            </>
          )}
          <div className="mb-3">
            <label className="form-label small fw-semibold">Email</label>
            <input type="email" className="form-control" name="email" value={form.email} onChange={handleChange} placeholder="you@company.com" autoComplete="username" />
          </div>
          <div className="mb-3">
            <label className="form-label small fw-semibold">Password</label>
            <input type="password" className="form-control" name="password" value={form.password} onChange={handleChange} placeholder="********" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
            {mode === 'register' && (
              <div className="form-text">Minimum 8 characters. New accounts are created with the Employee role.</div>
            )}
          </div>
          <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
            {submitting ? 'Please wait...' : mode === 'login' ? 'Sign In' : 'Create Account'}
          </button>
        </form>

        {mode === 'login' && (
          <div className="mt-4 border-top pt-3">
            <div className="text-secondary small fw-semibold mb-2">Demo accounts (password: Password@123)</div>
            <div className="d-flex flex-column gap-1">
              {DEMO_ACCOUNTS.map((account) => (
                <button
                  key={account.email}
                  type="button"
                  className="btn btn-sm btn-light border text-start d-flex justify-content-between align-items-center"
                  onClick={() => setForm((current) => ({ ...current, email: account.email, password: 'Password@123' }))}
                >
                  <span className="font-monospace small">{account.email}</span>
                  <span className="badge text-bg-light border">{account.role}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
