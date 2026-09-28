import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as dashboardService from '../services/dashboardService'
import { formatDateTime } from '../utils/format'

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState([])
  const [unread, setUnread] = useState(0)
  const containerRef = useRef(null)
  const navigate = useNavigate()

  const refresh = async () => {
    try {
      const [count, page] = await Promise.all([
        dashboardService.unreadCount(),
        dashboardService.listNotifications(0, 15),
      ])
      setUnread(count)
      setNotifications(page.content)
    } catch {
      // bell is non-critical; ignore transient failures
    }
  }

  useEffect(() => {
    refresh()
    const interval = setInterval(refresh, 30000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    function onClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const openTicket = async (notification) => {
    if (!notification.read) {
      try {
        await dashboardService.markNotificationRead(notification.id)
        setNotifications((current) =>
          current.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        )
        setUnread((c) => Math.max(0, c - 1))
      } catch {
        // ignore
      }
    }
    setOpen(false)
    if (notification.ticketId) {
      navigate(`/tickets/${notification.ticketId}`)
    }
  }

  const markAllRead = async () => {
    await dashboardService.markAllNotificationsRead()
    setUnread(0)
    setNotifications((current) => current.map((n) => ({ ...n, read: true })))
  }

  return (
    <div className="position-relative" ref={containerRef}>
      <button className="btn btn-link text-dark position-relative p-1" onClick={() => setOpen((v) => !v)} aria-label="Notifications">
        <i className="bi bi-bell fs-5" />
        {unread > 0 && (
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div className="its-card position-absolute end-0 mt-2 shadow" style={{ width: 380, zIndex: 1080 }}>
          <div className="d-flex justify-content-between align-items-center border-bottom px-3 py-2">
            <strong className="small">Notifications</strong>
            {unread > 0 && (
              <button className="btn btn-link btn-sm p-0 text-decoration-none" onClick={markAllRead}>
                Mark all read
              </button>
            )}
          </div>
          <div style={{ maxHeight: 360, overflowY: 'auto' }}>
            {notifications.length === 0 && (
              <div className="text-secondary small p-3">No notifications yet.</div>
            )}
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`its-notification-item border-bottom px-3 py-2 ${notification.read ? '' : 'unread'}`}
                style={{ cursor: notification.ticketId ? 'pointer' : 'default' }}
                onClick={() => openTicket(notification)}
              >
                <div className="small">{notification.message}</div>
                <div className="text-secondary" style={{ fontSize: '0.72rem' }}>
                  {formatDateTime(notification.createdAt)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
