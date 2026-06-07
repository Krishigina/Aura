import { useState } from 'react'
import { NavLink, useLocation, useNavigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { 
  LayoutDashboard, 
  Package, 
  Sparkles, 
  BookOpen, 
  Users, 
  BarChart3, 
  Settings, 
  LogOut,
  Menu,
  X,
  ChevronLeft,
  BookA
} from 'lucide-react'
import './Layout.css'

const menuItems = [
  { path: '/', icon: LayoutDashboard, label: 'Дашборд', permission: 'dashboard' },
  { path: '/products', icon: Package, label: 'Продукты', permission: 'products' },
  { path: '/procedures', icon: Sparkles, label: 'Процедуры', permission: 'procedures' },
  { path: '/content', icon: BookOpen, label: 'Контент', permission: 'content' },
  { path: '/users', icon: Users, label: 'Пользователи', permission: 'users' },
  { path: '/reports', icon: BarChart3, label: 'Отчеты', permission: 'reports' },
  { path: '/dictionaries', icon: BookA, label: 'Справочники', permission: 'dictionaries' },
  { path: '/knowledge-sources', icon: BookOpen, label: 'Источники знаний', permission: 'content' },
  { path: '/matching-rules', icon: BookOpen, label: 'Правила подбора', permission: 'content' },
  { path: '/ingredient-knowledge', icon: BookA, label: 'База ингредиентов', permission: 'content' },
  { path: '/settings', icon: Settings, label: 'Настройки', permission: 'settings' },
]

export default function Layout({ children }) {
  const { user, logout, hasPermission } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(true)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const filteredMenuItems = menuItems.filter(item => hasPermission(item.permission))

  return (
    <div className="layout">
      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <div className="logo">
            <img className="logo-icon" src="/logo.svg" alt="Aura" />
            {sidebarOpen && <span className="logo-text">Aura Admin</span>}
          </div>
          <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>
            <ChevronLeft className={sidebarOpen ? '' : 'rotated'} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {filteredMenuItems.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <item.icon className="nav-icon" />
              {sidebarOpen && <span className="nav-label">{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          {sidebarOpen && (
            <div className="user-info">
              <div className="user-avatar">{user?.name?.[0] || 'U'}</div>
              <div className="user-details">
                <div className="user-name">{user?.name}</div>
                <div className="user-role">{user?.role === 'admin' ? 'Администратор' : user?.role === 'manager' ? 'Менеджер' : 'Косметолог'}</div>
              </div>
            </div>
          )}
          <button className="logout-btn" onClick={handleLogout} title="Выйти">
            <LogOut size={20} />
            {sidebarOpen && <span>Выйти</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="topbar">
          <button className="menu-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>
            <Menu />
          </button>
          <div className="topbar-title">
            {menuItems.find(item => item.path === location.pathname)?.label || 'Дашборд'}
          </div>
          <div className="topbar-actions">
            <span className="user-email">{user?.email}</span>
          </div>
        </header>

        <div className="content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
