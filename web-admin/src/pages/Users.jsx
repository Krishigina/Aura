import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Eye, Edit2, Trash2, Mail, Phone, Calendar, X } from 'lucide-react'
import { usersApi } from '../api'
import Select from '../components/Select'
import './Users.css'

const defaultUsers = [
  { id: 1, name: 'Елена Иванова', email: 'elena@example.com', phone: '+7 999 123-45-67', skinType: 'Комбинированная', status: 'active', role: 'user', nickname: '@elena_ivanova' },
  { id: 2, name: 'Мария Петрова', email: 'maria@example.com', phone: '+7 999 234-56-78', skinType: 'Сухая', status: 'active', role: 'user', nickname: '@maria_petrova' },
  { id: 3, name: 'Анна Сидорова', email: 'anna@example.com', phone: '+7 999 345-67-89', skinType: 'Жирная', status: 'inactive', role: 'user', nickname: '@anna_sidorova' },
  { id: 4, name: 'Анна Петрова', email: 'anna.cosmetologist@aura.com', phone: '+7 999 456-78-90', skinType: 'Нормальная', status: 'active', role: 'cosmetologist', nickname: '@skin_expert' },
  { id: 5, name: 'Елена Волкова', email: 'elena.cosmetologist@aura.com', phone: '+7 999 567-89-01', skinType: 'Нормальная', status: 'active', role: 'cosmetologist', nickname: '@beauty_consultant' },
  { id: 6, name: 'Мария Соколова', email: 'maria.cosmetologist@aura.com', phone: '+7 999 678-90-12', skinType: 'Нормальная', status: 'active', role: 'cosmetologist', nickname: '@derma_pro' },
]

const skinTypes = ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная']
const userRoles = [
  { value: 'user', label: 'Пользователь' },
  { value: 'cosmetologist', label: 'Косметолог' },
  { value: 'manager', label: 'Менеджер' },
  { value: 'admin', label: 'Администратор' }
]

function generateNickname(name) {
  return '@' + name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z_]/g, '')
}

export default function Users() {
  const { hasPermission } = useAuth()
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
    phone: '', 
    skinType: '', 
    status: 'active',
    role: 'user',
    nickname: ''
  })

  const canEdit = hasPermission('users')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const usersData = await usersApi.getAll().catch(() => [])
      setUsers(usersData.length > 0 ? usersData : defaultUsers)
    } catch (err) {
      setUsers(defaultUsers)
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
    setForm({ name: '', email: '', phone: '', skinType: skinTypes[0], status: 'active', role: 'user', nickname: '' })
    setModalOpen(true)
  }

  const openEditModal = (user) => {
    setEditingUser(user)
    setForm({
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || '',
      skinType: user.skinType || '',
      status: user.status || 'active',
      role: user.role || 'user',
      nickname: user.nickname || ''
    })
    setModalOpen(true)
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    if (name === 'name') {
      setForm(prev => ({ ...prev, nickname: generateNickname(value) }))
    }
  }

  const handleSelectChange = (name, value) => {
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleSave = async () => {
    if (!form.name || !form.email) return

    const nickname = form.nickname || generateNickname(form.name)
    const userData = {
      ...form,
      nickname,
      avatar: ''
    }

    try {
      if (editingUser) {
        const updated = await usersApi.update(editingUser.id, userData)
        setUsers(prev => prev.map(u => u.id === editingUser.id ? updated : u))
      } else {
        const created = await usersApi.create(userData)
        setUsers(prev => [created, ...prev])
      }
      setModalOpen(false)
    } catch (err) {
      console.error('Error saving user:', err)
    }
  }

  const handleDelete = async () => {
    if (deleteModal) {
      try {
        await usersApi.delete(deleteModal.id)
        setUsers(prev => prev.filter(u => u.id !== deleteModal.id))
      } catch (err) {
        console.error('Error deleting user:', err)
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
            <div className="user-avatar">{user.name?.[0] || 'U'}</div>
            <div className="user-info">
              <h4>{user.name} <span className={`badge ${getRoleBadge(user.role).class}`} style={{marginLeft: '8px', fontSize: '10px'}}>{getRoleBadge(user.role).label}</span></h4>
              <div className="user-info-row"><Mail size={14} />{user.email}</div>
              <div className="user-info-row"><Phone size={14} />{user.phone}</div>
              {user.nickname && <div className="user-info-row"><span style={{width: '14px'}}/>{user.nickname}</div>}
              <div className="user-meta">
                <span className="skin-type">{user.skinType}</span>
                <span className={`status ${user.status}`}>{user.status === 'active' ? 'Активен' : 'Неактивен'}</span>
              </div>
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
                <div className="profile-row"><span>Телефон:</span> {viewModal.phone}</div>
                <div className="profile-row"><span>Тип кожи:</span> {viewModal.skinType}</div>
                <div className="profile-row"><span>Статус:</span> {viewModal.status === 'active' ? 'Активен' : 'Неактивен'}</div>
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
              </div>
              <div className="form-group">
                <label>Никнейм</label>
                <input className="input" name="nickname" value={form.nickname} onChange={handleInputChange} placeholder="@username" />
              </div>
              <Select label="Роль" name="role" value={form.role} onChange={handleSelectChange} options={userRoles.map(r => r.label)} />
              <div className="form-group">
                <label>Email *</label>
                <input className="input" name="email" type="email" value={form.email} onChange={handleInputChange} required />
              </div>
              <div className="form-group">
                <label>Телефон</label>
                <input className="input" name="phone" value={form.phone} onChange={handleInputChange} placeholder="+7 999 123-45-67" />
              </div>
              <Select label="Тип кожи" name="skinType" value={form.skinType} onChange={handleSelectChange} options={skinTypes} placeholder="Выберите тип кожи" />
              <Select label="Статус" name="status" value={form.status} onChange={handleSelectChange} options={['Активен', 'Неактивен']} />
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
