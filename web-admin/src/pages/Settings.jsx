import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { User, Bell, Shield, Palette, Save } from 'lucide-react'
import './Settings.css'

export default function Settings() {
  const { user } = useAuth()
  const { isDarkMode, toggleDarkMode } = useTheme()
  const [activeTab, setActiveTab] = useState('profile')

  const tabs = [
    { id: 'profile', label: 'Профиль', icon: User },
    { id: 'notifications', label: 'Уведомления', icon: Bell },
    { id: 'appearance', label: 'Внешний вид', icon: Palette },
  ]

  return (
    <div className="settings-page">
      <div className="page-header">
        <div>
          <h2>Настройки</h2>
          <p>Управление настройками системы</p>
        </div>
      </div>

      <div className="settings-layout">
        <div className="settings-tabs glass-card">
          {tabs.map(tab => (
            <button 
              key={tab.id} 
              className={`settings-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <tab.icon size={18} />
              {tab.label}
            </button>
          ))}
        </div>

        <div className="settings-content glass-card">
          {activeTab === 'profile' && (
            <div className="settings-section">
              <h3>Профиль пользователя</h3>
              <div className="form-group">
                <label>Имя</label>
                <input type="text" className="input" defaultValue={user?.name} />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" className="input" defaultValue={user?.email} />
              </div>
              <div className="form-group">
                <label>Роль</label>
                <input type="text" className="input" value={user?.role === 'admin' ? 'Администратор' : user?.role === 'manager' ? 'Менеджер' : 'Косметолог'} disabled />
              </div>
              <button className="btn btn-primary"><Save size={16} />Сохранить</button>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div className="settings-section">
              <h3>Настройки уведомлений</h3>
              <div className="toggle-item">
                <div><h4>Email уведомления</h4><p>Получать уведомления на email</p></div>
                <input type="checkbox" defaultChecked className="toggle" />
              </div>
              <div className="toggle-item">
                <div><h4>Новые пользователи</h4><p>Уведомлять о новых регистрациях</p></div>
                <input type="checkbox" defaultChecked className="toggle" />
              </div>
              <div className="toggle-item">
                <div><h4>Отчеты</h4><p>Еженедельная рассылка отчетов</p></div>
                <input type="checkbox" className="toggle" />
              </div>
            </div>
          )}

          {activeTab === 'appearance' && (
            <div className="settings-section">
              <h3>Внешний вид</h3>
              <div className="toggle-item">
                <div><h4>Темная тема</h4><p>Использовать темную тему оформления</p></div>
                <input type="checkbox" checked={isDarkMode} onChange={toggleDarkMode} className="toggle" />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
