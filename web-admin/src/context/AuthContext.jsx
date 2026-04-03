import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:3002'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [token, setToken] = useState(null)

  useEffect(() => {
    const validateToken = async () => {
      const savedToken = localStorage.getItem('aura_token')
      const savedUser = localStorage.getItem('aura_user')
      
      if (savedToken && savedUser) {
        try {
          // Validate token with backend
          const response = await fetch(`${API_BASE}/api/auth/me`, {
            headers: { Authorization: `Bearer ${savedToken}` }
          })
          
          if (response.ok) {
            const user = await response.json()
            setToken(savedToken)
            setUser(user)
          } else {
            // Token invalid - clear session
            localStorage.removeItem('aura_token')
            localStorage.removeItem('aura_user')
          }
        } catch {
          // Network error - keep stored session
          setToken(savedToken)
          setUser(JSON.parse(savedUser))
        }
      }
      setLoading(false)
    }
    
    validateToken()
  }, [])

  const login = async (email, password) => {
    try {
      const response = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })
      
      if (!response.ok) {
        const error = await response.json()
        return { success: false, error: error.detail || 'Ошибка входа' }
      }
      
      const data = await response.json()
      setToken(data.access_token)
      setUser(data.user)
      localStorage.setItem('aura_token', data.access_token)
      localStorage.setItem('aura_user', JSON.stringify(data.user))
      return { success: true }
    } catch (error) {
      return { success: false, error: 'Ошибка соединения с сервером' }
    }
  }

  const register = async (name, email, password, role = 'Пользователь', nickname = '') => {
    try {
      const response = await fetch(`${API_BASE}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name, email, password, role, nickname }),
      })
      
      if (!response.ok) {
        const error = await response.json()
        return { success: false, error: error.detail || 'Ошибка регистрации' }
      }
      
      const data = await response.json()
      setToken(data.access_token)
      setUser(data.user)
      localStorage.setItem('aura_token', data.access_token)
      localStorage.setItem('aura_user', JSON.stringify(data.user))
      return { success: true }
    } catch (error) {
      return { success: false, error: 'Ошибка соединения с сервером' }
    }
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('aura_token')
    localStorage.removeItem('aura_user')
  }

  const hasPermission = (permission) => {
    if (!user) return true // Allow when not logged in (for login page)
    
    const roleKey = user.role?.toLowerCase()
    const permissions = {
      'администратор': ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings', 'dictionaries', 'admin'],
      'менеджер': ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings'],
      'косметолог': ['dashboard', 'content', 'settings'],
      'пользователь': ['dashboard'],
      'admin': ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings', 'dictionaries', 'admin'],
      'manager': ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings'],
      'cosmetolog': ['dashboard', 'content', 'settings'],
      'user': ['dashboard'],
    }
    
    const userPermissions = permissions[roleKey] || []
    return userPermissions.some(p => p === permission || p.startsWith(permission.split('_')[0]))
  }

  // Get auth header for API calls
  const getAuthHeader = () => {
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, register, hasPermission, getAuthHeader, token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}