import { createContext, useCallback, useContext, useState } from 'react'

const ToastContext = createContext(null)

let nextId = 1

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((t) => t.id !== id))
  }, [])

  const show = useCallback(
    (message, variant = 'success') => {
      const id = nextId++
      setToasts((current) => [...current, { id, message, variant }])
      setTimeout(() => dismiss(id), 4200)
    },
    [dismiss]
  )

  return (
    <ToastContext.Provider value={{ show }}>
      {children}
      <div className="toast-container position-fixed top-0 end-0 p-3" style={{ zIndex: 1090 }}>
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast show align-items-center text-bg-${toast.variant} border-0`}>
            <div className="d-flex">
              <div className="toast-body">{toast.message}</div>
              <button type="button" className="btn-close btn-close-white me-2 m-auto" onClick={() => dismiss(toast.id)} />
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
