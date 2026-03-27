import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

// Mock users for demo
const MOCK_USERS = [
  { id: 1, email: 'admin@aura.ru', password: 'admin123', name: 'Администратор', role: 'admin' },
  { id: 2, email: 'manager@aura.ru', password: 'manager123', name: 'Менеджер', role: 'manager' },
  { id: 3, email: 'cosmetolog@aura.ru', password: 'cosmo123', name: 'Косметолог', role: 'cosmetolog' },
]

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Check for saved session
    const savedUser = localStorage.getItem('aura_user')
    if (savedUser) {
      setUser(JSON.parse(savedUser))
    }
    setLoading(false)
  }, [])

  const login = async (email, password) => {
    // Mock authentication
    const foundUser = MOCK_USERS.find(u => u.email === email && u.password === password)
    
    if (foundUser) {
      const { password: _, ...userWithoutPassword } = foundUser
      setUser(userWithoutPassword)
      localStorage.setItem('aura_user', JSON.stringify(userWithoutPassword))
      return { success: true }
    }
    
    return { success: false, error: 'Неверный email или пароль' }
  }

  const logout = () => {
    setUser(null)
    localStorage.removeItem('aura_user')
  }

  const hasPermission = (permission) => {
    if (!user) return true // Allow when not logged in (for login page)
    
    const permissions = {
      admin: ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings', 'dictionaries', 'admin'],
      manager: ['dashboard', 'products', 'procedures', 'content', 'users', 'reports', 'settings'],
      cosmetolog: ['dashboard', 'content', 'settings'],
    }
    
    const userPermissions = permissions[user.role] || []
    return userPermissions.some(p => p === permission || p.startsWith(permission.split('_')[0]))
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasPermission }}>
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
