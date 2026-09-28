import { useCallback, useEffect, useState } from 'react'
import { useToast } from '../components/ToastProvider'
import { useAuth } from '../context/AuthContext'
import { apiErrorMessage } from '../services/api'
import { createArticle, getArticle, listArticles, updateArticle, voteArticle } from '../services/dashboardService'
import { listCategories } from '../services/ticketService'
import { formatDate } from '../utils/format'

const EMPTY_FORM = { title: '', category: '', content: '' }

export default function KnowledgeBase() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'SUPPORT_AGENT'

  const [articles, setArticles] = useState([])
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState(null)
  const [modalMode, setModalMode] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setArticles(await listArticles(search.trim() || undefined))
    } catch (err) {
      toast.show(apiErrorMessage(err, 'Unable to load articles.'), 'danger')
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => setCategories([]))
  }, [])

  const openArticle = async (article) => {
    try {
      setSelected(await getArticle(article.id))
    } catch (err) {
      toast.show(apiErrorMessage(err, 'Unable to open article.'), 'danger')
    }
  }

  const handleVote = async (helpful) => {
    if (!selected) return
    try {
      const counts = await voteArticle(selected.id, helpful)
      setSelected({ ...selected, ...counts })
      setArticles((list) => list.map((a) => (a.id === selected.id ? { ...a, ...counts } : a)))
      toast.show(helpful ? 'Thanks for the feedback!' : 'Thanks — we will improve this article.')
    } catch (err) {
      toast.show(apiErrorMessage(err, 'Unable to record vote.'), 'danger')
    }
  }

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setFormErrors({})
    setModalMode('new')
  }

  const openEdit = () => {
    setForm({ title: selected.title, category: selected.category, content: selected.content })
    setFormErrors({})
    setModalMode('edit')
  }

  const setField = (name, value) => setForm((f) => ({ ...f, [name]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormErrors({})
    const payload = { title: form.title.trim(), category: form.category, content: form.content.trim() }
    try {
      const saved = modalMode === 'new' ? await createArticle(payload) : await updateArticle(selected.id, payload)
      toast.show(modalMode === 'new' ? 'Article published.' : 'Article updated.')
      setModalMode(null)
      setSelected(saved)
      load()
    } catch (err) {
      if (err?.response?.data?.fieldErrors) {
        setFormErrors(err.response.data.fieldErrors)
      }
      toast.show(apiErrorMessage(err, 'Unable to save article.'), 'danger')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <h1 className="h4 mb-1">Knowledge Base</h1>
          <p className="text-secondary mb-0 small">Self-service guides for common IT issues.</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary btn-sm" onClick={openCreate}>
            <i className="bi bi-journal-plus me-1" />New Article
          </button>
        )}
      </div>

      <div className="input-group input-group-sm mb-3" style={{ maxWidth: 420 }}>
        <span className="input-group-text bg-white">
          <i className="bi bi-search text-secondary" />
        </span>
        <input
          className="form-control"
          placeholder="Search articles (title or content)…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {search && (
          <button className="btn btn-outline-secondary" onClick={() => setSearch('')}>
            Clear
          </button>
        )}
      </div>

      {loading ? (
        <div className="text-secondary small py-4 text-center">Loading articles…</div>
      ) : articles.length === 0 ? (
        <div className="text-secondary small py-4 text-center">No articles found.</div>
      ) : (
        <div className="row g-3">
          {articles.map((article) => (
            <div className="col-md-6 col-xl-4" key={article.id}>
              <div
                className="card its-card h-100"
                role="button"
                onClick={() => openArticle(article)}
                style={{ cursor: 'pointer' }}
              >
                <div className="card-body d-flex flex-column">
                  <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                    <span className="badge its-badge bg-light text-dark border">{article.category}</span>
                    <span className="small text-secondary text-nowrap">
                      <i className="bi bi-hand-thumbs-up text-success me-1" />
                      {article.helpfulYes}
                      <i className="bi bi-hand-thumbs-down text-danger ms-2 me-1" />
                      {article.helpfulNo}
                    </span>
                  </div>
                  <h2 className="h6 mb-2">{article.title}</h2>
                  <p className="text-secondary small mb-3" style={{ display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {article.content}
                  </p>
                  <div className="mt-auto small text-secondary">
                    By {article.author?.fullName} · Updated {formatDate(article.updatedAt)}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {selected && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,.5)' }}>
          <div className="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div className="modal-content">
              <div className="modal-header">
                <div>
                  <span className="badge its-badge bg-light text-dark border mb-1">{selected.category}</span>
                  <h5 className="modal-title">{selected.title}</h5>
                </div>
                <button type="button" className="btn-close" onClick={() => setSelected(null)} />
              </div>
              <div className="modal-body">
                <div className="small text-secondary mb-3">
                  By {selected.author?.fullName} · Created {formatDate(selected.createdAt)} · Updated{' '}
                  {formatDate(selected.updatedAt)}
                </div>
                <div className="mb-4" style={{ whiteSpace: 'pre-wrap' }}>
                  {selected.content}
                </div>
                <div className="border-top pt-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
                  <div>
                    <span className="small text-secondary me-2">Was this article helpful?</span>
                    <button className="btn btn-sm btn-outline-success me-2" onClick={() => handleVote(true)}>
                      <i className="bi bi-hand-thumbs-up me-1" />Yes ({selected.helpfulYes})
                    </button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => handleVote(false)}>
                      <i className="bi bi-hand-thumbs-down me-1" />No ({selected.helpfulNo})
                    </button>
                  </div>
                  {canEdit && (
                    <button className="btn btn-sm btn-outline-secondary" onClick={openEdit}>
                      <i className="bi bi-pencil me-1" />Edit
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {modalMode && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,.5)', zIndex: 1080 }}
        >
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content">
              <form onSubmit={handleSubmit}>
                <div className="modal-header">
                  <h5 className="modal-title">{modalMode === 'new' ? 'New Article' : 'Edit Article'}</h5>
                  <button type="button" className="btn-close" onClick={() => setModalMode(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Title</label>
                    <input
                      className={`form-control form-control-sm ${formErrors.title ? 'is-invalid' : ''}`}
                      value={form.title}
                      onChange={(e) => setField('title', e.target.value)}
                      required
                    />
                    {formErrors.title && <div className="invalid-feedback d-block">{formErrors.title}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Category</label>
                    <select
                      className={`form-select form-select-sm ${formErrors.category ? 'is-invalid' : ''}`}
                      value={form.category}
                      onChange={(e) => setField('category', e.target.value)}
                      required
                    >
                      <option value="">Select a category…</option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.name}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                    {formErrors.category && <div className="invalid-feedback d-block">{formErrors.category}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Content</label>
                    <textarea
                      className={`form-control form-control-sm ${formErrors.content ? 'is-invalid' : ''}`}
                      rows={10}
                      value={form.content}
                      onChange={(e) => setField('content', e.target.value)}
                      required
                    />
                    <div className="form-text">Minimum 20 characters. Plain text; line breaks are preserved.</div>
                    {formErrors.content && <div className="invalid-feedback d-block">{formErrors.content}</div>}
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setModalMode(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-sm btn-primary" disabled={saving}>
                    {saving ? 'Saving…' : modalMode === 'new' ? 'Publish' : 'Save Changes'}
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
