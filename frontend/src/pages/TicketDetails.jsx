import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import PriorityBadge from '../components/PriorityBadge'
import SlaBadge from '../components/SlaBadge'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../components/ToastProvider'
import { apiErrorMessage } from '../services/api'
import * as ticketService from '../services/ticketService'
import { formatDateTime, roleLabel, statusLabel } from '../utils/format'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

const TRANSITIONS = {
  OPEN: ['ASSIGNED', 'IN_PROGRESS', 'ESCALATED'],
  ASSIGNED: ['IN_PROGRESS', 'ESCALATED'],
  IN_PROGRESS: ['WAITING_FOR_USER', 'RESOLVED', 'ESCALATED'],
  WAITING_FOR_USER: ['IN_PROGRESS', 'RESOLVED'],
  ESCALATED: ['ASSIGNED', 'IN_PROGRESS'],
  REOPENED: ['ASSIGNED', 'IN_PROGRESS'],
  RESOLVED: ['CLOSED', 'REOPENED'],
  CLOSED: [],
}

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export default function TicketDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()

  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [comment, setComment] = useState('')
  const [sendingComment, setSendingComment] = useState(false)
  const [statusModal, setStatusModal] = useState(null) // { target }
  const [statusNote, setStatusNote] = useState('')
  const [assignModal, setAssignModal] = useState(false)
  const [suggestions, setSuggestions] = useState([])
  const [selectedAgent, setSelectedAgent] = useState('')
  const [ratingModal, setRatingModal] = useState(false)
  const [stars, setStars] = useState(5)
  const [ratingFeedback, setRatingFeedback] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      const data = await ticketService.getTicket(id)
      setDetail(data)
      setError('')
    } catch (err) {
      setError(apiErrorMessage(err, 'Unable to load this ticket.'))
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  const ticket = detail?.ticket

  const availableTransitions = useMemo(() => {
    if (!ticket) return []
    const all = TRANSITIONS[ticket.status] || []
    if (user.role === 'EMPLOYEE') {
      const isOwner = ticket.createdBy?.id === user.id
      return isOwner ? all.filter((s) => s === 'CLOSED' || s === 'REOPENED') : []
    }
    if (user.role === 'SUPPORT_AGENT') {
      return all.filter((s) => s !== 'CLOSED' && s !== 'REOPENED' && s !== 'ASSIGNED')
    }
    return all
  }, [ticket, user])

  const runAction = async (fn, successMessage) => {
    setBusy(true)
    setActionError('')
    try {
      await fn()
      await load()
      if (successMessage) toast.show(successMessage)
      return true
    } catch (err) {
      setActionError(apiErrorMessage(err, 'Unable to update ticket. Please try again.'))
      return false
    } finally {
      setBusy(false)
    }
  }

  const handleStatusClick = (target) => {
    if (target === 'RESOLVED' || target === 'ESCALATED' || target === 'REOPENED') {
      setStatusNote('')
      setStatusModal({ target })
    } else {
      runAction(() => ticketService.updateTicketStatus(ticket.id, target), `Ticket moved to ${statusLabel(target)}.`)
    }
  }

  const confirmStatusModal = async () => {
    const ok = await runAction(
      () => ticketService.updateTicketStatus(ticket.id, statusModal.target, statusNote),
      `Ticket moved to ${statusLabel(statusModal.target)}.`
    )
    if (ok) setStatusModal(null)
  }

  const openAssignModal = async () => {
    setAssignModal(true)
    setSelectedAgent('')
    try {
      const data = await ticketService.suggestedAgents(ticket.id)
      setSuggestions(data)
      const best = data.find((s) => s.skillMatch) || data[0]
      if (best) setSelectedAgent(String(best.agentId))
    } catch {
      setSuggestions([])
    }
  }

  const confirmAssign = async () => {
    if (!selectedAgent) return
    const ok = await runAction(
      () => ticketService.assignTicket(ticket.id, Number(selectedAgent)),
      'Ticket assigned.'
    )
    if (ok) setAssignModal(false)
  }

  const handlePriorityChange = async (priority) => {
    await runAction(() => ticketService.updateTicketPriority(ticket.id, priority), `Priority set to ${priority}. SLA recalculated.`)
  }

  const handleComment = async (event) => {
    event.preventDefault()
    if (!comment.trim()) return
    setSendingComment(true)
    try {
      await ticketService.addComment(ticket.id, comment.trim())
      setComment('')
      await load()
    } catch (err) {
      setActionError(apiErrorMessage(err, 'Unable to add comment.'))
    } finally {
      setSendingComment(false)
    }
  }

  const handleAttachment = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    await runAction(() => ticketService.uploadAttachment(ticket.id, file), 'Attachment uploaded.')
    event.target.value = ''
  }

  const downloadAttachment = async (attachment) => {
    try {
      const response = await api.get(`/tickets/attachments/${attachment.id}`, { responseType: 'blob' })
      const url = URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = attachment.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      toast.show('Unable to download the attachment.', 'danger')
    }
  }

  const submitRating = async () => {
    const ok = await runAction(
      () => ticketService.rateTicket(ticket.id, stars, ratingFeedback),
      'Thanks for rating the support!'
    )
    if (ok) setRatingModal(false)
  }

  const timeline = useMemo(() => {
    if (!detail) return []
    const items = [
      ...detail.history.map((h) => ({ at: h.createdAt, kind: 'history', data: h })),
      ...detail.comments.map((c) => ({ at: c.createdAt, kind: 'comment', data: c })),
    ]
    items.sort((a, b) => new Date(a.at) - new Date(b.at))
    return items
  }, [detail])

  if (error) {
    return <div className="alert alert-danger">{error}</div>
  }
  if (!detail || !ticket) {
    return (
      <div className="text-secondary py-5 text-center">
        <span className="spinner-border spinner-border-sm me-2" />Loading ticket...
      </div>
    )
  }

  const canComment = ticket.status !== 'CLOSED'
  const canRate =
    user.role === 'EMPLOYEE' &&
    ticket.createdBy?.id === user.id &&
    (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') &&
    !ticket.rated

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <div className="text-secondary small font-monospace">
            <Link to="/tickets" className="text-decoration-none">Tickets</Link> / {ticket.ticketNumber}
          </div>
          <h5 className="mb-1">{ticket.subject}</h5>
          <div className="d-flex flex-wrap gap-2 align-items-center">
            <StatusBadge status={ticket.status} />
            <PriorityBadge priority={ticket.priority} />
            <span className="its-badge bg-light text-dark border">{ticket.category?.name}</span>
            <SlaBadge slaStatus={ticket.slaStatus} slaRemainingMinutes={ticket.slaRemainingMinutes} />
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {user.role === 'ADMIN' && ticket.status !== 'CLOSED' && (
            <button className="btn btn-sm btn-outline-primary" onClick={openAssignModal} disabled={busy}>
              <i className="bi bi-person-check me-1" />{ticket.assignedAgent ? 'Reassign' : 'Assign'}
            </button>
          )}
          {canRate && (
            <button className="btn btn-sm btn-warning" onClick={() => setRatingModal(true)}>
              <i className="bi bi-star me-1" />Rate Support
            </button>
          )}
          {ticket.rated && (
            <span className="btn btn-sm btn-light border disabled text-warning">
              {'★'.repeat(ticket.ratingStars || 0)}<span className="text-secondary ms-1">rated</span>
            </span>
          )}
        </div>
      </div>

      {actionError && <div className="alert alert-danger small py-2">{actionError}</div>}

      <div className="row g-3">
        <div className="col-lg-8">
          <div className="its-card p-3 mb-3">
            <div className="fw-semibold small text-secondary mb-2">DESCRIPTION</div>
            <div style={{ whiteSpace: 'pre-wrap' }}>{ticket.description}</div>

            {ticket.attachments?.length > 0 && (
              <div className="mt-3 border-top pt-2">
                <div className="fw-semibold small text-secondary mb-1">ATTACHMENTS</div>
                {ticket.attachments.map((attachment) => (
                  <button
                    key={attachment.id}
                    className="btn btn-sm btn-light border me-2 mb-1"
                    onClick={() => downloadAttachment(attachment)}
                  >
                    <i className="bi bi-paperclip me-1" />{attachment.fileName}
                    <span className="text-secondary ms-1">({Math.max(1, Math.round(attachment.sizeBytes / 1024))} KB)</span>
                  </button>
                ))}
              </div>
            )}

            {ticket.resolutionNote && (
              <div className="mt-3 border-top pt-2">
                <div className="fw-semibold small text-secondary mb-1">RESOLUTION NOTE</div>
                <div className="small">{ticket.resolutionNote}</div>
              </div>
            )}

            {canComment && ticket.status !== 'CLOSED' && (
              <div className="mt-3 border-top pt-2">
                <input type="file" className="form-control form-control-sm" style={{ maxWidth: 320 }} onChange={handleAttachment} />
              </div>
            )}
          </div>

          <div className="its-card p-3">
            <div className="fw-semibold small text-secondary mb-3">ACTIVITY</div>
            <ul className="its-timeline">
              {timeline.map((item, index) =>
                item.kind === 'history' ? (
                  <li key={`h-${item.data.id}`} className={index === timeline.length - 1 ? 'tl-highlight' : ''}>
                    <div className="small fw-semibold">{item.data.action.replace(/_/g, ' ').toLowerCase()}</div>
                    {item.data.detail && <div className="small text-secondary">{item.data.detail}</div>}
                    <div className="text-secondary" style={{ fontSize: '0.72rem' }}>
                      {formatDateTime(item.data.createdAt)} &middot; {item.data.performedBy?.fullName}
                    </div>
                  </li>
                ) : (
                  <li key={`c-${item.data.id}`}>
                    <div className="small">
                      <span className="fw-semibold">{item.data.author?.fullName}</span>{' '}
                      <span className="badge text-bg-light border ms-1">{roleLabel(item.data.authorRole)}</span>
                    </div>
                    <div className="small bg-light border rounded p-2 mt-1">{item.data.message}</div>
                    <div className="text-secondary" style={{ fontSize: '0.72rem' }}>{formatDateTime(item.data.createdAt)}</div>
                  </li>
                )
              )}
            </ul>

            {canComment && (
              <form onSubmit={handleComment} className="mt-3 border-top pt-3">
                <label className="form-label small fw-semibold">Add Comment</label>
                <div className="input-group">
                  <input
                    className="form-control"
                    placeholder="Write an update..."
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    maxLength={2000}
                  />
                  <button className="btn btn-primary" type="submit" disabled={sendingComment || !comment.trim()}>
                    Send
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>

        <div className="col-lg-4">
          <div className="its-card p-3 mb-3">
            <div className="fw-semibold small text-secondary mb-2">DETAILS</div>
            <dl className="row small mb-0">
              <dt className="col-5 fw-semibold text-secondary">Created by</dt>
              <dd className="col-7">{ticket.createdBy?.fullName}<div className="text-secondary" style={{ fontSize: '0.75rem' }}>{ticket.createdBy?.email}</div></dd>
              <dt className="col-5 fw-semibold text-secondary">Assigned to</dt>
              <dd className="col-7">
                {ticket.assignedAgent ? (
                  <>
                    {ticket.assignedAgent.fullName}
                    <div className="text-secondary" style={{ fontSize: '0.75rem' }}>{roleLabel('SUPPORT_AGENT')}</div>
                  </>
                ) : (
                  <span className="text-secondary">Unassigned</span>
                )}
              </dd>
              <dt className="col-5 fw-semibold text-secondary">Created</dt>
              <dd className="col-7">{formatDateTime(ticket.createdAt)}</dd>
              <dt className="col-5 fw-semibold text-secondary">SLA deadline</dt>
              <dd className="col-7">{formatDateTime(ticket.slaDeadline)}</dd>
              {ticket.firstResponseAt && (
                <>
                  <dt className="col-5 fw-semibold text-secondary">First response</dt>
                  <dd className="col-7">{formatDateTime(ticket.firstResponseAt)}</dd>
                </>
              )}
              {ticket.resolvedAt && (
                <>
                  <dt className="col-5 fw-semibold text-secondary">Resolved</dt>
                  <dd className="col-7">{formatDateTime(ticket.resolvedAt)}</dd>
                </>
              )}
              <dt className="col-5 fw-semibold text-secondary">Location</dt>
              <dd className="col-7">{ticket.location || '-'}</dd>
              <dt className="col-5 fw-semibold text-secondary">Contact via</dt>
              <dd className="col-7">{ticket.preferredContact || 'EMAIL'}</dd>
            </dl>
          </div>

          <div className="its-card p-3">
            <div className="fw-semibold small text-secondary mb-2">UPDATE STATUS</div>
            {availableTransitions.length === 0 && (
              <div className="text-secondary small">No actions available for you on this ticket.</div>
            )}
            <div className="d-flex flex-column gap-2">
              {availableTransitions.map((target) => (
                <button
                  key={target}
                  className={`btn btn-sm ${target === 'RESOLVED' ? 'btn-success' : target === 'CLOSED' ? 'btn-dark' : target === 'ESCALATED' ? 'btn-outline-danger' : target === 'REOPENED' ? 'btn-outline-warning' : 'btn-outline-primary'}`}
                  onClick={() => handleStatusClick(target)}
                  disabled={busy}
                >
                  {target === 'RESOLVED' && <i className="bi bi-check2-circle me-1" />}
                  {target === 'CLOSED' && <i className="bi bi-archive me-1" />}
                  {target === 'ESCALATED' && <i className="bi bi-arrow-up-circle me-1" />}
                  {target === 'REOPENED' && <i className="bi bi-arrow-counterclockwise me-1" />}
                  Move to {statusLabel(target)}
                </button>
              ))}
            </div>

            {user.role === 'ADMIN' && ticket.status !== 'CLOSED' && ticket.status !== 'RESOLVED' && (
              <div className="mt-3 border-top pt-2">
                <div className="fw-semibold small text-secondary mb-1">Priority (admin)</div>
                <select
                  className="form-select form-select-sm"
                  value={ticket.priority}
                  onChange={(event) => handlePriorityChange(event.target.value)}
                  disabled={busy}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Status note modal (resolve / escalate / reopen) */}
      {statusModal && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h6 className="modal-title">
                  {statusModal.target === 'RESOLVED' ? 'Resolve ticket' : statusModal.target === 'ESCALATED' ? 'Escalate ticket' : 'Reopen ticket'}
                </h6>
                <button type="button" className="btn-close" onClick={() => setStatusModal(null)} />
              </div>
              <div className="modal-body">
                <label className="form-label small fw-semibold">
                  {statusModal.target === 'RESOLVED' ? 'Resolution note *' : 'Reason (optional)'}
                </label>
                <textarea
                  className="form-control"
                  rows={4}
                  value={statusNote}
                  onChange={(event) => setStatusNote(event.target.value)}
                  placeholder={
                    statusModal.target === 'RESOLVED'
                      ? 'What was the root cause and how was it fixed?'
                      : 'Why is this being changed?'
                  }
                />
                {statusModal.target === 'RESOLVED' && (
                  <div className="form-text">The requester will be notified and asked to confirm.</div>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-light border" onClick={() => setStatusModal(null)}>Cancel</button>
                <button
                  className="btn btn-primary"
                  onClick={confirmStatusModal}
                  disabled={busy || (statusModal.target === 'RESOLVED' && !statusNote.trim())}
                >
                  Confirm
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Assign modal */}
      {assignModal && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h6 className="modal-title">Assign to support agent</h6>
                <button type="button" className="btn-close" onClick={() => setAssignModal(false)} />
              </div>
              <div className="modal-body">
                <div className="text-secondary small mb-2">
                  Ranked by skill match with <strong>{ticket.category?.name}</strong> and current workload.
                </div>
                <div className="list-group">
                  {suggestions.map((agent) => (
                    <label
                      key={agent.agentId}
                      className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${selectedAgent === String(agent.agentId) ? 'active' : ''}`}
                    >
                      <span>
                        <input
                          type="radio"
                          name="agent"
                          className="form-check-input me-2"
                          checked={selectedAgent === String(agent.agentId)}
                          onChange={() => setSelectedAgent(String(agent.agentId))}
                        />
                        <span className="fw-semibold">{agent.fullName}</span>
                        {agent.skillMatch && <span className="badge text-bg-success ms-2">Skill match</span>}
                        <div className="small mt-1 opacity-75">{agent.skills.join(', ') || 'No specific skills'}</div>
                      </span>
                      <span className="text-end small">
                        <div>{agent.activeTickets} active</div>
                        {agent.breachedTickets > 0 && <div className="text-danger">{agent.breachedTickets} breached</div>}
                      </span>
                    </label>
                  ))}
                  {suggestions.length === 0 && <div className="text-secondary small p-2">No active agents available.</div>}
                </div>
              </div>
              <div className="modal-footer">
                <button className="btn btn-light border" onClick={() => setAssignModal(false)}>Cancel</button>
                <button className="btn btn-primary" onClick={confirmAssign} disabled={!selectedAgent || busy}>
                  Assign
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Rating modal */}
      {ratingModal && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h6 className="modal-title">Rate the support</h6>
                <button type="button" className="btn-close" onClick={() => setRatingModal(false)} />
              </div>
              <div className="modal-body">
                <div className="mb-3">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <i
                      key={value}
                      className={`bi ${value <= stars ? 'bi-star-fill its-star filled' : 'bi-star its-star'}`}
                      onClick={() => setStars(value)}
                    />
                  ))}
                </div>
                <textarea
                  className="form-control"
                  rows={3}
                  placeholder="Optional feedback, e.g. 'Very quick resolution.'"
                  value={ratingFeedback}
                  onChange={(event) => setRatingFeedback(event.target.value)}
                />
              </div>
              <div className="modal-footer">
                <button className="btn btn-light border" onClick={() => setRatingModal(false)}>Cancel</button>
                <button className="btn btn-primary" onClick={submitRating} disabled={busy}>Submit rating</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
