import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Eye, Edit2, Trash2, Mail, Phone, Calendar, X } from 'lucide-react'
import './Users.css'

const STORAGE_KEY = 'aura_users'

const defaultUsers = [
  { id: 1, name: 'Елена Иванова', email: 'elena@example.com', phone: '+7 999 123-45-67', skinType: 'Комбинированная', date: '15.01.2026', status: 'active', role: 'user', nickname: '@elena_ivanova' },
  { id: 2, name: 'Мария Петрова', email: 'maria@example.com', phone: '+7 999 234-56-78', skinType: 'Сухая', date: '10.02.2026', status: 'active', role: 'user', nickname: '@maria_petrova' },
  { id: 3, name: 'Анна Сидорова', email: 'anna@example.com', phone: '+7 999 345-67-89', skinType: 'Жирная', date: '05.03.2026', status: 'inactive', role: 'user', nickname: '@anna_sidorova' },
  { id: 4, name: 'Анна Петрова', email: 'anna.cosmetologist@aura.com', phone: '+7 999 456-78-90', skinType: 'Нормальная', date: '01.01.2026', status: 'active', role: 'cosmetologist', nickname: '@skin_expert' },
  { id: 5, name: 'Елена Волкова', email: 'elena.cosmetologist@aura.com', phone: '+7 999 567-89-01', skinType: 'Нормальная', date: '01.01.2026', status: 'active', role: 'cosmetologist', nickname: '@beauty_consultant' },
  { id: 6, name: 'Мария Соколова', email: 'maria.cosmetologist@aura.com', phone: '+7 999 678-90-12', skinType: 'Нормальная', date: '01.01.2026', status: 'active', role: 'cosmetologist', nickname: '@derma_pro' },
]

const skinTypes = ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная']
const userRoles = ['user', 'cosmetologist', 'manager', 'admin']

function loadUsers() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : defaultUsers
  } catch {
    return defaultUsers
  }
}

function saveUsers(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

function generateNickname(name) {
  return '@' + name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z_]/g, '')
}

export default function Users() {
  const { hasPermission } = useAuth()
  const [users, setUsers] = useState(loadUsers)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [viewModal, setViewModal] = useState(null)
  const [editingUser, setEditingUser] = useState(null)
  const [form, setForm] = useState({ 
    name: '', 
    email: '', 
    phone: '', 
    skinType: 'Нормальная', 
    status: 'active',
    role: 'user',
    nickname: ''
  })

  const canEdit = hasPermission('users')

  useEffect(() => {
    saveUsers(users)
  }, [users])

  const filtered = users.filter(u => 
    u.name.toLowerCase().includes(search.toLowerCase()) || 
    u.email.toLowerCase().includes(search.toLowerCase()) ||
    (u.nickname && u.nickname.toLowerCase().includes(search.toLowerCase()))
  )

  const openAddModal = () => {
    setEditingUser(null)
    setForm({ name: '', email: '', phone: '', skinType: 'Нормальная', status: 'active', role: 'user', nickname: '' })
    setModalOpen(true)
  }

  const openEditModal = (user) => {
    setEditingUser(user)
    setForm({
      name: user.name,
      email: user.email,
      phone: user.phone,
      skinType: user.skinType,
      status: user.status,
      role: user.role || 'user',
      nickname: user.nickname || generateNickname(user.name)
    })
    setModalOpen(true)
  }

  const handleNameChange = (name) => {
    const nickname = generateNickname(name)
    setForm({...form, name, nickname})
  }

  const handleSave = () => {
    if (!form.name || !form.email) return

    const nickname = form.nickname || generateNickname(form.name)

    if (editingUser) {
      setUsers(prev => prev.map(u => 
        u.id === editingUser.id 
          ? { ...u, ...form, nickname }
          : u
      ))
    } else {
      const newUser = {
        id: Date.now(),
        ...form,
        nickname,
        date: new Date().toLocaleDateString('ru-RU')
      }
      setUsers(prev => [newUser, ...prev])
    }
    setModalOpen(false)
  }

  const handleDelete = () => {
    if (deleteModal) {
      setUsers(prev => prev.filter(u => u.id !== deleteModal.id))
      setDeleteModal(null)
    }
  }

  const getRoleBadge = (role) => {
    const badges = {
      admin: { label: 'Админ', class: 'badge-error' },
      manager: { label: 'Менеджер', class: 'badge-warning' },
      cosmetologist: { label: 'Косметолог', class: 'badge-info' },
      user: { label: 'Пользователь', class: 'badge-success' }
    }
    return badges[role] || badges.user
  }

  return (
    <div className="users-page">
      <div className="page-header">
        <div>
          <h2>Пользователи</h2>
          <p>Управление пользователями системы</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={openAddModal}>Добавить пользователя</button>
        )}
      </div>

      <div className="filters-bar glass-card">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input type="text" placeholder="Поиск пользователей..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
      </div>

      <div className="users-grid">
        {filtered.map(user => (
          <div key={user.id} className="user-card glass-card clickable" onClick={() => openEditModal(user)}>
            <div className="user-avatar">{user.name[0]}</div>
            <div className="user-info">
              <h4>{user.name} <span className={`badge ${getRoleBadge(user.role).class}`} style={{marginLeft: '8px', fontSize: '10px'}}>{getRoleBadge(user.role).label}</span></h4>
              <div className="user-info-row"><Mail size={14} />{user.email}</div>
              <div className="user-info-row"><Phone size={14} />{user.phone}</div>
              {user.nickname && <div className="user-info-row"><span style={{width: '14px'}}/>{user.nickname}</div>}
              <div className="user-meta">
                <span className="skin-type">{user.skinType}</span>
                <span className={`status ${user.status}`}>{user.status === 'active' ? 'Активен' : 'Неактивен'}</span>
              </div>
              <div className="user-info-row join-date"><Calendar size={14} />С {user.date}</div>
            </div>
            {canEdit && (
              <div className="user-actions">
                <button className="btn btn-ghost btn-sm" onClick={() => setViewModal(user)}><Eye size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => openEditModal(user)}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => setDeleteModal(user)}><Trash2 size={16} /></button>
              </div>
            )}
          </div>
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="empty-state glass-card">
          <p>Пользователи не найдены</p>
        </div>
      )}

      {viewModal && (
        <div className="modal-overlay" onClick={() => setViewModal(null)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()} style={{maxWidth: '480px'}}>
            <div className="modal-header">
              <h3>Профиль пользователя</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setViewModal(null)}><X size={20} /></button>
            </div>
            <div className="user-profile">
              <div className="profile-avatar">{viewModal.name[0]}</div>
              <div className="profile-details">
                <div className="profile-row"><span>Имя:</span> {viewModal.name}</div>
                <div className="profile-row"><span>Никнейм:</span> {viewModal.nickname}</div>
                <div className="profile-row"><span>Роль:</span> {getRoleBadge(viewModal.role).label}</div>
                <div className="profile-row"><span>Email:</span> {viewModal.email}</div>
                <div className="profile-row"><span>Телефон:</span> {viewModal.phone}</div>
                <div className="profile-row"><span>Тип кожи:</span> {viewModal.skinType}</div>
                <div className="profile-row"><span>Статус:</span> {viewModal.status === 'active' ? 'Активен' : 'Неактивен'}</div>
                <div className="profile-row"><span>Дата регистрации:</span> {viewModal.date}</div>
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setViewModal(null)}>Закрыть</button>
            </div>
          </div>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingUser ? 'Редактировать пользователя' : 'Новый пользователь'}</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}><X size={20} /></button>
            </div>
            <div className="form-grid">
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Имя</label>
                <input className="input" value={form.name} onChange={e => handleNameChange(e.target.value)} placeholder="Полное имя" />
              </div>
              <div className="form-group">
                <label>Никнейм</label>
                <input className="input" value={form.nickname} onChange={e => setForm({...form, nickname: e.target.value})} placeholder="@username" />
              </div>
              <div className="form-group">
                <label>Роль</label>
                <select className="input" value={form.role} onChange={e => setForm({...form, role: e.target.value})}>
                  <option value="user">Пользователь</option>
                  <option value="cosmetologist">Косметолог</option>
                  <option value="manager">Менеджер</option>
                  <option value="admin">Администратор</option>
                </select>
              </div>
              <div className="form-group">
                <label>Email</label>
                <input className="input" type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Телефон</label>
                <input className="input" value={form.phone} onChange={e => setForm({...form, phone: e.target.value})} placeholder="+7 999 123-45-67" />
              </div>
              <div className="form-group">
                <label>Тип кожи</label>
                <select className="input" value={form.skinType} onChange={e => setForm({...form, skinType: e.target.value})}>
                  {skinTypes.map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Статус</label>
                <select className="input" value={form.status} onChange={e => setForm({...form, status: e.target.value})}>
                  <option value="active">Активен</option>
                  <option value="inactive">Неактивен</option>
                </select>
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setModalOpen(false)}>Отмена</button>
              <button className="btn btn-primary" onClick={handleSave}>Сохранить</button>
            </div>
          </div>
        </div>
      )}

      {deleteModal && (
        <div className="modal-overlay" onClick={() => setDeleteModal(null)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()} style={{maxWidth: '400px'}}>
            <h3>Удалить пользователя?</h3>
            <p style={{marginTop: '8px', color: 'var(--color-gray-500)'}}>
              Вы уверены, что хотите удалить "{deleteModal.name}"?
            </p>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setDeleteModal(null)}>Отмена</button>
              <button className="btn btn-danger" onClick={handleDelete}>Удалить</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
