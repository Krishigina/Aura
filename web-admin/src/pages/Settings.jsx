import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { User, Bell, Shield, Palette, Save, BookA, Plus, Edit2, Trash2, X, Check } from 'lucide-react'
import './Settings.css'

const STORAGE_KEYS = {
  brands: 'aura_brands',
  categories: 'aura_categories',
  segments: 'aura_segments',
  volumes: 'aura_volumes',
  procedures: 'aura_procedure_categories'
}

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'],
  procedures: ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж']
}

function loadEnums() {
  try {
    const result = {}
    for (const [key, storageKey] of Object.entries(STORAGE_KEYS)) {
      const stored = localStorage.getItem(storageKey)
      result[key] = stored ? JSON.parse(stored) : defaultEnums[key]
    }
    return result
  } catch {
    return defaultEnums
  }
}

function saveEnums(enums) {
  for (const [key, storageKey] of Object.entries(STORAGE_KEYS)) {
    localStorage.setItem(storageKey, JSON.stringify(enums[key]))
  }
}

export default function Settings() {
  const { user, hasPermission } = useAuth()
  const { isDarkMode, toggleDarkMode } = useTheme()
  const [activeTab, setActiveTab] = useState('profile')
  const [enums, setEnums] = useState(loadEnums)
  const [editingEnum, setEditingEnum] = useState(null)
  const [newValue, setNewValue] = useState('')
  const [showAddModal, setShowAddModal] = useState(null)

  useEffect(() => {
    saveEnums(enums)
  }, [enums])

  const canEdit = user?.role === 'admin'

  const tabs = [
    { id: 'profile', label: 'Профиль', icon: User },
    { id: 'dictionaries', label: 'Справочники', icon: BookA },
    { id: 'notifications', label: 'Уведомления', icon: Bell },
    { id: 'appearance', label: 'Внешний вид', icon: Palette },
  ]

  const addEnumValue = (enumKey) => {
    if (!newValue.trim()) return
    setEnums(prev => ({
      ...prev,
      [enumKey]: [...prev[enumKey], newValue.trim()]
    }))
    setNewValue('')
    setShowAddModal(null)
  }

  const deleteEnumValue = (enumKey, value) => {
    setEnums(prev => ({
      ...prev,
      [enumKey]: prev[enumKey].filter(v => v !== value)
    }))
  }

  const DictionaryCard = ({ title, key, values }) => (
    <div className="dictionary-card">
      <div className="dictionary-header">
        <h4>{title}</h4>
        {canEdit && (
          <button className="btn btn-ghost btn-sm" onClick={() => setShowAddModal(key)}>
            <Plus size={16} />Добавить
          </button>
        )}
      </div>
      <div className="dictionary-tags">
        {values.map((value, idx) => (
          <span key={idx} className="dictionary-tag">
            {value}
            {canEdit && (
              <button className="tag-remove" onClick={() => deleteEnumValue(key, value)}>
                <X size={12} />
              </button>
            )}
          </span>
        ))}
      </div>
    </div>
  )

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

          {activeTab === 'dictionaries' && (
            <div className="settings-section dictionaries-section">
              <h3>Справочники системы</h3>
              <p className="section-desc">Управление справочными данными для продуктов и процедур</p>
              
              <DictionaryCard title="Бренды" key="brands" values={enums.brands} />
              <DictionaryCard title="Категории" key="categories" values={enums.categories} />
              <DictionaryCard title="Сегменты" key="segments" values={enums.segments} />
              <DictionaryCard title="Объёмы" key="volumes" values={enums.volumes} />
              <DictionaryCard title="Категории процедур" key="procedures" values={enums.procedures} />
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

      {showAddModal && (
        <div className="modal-overlay" onClick={() => { setShowAddModal(null); setNewValue(''); }}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()} style={{maxWidth: '400px'}}>
            <div className="modal-header">
              <h3>Добавить значение</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => { setShowAddModal(null); setNewValue(''); }}><X size={20} /></button>
            </div>
            <div className="form-group">
              <label>Значение</label>
              <input 
                type="text" 
                className="input" 
                value={newValue}
                onChange={e => setNewValue(e.target.value)}
                placeholder="Введите значение"
                autoFocus
                onKeyDown={e => e.key === 'Enter' && addEnumValue(showAddModal)}
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => { setShowAddModal(null); setNewValue(''); }}>Отмена</button>
              <button className="btn btn-primary" onClick={() => addEnumValue(showAddModal)}><Check size={16} />Добавить</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
