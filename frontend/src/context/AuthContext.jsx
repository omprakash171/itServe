import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import * as authService from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('itserve_token'))
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('itserve_user')
    try {
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })

  const persist = useCallback((nextToken, nextUser) => {
    localStorage.setItem('itserve_token', nextToken)
    localStorage.setItem('itserve_user', JSON.stringify(nextUser))
    setToken(nextToken)
    setUser(nextUser)
  }, [])

  const login = useCallback(
    async (email, password) => {
      const data = await authService.login(email, password)
      persist(data.token, data.user)
      return data.user
    },
    [persist]
  )

  const register = useCallback(
    async (payload) => {
      const data = await authService.register(payload)
      persist(data.token, data.user)
      return data.user
    },
    [persist]
  )

  const logout = useCallback(() => {
    localStorage.removeItem('itserve_token')
    localStorage.removeItem('itserve_user')
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ token, user, isAuthenticated: Boolean(token && user), login, register, logout, setUser }),
    [token, user, login, register, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
