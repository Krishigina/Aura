import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Plus, Edit2, Trash2, Clock, DollarSign, X, Settings } from 'lucide-react'
import { proceduresApi, dictionariesApi } from '../api'
import Select from '../components/Select'
import DictionaryPanel from '../components/DictionaryPanel'
import './Procedures.css'

const defaultProcedureCategories = ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход']

const procedureDictConfig = {
  procedureCategories: { label: 'Категории процедур', icon: Settings, color: '#8B5CF6' }
}

export default function Procedures() {
  const { user, hasPermission } = useAuth()
  const [procedures, setProcedures] = useState([])
  const [categories, setCategories] = useState(defaultProcedureCategories)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Все')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [editingProcedure, setEditingProcedure] = useState(null)
  const [showDictPanel, setShowDictPanel] = useState(false)
  const [fullPageDict, setFullPageDict] = useState(null)
  const [fullPageFilter, setFullPageFilter] = useState('')
  const [form, setForm] = useState({ 
    name: '', 
    duration: '', 
    price: '', 
    category: '', 
    description: '',
    contraindications: ''
  })

  const canEdit = hasPermission('procedures')
  const canManageEnums = user?.role === 'admin'

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [proceduresData, procedureCategoriesData] = await Promise.all([
        proceduresApi.getAll().catch(() => []),
        dictionariesApi.get('procedureCategories').catch(() => defaultProcedureCategories),
      ])
      setProcedures(proceduresData)
      setCategories(procedureCategoriesData)
    } catch (err) {
      setProcedures([])
      setCategories(defaultProcedureCategories)
    } finally {
      setLoading(false)
    }
  }

  const filtered = procedures.filter(p => {
    const matchesSearch = p.name?.toLowerCase().includes(search.toLowerCase())
    const matchesCategory = category === 'Все' || p.category === category
    return matchesSearch && matchesCategory
  })

  const openAddModal = () => {
    setEditingProcedure(null)
    setForm({ 
      name: '', 
      duration: '', 
      price: '', 
      category: categories[0] || '', 
      description: '',
      contraindications: ''
    })
    setModalOpen(true)
  }

  const openEditModal = (procedure) => {
    setEditingProcedure(procedure)
    setForm({
      name: procedure.name || '',
      duration: procedure.duration?.toString() || '',
      price: procedure.price?.toString() || '',
      category: procedure.category || '',
      description: procedure.description || '',
      contraindications: procedure.contraindications || ''
    })
    setModalOpen(true)
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleSelectChange = (name, value) => {
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleSave = async () => {
    if (!form.name || !form.duration || !form.price) return

    const procedureData = {
      name: form.name,
      duration: parseInt(form.duration),
      price: parseInt(form.price),
      category: form.category,
      description: form.description || '',
      contraindications: form.contraindications || ''
    }

    try {
      if (editingProcedure) {
        const updated = await proceduresApi.update(editingProcedure.id, procedureData)
        setProcedures(prev => prev.map(p => p.id === editingProcedure.id ? updated : p))
      } else {
        const created = await proceduresApi.create(procedureData)
        setProcedures(prev => [created, ...prev])
      }
      setModalOpen(false)
    } catch (err) {
      console.error('Error saving procedure:', err)
    }
  }

  const handleDelete = async () => {
    if (deleteModal) {
      try {
        await proceduresApi.delete(deleteModal.id)
        setProcedures(prev => prev.filter(p => p.id !== deleteModal.id))
      } catch (err) {
        console.error('Error deleting procedure:', err)
      }
      setDeleteModal(null)
    }
  }

  const handleDictAdd = async (key, value) => {
    try {
      await dictionariesApi.create(key, value)
      setCategories(prev => [...prev, value])
    } catch (err) {
      console.error('Error adding dictionary value:', err)
    }
  }

  const handleDictDelete = async (key, value) => {
    try {
      await dictionariesApi.delete(key, value)
      setCategories(prev => prev.filter(v => v !== value))
    } catch (err) {
      console.error('Error deleting dictionary value:', err)
    }
  }

  const handleDictUpdate = async (key, oldValue, newValue) => {
    try {
      await dictionariesApi.update(key, oldValue, newValue)
      setCategories(prev => prev.map(v => v === oldValue ? newValue : v))
    } catch (err) {
      console.error('Error updating dictionary value:', err)
    }
  }

  const openFullPage = (key) => setFullPageDict(key)
  const closeFullPage = () => { setFullPageDict(null); setFullPageFilter('') }

  if (loading) {
    return (
      <div className="procedures-page">
        <div className="loading-state">Загрузка...</div>
      </div>
    )
  }

  return (
    <div className="procedures-page">
      <div className="page-header">
        <div>
          <h2>Процедуры</h2>
          <p>Управление салонными процедурами</p>
        </div>
        <div className="header-actions">
          {canManageEnums && (
            <button className={`btn ${showDictPanel ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setShowDictPanel(!showDictPanel)}>
              <Settings size={16} />{showDictPanel ? 'Скрыть' : 'Справочники'}
            </button>
          )}
          {canEdit && (
            <button className="btn btn-primary" onClick={openAddModal}><Plus size={18} />Добавить процедуру</button>
          )}
        </div>
      </div>

      {showDictPanel && canManageEnums && (
        <DictionaryPanel
          dictionaries={{ procedureCategories: categories }}
          config={procedureDictConfig}
          onAdd={handleDictAdd}
          onDelete={handleDictDelete}
          onUpdate={handleDictUpdate}
          canEdit={canManageEnums}
          onFullPage={openFullPage}
          fullPageDict={fullPageDict}
          onCloseFullPage={closeFullPage}
          fullPageFilter={fullPageFilter}
          onFullPageFilterChange={setFullPageFilter}
        />
      )}

      <div className="filters-bar glass-card">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input type="text" placeholder="Поиск..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
        <div className="category-tabs">
          <button key="all" className={`category-tab ${category === 'Все' ? 'active' : ''}`} onClick={() => setCategory('Все')}>Все</button>
          {categories.map((cat, idx) => (
            <button key={cat || idx} className={`category-tab ${category === cat ? 'active' : ''}`} onClick={() => setCategory(cat)}>{cat}</button>
          ))}
        </div>
      </div>

      <div className="procedures-grid">
        {filtered.map(procedure => (
          <div key={procedure.id} className="procedure-card glass-card clickable" onClick={() => openEditModal(procedure)}>
            <div className="procedure-header">
              <h4>{procedure.name}</h4>
            </div>
            <div className="procedure-meta">
              <div className="meta-item"><Clock size={16} /><span>{procedure.duration} мин</span></div>
              <div className="meta-item"><DollarSign size={16} /><span>{parseInt(procedure.price).toLocaleString()} ₽</span></div>
            </div>
            <div className="procedure-category">{procedure.category}</div>
            {canEdit && (
              <div className="procedure-actions">
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); openEditModal(procedure) }}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setDeleteModal(procedure) }}><Trash2 size={16} /></button>
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
                <label>Название *</label>
                <input className="input" name="name" value={form.name} onChange={handleInputChange} placeholder="Название процедуры" required />
              </div>
              <Select label="Категория" name="category" value={form.category} onChange={handleSelectChange} options={categories} placeholder="Выберите категорию" />
              <div className="form-group">
                <label>Длительность (мин) *</label>
                <input className="input" name="duration" type="number" value={form.duration} onChange={handleInputChange} required />
              </div>
              <div className="form-group">
                <label>Цена (₽) *</label>
                <input className="input" name="price" type="number" value={form.price} onChange={handleInputChange} required />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Описание</label>
                <textarea className="input textarea" name="description" value={form.description} onChange={handleInputChange} rows="3" />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Противопоказания</label>
                <textarea className="input textarea" name="contraindications" value={form.contraindications} onChange={handleInputChange} rows="2" />
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
