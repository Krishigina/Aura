import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Plus, Edit2, Trash2, Clock, DollarSign, X } from 'lucide-react'
import './Procedures.css'

const STORAGE_KEY = 'aura_procedures'

const defaultProcedures = [
  { id: 1, name: 'Ультразвуковая чистка', duration: 60, price: 3500, category: 'Чистка', status: 'active' },
  { id: 2, name: 'HydraFacial', duration: 90, price: 8000, category: 'Увлажнение', status: 'active' },
  { id: 3, name: 'Мезотерапия', duration: 45, price: 5000, category: 'Инъекции', status: 'active' },
  { id: 4, name: 'Лазерная эпиляция', duration: 30, price: 2500, category: 'Эпиляция', status: 'active' },
]

const categories = ['Все', 'Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж']

function loadProcedures() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : defaultProcedures
  } catch {
    return defaultProcedures
  }
}

function saveProcedures(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export default function Procedures() {
  const { hasPermission } = useAuth()
  const [procedures, setProcedures] = useState(loadProcedures)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Все')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [editingProcedure, setEditingProcedure] = useState(null)
  const [form, setForm] = useState({ name: '', duration: '', price: '', category: 'Чистка', status: 'active' })

  const canEdit = hasPermission('procedures')

  useEffect(() => {
    saveProcedures(procedures)
  }, [procedures])

  const filtered = procedures.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(search.toLowerCase())
    const matchesCategory = category === 'Все' || p.category === category
    return matchesSearch && matchesCategory
  })

  const openAddModal = () => {
    setEditingProcedure(null)
    setForm({ name: '', duration: '', price: '', category: 'Чистка', status: 'active' })
    setModalOpen(true)
  }

  const openEditModal = (procedure) => {
    setEditingProcedure(procedure)
    setForm({
      name: procedure.name,
      duration: procedure.duration.toString(),
      price: procedure.price.toString(),
      category: procedure.category,
      status: procedure.status
    })
    setModalOpen(true)
  }

  const handleSave = () => {
    if (!form.name || !form.duration || !form.price) return

    if (editingProcedure) {
      setProcedures(prev => prev.map(p => 
        p.id === editingProcedure.id 
          ? { ...p, name: form.name, duration: parseInt(form.duration), price: parseInt(form.price), category: form.category, status: form.status }
          : p
      ))
    } else {
      const newProcedure = {
        id: Date.now(),
        name: form.name,
        duration: parseInt(form.duration),
        price: parseInt(form.price),
        category: form.category,
        status: 'active'
      }
      setProcedures(prev => [newProcedure, ...prev])
    }
    setModalOpen(false)
  }

  const handleDelete = () => {
    if (deleteModal) {
      setProcedures(prev => prev.filter(p => p.id !== deleteModal.id))
      setDeleteModal(null)
    }
  }

  return (
    <div className="procedures-page">
      <div className="page-header">
        <div>
          <h2>Процедуры</h2>
          <p>Управление салонными процедурами</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={openAddModal}><Plus size={18} />Добавить процедуру</button>
        )}
      </div>

      <div className="filters-bar glass-card">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input type="text" placeholder="Поиск..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
        <div className="category-tabs">
          {categories.map(cat => (
            <button key={cat} className={`category-tab ${category === cat ? 'active' : ''}`} onClick={() => setCategory(cat)}>{cat}</button>
          ))}
        </div>
      </div>

      <div className="procedures-grid">
        {filtered.map(procedure => (
          <div key={procedure.id} className="procedure-card glass-card clickable" onClick={() => openEditModal(procedure)}>
            <div className="procedure-header">
              <h4>{procedure.name}</h4>
              <span className={`badge ${procedure.status === 'active' ? 'badge-success' : 'badge-error'}`}>
                {procedure.status === 'active' ? 'Активна' : 'Неактивна'}
              </span>
            </div>
            <div className="procedure-meta">
              <div className="meta-item"><Clock size={16} /><span>{procedure.duration} мин</span></div>
              <div className="meta-item"><DollarSign size={16} /><span>{procedure.price.toLocaleString()} ₽</span></div>
            </div>
            <div className="procedure-category">{procedure.category}</div>
            {canEdit && (
              <div className="procedure-actions">
                <button className="btn btn-ghost btn-sm" onClick={() => openEditModal(procedure)}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => setDeleteModal(procedure)}><Trash2 size={16} /></button>
              </div>
            )}
          </div>
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="empty-state glass-card">
          <p>Процедуры не найдены</p>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingProcedure ? 'Редактировать процедуру' : 'Новая процедура'}</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}><X size={20} /></button>
            </div>
            <div className="form-grid">
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Название</label>
                <input className="input" value={form.name} onChange={e => setForm({...form, name: e.target.value})} placeholder="Название процедуры" />
              </div>
              <div className="form-group">
                <label>Длительность (мин)</label>
                <input className="input" type="number" value={form.duration} onChange={e => setForm({...form, duration: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Цена (₽)</label>
                <input className="input" type="number" value={form.price} onChange={e => setForm({...form, price: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Категория</label>
                <select className="input" value={form.category} onChange={e => setForm({...form, category: e.target.value})}>
                  {categories.filter(c => c !== 'Все').map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Статус</label>
                <select className="input" value={form.status} onChange={e => setForm({...form, status: e.target.value})}>
                  <option value="active">Активна</option>
                  <option value="inactive">Неактивна</option>
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
            <h3>Удалить процедуру?</h3>
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
