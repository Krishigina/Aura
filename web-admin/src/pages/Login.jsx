import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Eye, EyeOff, Lock, Mail } from 'lucide-react'
import './Login.css'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    const result = await login(email, password)
    
    if (result.success) {
      navigate('/')
    } else {
      setError(result.error)
    }
    
    setLoading(false)
  }

  return (
    <div className="login-page">
      <div className="login-background">
        <div className="bg-gradient"></div>
        <div className="bg-shapes">
          <div className="shape shape-1"></div>
          <div className="shape shape-2"></div>
          <div className="shape shape-3"></div>
        </div>
      </div>

      <div className="login-container">
        <div className="login-card glass-card">
          <div className="login-header">
            <div className="login-logo">
              <img className="logo-icon" src="/logo.svg" alt="Aura" />
            </div>
            <h1>Aura Admin</h1>
            <p>Панель управления системой</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            <div className="form-group">
              <label>Email</label>
              <div className="input-wrapper">
                <Mail className="input-icon" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Введите email"
                  className="input"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label>Пароль</label>
              <div className="input-wrapper">
                <Lock className="input-icon" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Введите пароль"
                  className="input"
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button type="submit" className="btn btn-primary login-btn" disabled={loading}>
              {loading ? 'Вход...' : 'Войти'}
            </button>
          </form>

        </div>
      </div>
    </div>
  )
}
