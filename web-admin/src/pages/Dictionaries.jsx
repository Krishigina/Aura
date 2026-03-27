import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Plus, Edit2, Trash2, X, Settings, Tag } from 'lucide-react'
import './Dictionaries.css'

const STORAGE_KEY_BRANDS = 'aura_brands'
const STORAGE_KEY_CATEGORIES = 'aura_categories'
const STORAGE_KEY_SEGMENTS = 'aura_segments'

const defaultBrands = [
  { id: 1, name: 'Aura', active: true },
  { id: 2, name: 'La Roche-Posay', active: true },
  { id: 3, name: 'Vichy', active: true },
  { id: 4, name: 'Bioderma', active: true },
  { id: 5, name: 'CeraVe', active: true },
  { id: 6, name: 'The Ordinary', active: true },
  { id: 7, name: "Paula's Choice", active: true },
  { id: 8, name: 'Cosrx', active: true },
  { id: 9, name: 'Eucerin', active: true },
  { id: 10, name: 'Nivea', active: true },
]

const defaultCategories = [
  { id: 1, name: 'Очищение', active: true },
  { id: 2, name: 'Увлажнение', active: true },
  { id: 3, name: 'Сыворотки', active: true },
  { id: 4, name: 'SPF', active: true },
  { id: 5, name: 'Уход', active: true },
  { id: 6, name: 'Маска', active: true },
  { id: 7, name: 'Тоник', active: true },
  { id: 8, name: 'Крем', active: true },
  { id: 9, name: 'Масло', active: true },
]

const defaultSegments = [
  { id: 1, name: 'Бюджетная', active: true },
  { id: 2, name: 'Люкс', active: true },
  { id: 3, name: 'Профессиональная', active: true },
  { id: 4, name: 'Космецевтика', active: true },
]

function loadData(key, defaultData) {
  try {
    const stored = localStorage.getItem(key)
    return stored ? JSON.parse(stored) : defaultData
  } catch {
    return defaultData
  }
}

function saveData(key, data) {
  localStorage.setItem(key, JSON.stringify(data))
}

export default function Dictionaries() {
  const { hasPermission } = useAuth()
  const [brands, setBrands] = useState(() => loadData(STORAGE_KEY_BRANDS, defaultBrands))
  const [categories, setCategories] = useState(() => loadData(STORAGE_KEY_CATEGORIES, defaultCategories))
  const [segments, setSegments] = useState(() => loadData(STORAGE_KEY_SEGMENTS, defaultSegments))
  
  const [activeTab, setActiveTab] = useState('brands')
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [editingItem, setEditingItem] = useState(null)
  const [form, setForm] = useState({ name: '', active: true })

  const canEdit = hasPermission('dictionaries')

  useEffect(() => {
    saveData(STORAGE_KEY_BRANDS, brands)
    saveData(STORAGE_KEY_CATEGORIES, categories)
    saveData(STORAGE_KEY_SEGMENTS, segments)
  }, [brands, categories, segments])

  const getData = () => {
    switch (activeTab) {
      case 'brands': return brands
      case 'categories': return categories
      case 'segments': return segments
      default: return []
    }
  }

  const setData = (data) => {
    switch (activeTab) {
      case 'brands': setBrands(data); break
      case 'categories': setCategories(data); break
      case 'segments': setSegments(data); break
    }
  }

  const filteredItems = getData().filter(item =>
    item.name.toLowerCase().includes(search.toLowerCase())
  )

  const openAddModal = () => {
    setEditingItem(null)
    setForm({ name: '', active: true })
    setModalOpen(true)
  }

  const openEditModal = (item) => {
    setEditingItem(item)
    setForm({ name: item.name, active: item.active })
    setModalOpen(true)
  }

  const handleSave = () => {
    if (!form.name.trim()) return

    if (editingItem) {
      setData(getData().map(item => 
        item.id === editingItem.id ? { ...item, name: form.name, active: form.active } : item
      ))
    } else {
      const newItem = { id: Date.now(), name: form.name, active: form.active }
      setData([newItem, ...getData()])
    }
    setModalOpen(false)
  }

  const handleDelete = (item) => {
    setDeleteModal(item)
  }

  const confirmDelete = () => {
    if (deleteModal) {
      setData(getData().filter(item => item.id !== deleteModal.id))
      setDeleteModal(null)
    }
  }

  const toggleActive = (item) => {
    setData(getData().map(i => 
      i.id === item.id ? { ...i, active: !i.active } : i
    ))
  }

  const getTabName = () => {
    switch (activeTab) {
      case 'brands': return 'Бренды'
      case 'categories': return 'Категории'
      case 'segments': return 'Сегменты'
      default: return ''
    }
  }

  return (
    <div className="dictionaries-page">
      <div className="page-header">
        <div>
          <h2>Справочники</h2>
          <p>Управление справочными данными системы</p>
        </div>
      </div>

      <div className="dict-tabs glass-card">
        <button className={`dict-tab ${activeTab === 'brands' ? 'active' : ''}`} onClick={() => setActiveTab('brands')}>
          Бренды ({brands.length})
        </button>
        <button className={`dict-tab ${activeTab === 'categories' ? 'active' : ''}`} onClick={() => setActiveTab('categories')}>
          Категории ({categories.length})
        </button>
        <button className={`dict-tab ${activeTab === 'segments' ? 'active' : ''}`} onClick={() => setActiveTab('segments')}>
          Сегменты ({segments.length})
        </button>
      </div>

      <div className="filters-bar glass-card">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input type="text" placeholder={`Поиск ${getTabName().toLowerCase()}...`} value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={openAddModal}>
            <Plus size={18} />Добавить
          </button>
        )}
      </div>

      <div className="dict-grid">
        {filteredItems.map(item => (
          <div key={item.id} className={`dict-card glass-card ${!item.active ? 'inactive' : ''}`} onClick={() => canEdit && openEditModal(item)}>
            <div className="dict-info">
              <Tag size={18} className="dict-icon" />
              <span className="dict-name">{item.name}</span>
            </div>
            {canEdit && (
              <div className="dict-actions" onClick={e => e.stopPropagation()}>
                <button className={`btn btn-ghost btn-sm toggle-btn ${item.active ? 'active' : ''}`} onClick={() => toggleActive(item)}>
                  {item.active ? 'Активен' : 'Неактивен'}
                </button>
                <button className="btn btn-ghost btn-sm" onClick={() => handleDelete(item)}>
                  <Trash2 size={16} />
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      {filteredItems.length === 0 && (
        <div className="empty-state glass-card">
          <p>{getTabName()} не найдены</p>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingItem ? `Редактировать ${getTabName().slice(0, -1)}` : `Добавить ${getTabName().slice(0, -1)}`}</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}><X size={20} /></button>
            </div>
            <div className="form-grid">
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Название</label>
                <input className="input" value={form.name} onChange={e => setForm({...form, name: e.target.value})} placeholder={`Название ${getTabName().toLowerCase().slice(0, -1)}`} />
              </div>
              <div className="form-group toggle-group">
                <label className="toggle-label">
                  <input type="checkbox" checked={form.active} onChange={e => setForm({...form, active: e.target.checked})} />
                  <span>Активен</span>
                </label>
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
            <h3>Удалить {getTabName().slice(0, -1)}?</h3>
            <p style={{marginTop: '8px', color: 'var(--color-gray-500)'}}>
              Вы уверены, что хотите удалить "{deleteModal.name}"?
            </p>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setDeleteModal(null)}>Отмена</button>
              <button className="btn btn-danger" onClick={confirmDelete}>Удалить</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
