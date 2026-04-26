import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Eye, Edit2, Trash2, Mail, Calendar, X, Plus } from 'lucide-react'
import { usersApi } from '../api'
import Select from '../components/Select'
import './Users.css'

const sampleUsers = [
  { value: 'user', label: 'Пользователь' },
  { value: 'cosmetologist', label: 'Косметолог' },
  { value: 'manager', label: 'Менеджер' },
  { value: 'admin', label: 'Администратор' }
]

function generateNickname(name) {
  return '@' + name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z_]/g, '')
}

function normalizeLogin(value) {
  const trimmed = (value || '').replace(/\s+/g, '').toLowerCase()
  if (!trimmed) return ''
  const noAt = trimmed.replace(/@/g, '')
  return '@' + noAt.replace(/[^a-z0-9_]/g, '')
}

function formatPhone(value) {
  let digits = (value || '').replace(/\D/g, '')
  if (!digits) return { masked: '', digits: '' }
  if (digits.startsWith('8')) digits = '7' + digits.slice(1)
  if (!digits.startsWith('7')) digits = '7' + digits
  digits = digits.slice(0, 11)

  const code = digits.slice(1, 4)
  const p1 = digits.slice(4, 7)
  const p2 = digits.slice(7, 9)
  const p3 = digits.slice(9, 11)

  let masked = '+7'
  if (code) masked += ` (${code}`
  if (code.length === 3) masked += ')'
  if (p1) masked += ` ${p1}`
  if (p2) masked += `-${p2}`
  if (p3) masked += `-${p3}`

  return { masked, digits }
}

export default function Users() {
  const { hasPermission } = useAuth()
  const { success, error } = useToast()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [viewModal, setViewModal] = useState(null)
  const [editingUser, setEditingUser] = useState(null)
  const [form, setForm] = useState({ 
    name: '', 
    email: '', 
    role: 'user',
    nickname: ''
  })
  const [formErrors, setFormErrors] = useState({})

  const canEdit = hasPermission('users')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const usersData = await usersApi.getAll().catch(() => [])
      setUsers(usersData)
    } catch (err) {
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  const filtered = users.filter(u => 
    u.name?.toLowerCase().includes(search.toLowerCase()) || 
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    (u.nickname && u.nickname.toLowerCase().includes(search.toLowerCase()))
  )

  const openAddModal = () => {
    setEditingUser(null)
    setForm({ name: '', email: '', role: 'user', nickname: '' })
    setFormErrors({})
    setModalOpen(true)
  }

  const openEditModal = (user) => {
    setEditingUser(user)
    setForm({
      name: user.name || '',
      email: user.email || '',
      role: user.role || 'user',
      nickname: user.nickname || ''
    })
    setFormErrors({})
    setModalOpen(true)
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target

    if (name === 'nickname') {
      const login = normalizeLogin(value)
      setForm(prev => ({ ...prev, nickname: login }))
      setFormErrors(prev => ({ ...prev, nickname: '' }))
      return
    }

    setForm(prev => ({ ...prev, [name]: value }))
    if (name === 'name') {
      setForm(prev => {
        const currentLogin = prev.nickname || ''
        const nextLogin = (!currentLogin || currentLogin === '@') ? generateNickname(value) : currentLogin
        return { ...prev, nickname: nextLogin }
      })
      setFormErrors(prev => ({ ...prev, name: '' }))
    }
    if (name === 'email') {
      setFormErrors(prev => ({ ...prev, email: '' }))
    }
  }

  const handleSelectChange = (name, value) => {
    setForm(prev => ({ ...prev, [name]: value }))
    setFormErrors(prev => ({ ...prev, [name]: '' }))
  }

  const validateForm = () => {
    const errors = {}
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
    const loginRegex = /^@[a-z0-9_]{3,32}$/

    if (!form.name?.trim()) {
      errors.name = 'Введите имя'
    }

    if (!form.email?.trim() || !emailRegex.test(form.email.trim())) {
      errors.email = 'Введите корректный email с доменом (example@site.com)'
    }

    if (!form.nickname || !loginRegex.test(form.nickname)) {
      errors.nickname = 'Логин: @ + 3-32 символа, только a-z, 0-9, _'
    } else {
      const lower = form.nickname.toLowerCase()
      const duplicate = users.find(u => (u.nickname || '').toLowerCase() === lower && u.id !== editingUser?.id)
      if (duplicate) {
        errors.nickname = 'Этот логин уже занят'
      }
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSave = async () => {
    if (!validateForm()) return

    const nickname = normalizeLogin(form.nickname || generateNickname(form.name))
    const userData = {
      name: form.name.trim(),
      email: form.email.trim(),
      role: form.role,
      nickname,
      avatar: ''
    }

    try {
      if (editingUser) {
        const updated = await usersApi.update(editingUser.id, userData)
        setUsers(prev => prev.map(u => u.id === editingUser.id ? updated : u))
        success('Пользователь обновлен')
      } else {
        const created = await usersApi.create(userData)
        setUsers(prev => [created, ...prev])
        success('Пользователь создан')
      }
      setModalOpen(false)
    } catch (err) {
      console.error('Error saving user:', err)
      error('Ошибка сохранения: ' + err.message)
    }
  }

  const handleDelete = async () => {
    if (deleteModal) {
      try {
        await usersApi.delete(deleteModal.id)
        setUsers(prev => prev.filter(u => u.id !== deleteModal.id))
        success('Пользователь удален')
      } catch (err) {
        console.error('Error deleting user:', err)
        error('Ошибка удаления пользователя')
      }
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

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleDateString('ru-RU')
  }

  if (loading) {
    return (
      <div className="users-page">
        <div className="loading-state">Загрузка...</div>
      </div>
    )
  }

  return (
    <div className="users-page">
      <div className="page-header">
        <div>
          <h2>Пользователи</h2>
          <p>Управление пользователями системы</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={openAddModal}><Plus size={16} />Добавить пользователя</button>
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
            <div className="user-avatar">{user.name?.[0] || 'U'}</div>
            <div className="user-info">
              <h4>{user.name} <span className={`badge ${getRoleBadge(user.role).class}`} style={{marginLeft: '8px', fontSize: '10px'}}>{getRoleBadge(user.role).label}</span></h4>
              <div className="user-info-row"><Mail size={14} />{user.email}</div>
              {user.nickname && <div className="user-info-row"><span style={{width: '14px'}}/>{user.nickname}</div>}
              <div className="user-info-row join-date"><Calendar size={14} />С {formatDate(user.created_at)}</div>
            </div>
            {canEdit && (
              <div className="user-actions">
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setViewModal(user) }}><Eye size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); openEditModal(user) }}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setDeleteModal(user) }}><Trash2 size={16} /></button>
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
              <div className="profile-avatar">{viewModal.name?.[0] || 'U'}</div>
              <div className="profile-details">
                <div className="profile-row"><span>Имя:</span> {viewModal.name}</div>
                <div className="profile-row"><span>Никнейм:</span> {viewModal.nickname}</div>
                <div className="profile-row"><span>Роль:</span> {getRoleBadge(viewModal.role).label}</div>
                <div className="profile-row"><span>Email:</span> {viewModal.email}</div>
                <div className="profile-row"><span>Дата регистрации:</span> {formatDate(viewModal.created_at)}</div>
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
              <div className="form-group">
                <label>Имя *</label>
                <input className="input" name="name" value={form.name} onChange={handleInputChange} placeholder="Полное имя" required />
                {formErrors.name && <small style={{ color: '#ef4444' }}>{formErrors.name}</small>}
              </div>
              <div className="form-group">
                <label>Логин *</label>
                <input
                  className="input"
                  name="nickname"
                  value={form.nickname}
                  onFocus={() => {
                    if (!form.nickname) {
                      setForm(prev => ({ ...prev, nickname: '@' }))
                    }
                  }}
                  onChange={handleInputChange}
                  placeholder="@login"
                />
                {formErrors.nickname && <small style={{ color: '#ef4444' }}>{formErrors.nickname}</small>}
              </div>
              <Select label="Роль" name="role" value={form.role} onChange={handleSelectChange} options={sampleUsers} />
              <div className="form-group">
                <label>Email *</label>
                <input className="input" name="email" type="email" value={form.email} onChange={handleInputChange} required />
                {formErrors.email && <small style={{ color: '#ef4444' }}>{formErrors.email}</small>}
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
