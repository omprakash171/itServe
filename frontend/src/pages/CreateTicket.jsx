import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as ticketService from '../services/ticketService'
import { apiErrorMessage } from '../services/api'
import { useToast } from '../components/ToastProvider'

export default function CreateTicket() {
  const navigate = useNavigate()
  const toast = useToast()
  const [categories, setCategories] = useState([])
  const [attachment, setAttachment] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [errors, setErrors] = useState({})
  const [form, setForm] = useState({
    subject: '',
    description: '',
    categoryId: '',
    priority: 'MEDIUM',
    location: '',
    preferredContact: 'EMAIL',
  })

  useEffect(() => {
    ticketService.listCategories().then(setCategories).catch(() => setError('Could not load categories.'))
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  const validate = () => {
    const next = {}
    if (form.subject.trim().length < 5) next.subject = 'Subject must be at least 5 characters.'
    if (form.description.trim().length < 10) next.description = 'Please describe the issue in at least 10 characters.'
    if (!form.categoryId) next.categoryId = 'Select a category.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    if (!validate()) return

    setSubmitting(true)
    try {
      const ticket = await ticketService.createTicket({
        ...form,
        categoryId: Number(form.categoryId),
      })
      if (attachment) {
        await ticketService.uploadAttachment(ticket.id, attachment)
      }
      toast.show(`Ticket ${ticket.ticketNumber} created. We'll keep you updated.`)
      navigate(`/tickets/${ticket.id}`)
    } catch (err) {
      const data = err?.response?.data
      if (data?.fieldErrors) {
        setErrors(data.fieldErrors)
      }
      setError(apiErrorMessage(err, 'Unable to create the ticket. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: 720 }}>
      <div className="mb-3">
        <h5 className="mb-0">Raise a New Ticket</h5>
        <div className="text-secondary small">Describe the issue clearly so the right team can help fast</div>
      </div>

      <div className="its-card p-4">
        {error && <div className="alert alert-danger small py-2">{error}</div>}
        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label className="form-label small fw-semibold">Subject *</label>
            <input
              className={`form-control ${errors.subject ? 'is-invalid' : ''}`}
              name="subject"
              value={form.subject}
              onChange={handleChange}
              placeholder="e.g. VPN connection failing with error 800"
              maxLength={200}
            />
            <div className="invalid-feedback">{errors.subject}</div>
          </div>

          <div className="mb-3">
            <label className="form-label small fw-semibold">Description *</label>
            <textarea
              className={`form-control ${errors.description ? 'is-invalid' : ''}`}
              name="description"
              rows={5}
              value={form.description}
              onChange={handleChange}
              placeholder="What happened? When did it start? Any error messages? What have you already tried?"
              maxLength={4000}
            />
            <div className="invalid-feedback">{errors.description}</div>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label className="form-label small fw-semibold">Category *</label>
              <select
                className={`form-select ${errors.categoryId ? 'is-invalid' : ''}`}
                name="categoryId"
                value={form.categoryId}
                onChange={handleChange}
              >
                <option value="">Select a category...</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>{category.name}</option>
                ))}
              </select>
              <div className="invalid-feedback">{errors.categoryId}</div>
            </div>
            <div className="col-md-6">
              <label className="form-label small fw-semibold">Priority *</label>
              <select className="form-select" name="priority" value={form.priority} onChange={handleChange}>
                <option value="LOW">Low &mdash; minor inconvenience (SLA 24h)</option>
                <option value="MEDIUM">Medium &mdash; affecting my work (SLA 8h)</option>
                <option value="HIGH">High &mdash; urgent, blocked (SLA 4h)</option>
                <option value="CRITICAL">Critical &mdash; team/business down (SLA 2h)</option>
              </select>
            </div>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label className="form-label small fw-semibold">Location</label>
              <input
                className="form-control"
                name="location"
                value={form.location}
                onChange={handleChange}
                placeholder="e.g. HQ - 3rd floor / Remote"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label small fw-semibold">Preferred contact</label>
              <select className="form-select" name="preferredContact" value={form.preferredContact} onChange={handleChange}>
                <option value="EMAIL">Email</option>
                <option value="PHONE">Phone</option>
                <option value="CHAT">Chat</option>
              </select>
            </div>
          </div>

          <div className="mb-4">
            <label className="form-label small fw-semibold">Attachment (optional)</label>
            <input
              type="file"
              className="form-control"
              onChange={(event) => setAttachment(event.target.files[0] || null)}
            />
            <div className="form-text">Screenshot or log file, max 5 MB.</div>
          </div>

          <div className="d-flex gap-2">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Ticket'}
            </button>
            <button type="button" className="btn btn-light border" onClick={() => navigate(-1)} disabled={submitting}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
