import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, Clock, DollarSign, Image as ImageIcon, Scissors } from 'lucide-react'
import { proceduresApi, dictionariesApi } from '../api'
import ProcedureWizard from '../components/ProcedureWizard'
import './Procedures.css'

const defaultProcedureCategories = ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход']

export default function Procedures() {
  const { hasPermission } = useAuth()
  const { success, error } = useToast()
  const [procedures, setProcedures] = useState([])
  const [categories, setCategories] = useState(defaultProcedureCategories)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Все')
  const [deleteModal, setDeleteModal] = useState(null)
  const [dictionaries, setDictionaries] = useState({
    method_types: [],
    durations: [],
    equipment: [],
    zones: [],
    effects: [],
    problems: [],
    directions: []
  })
  const [showWizard, setShowWizard] = useState(false)
  const [editingProcedure, setEditingProcedure] = useState(null)

  const canEdit = hasPermission('procedures')

  useEffect(() => {
    loadData()
    loadDictionaries()
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

  const loadDictionaries = async () => {
    try {
      const [method_types, durations, equipment, zones, effects, problems, directions] = await Promise.all([
        proceduresApi.getMethodTypes().catch(() => []),
        proceduresApi.getDurations().catch(() => []),
        proceduresApi.getEquipment().catch(() => []),
        proceduresApi.getZones().catch(() => []),
        proceduresApi.getEffects().catch(() => []),
        proceduresApi.getProblems().catch(() => []),
        dictionariesApi.get('procedureCategories').catch(() => ['Аппаратная косметология', 'Инъекционная косметология', 'Эстетическая косметология']),
      ])
      setDictionaries({
        method_types: method_types.map(m => m.value),
        durations: durations.map(d => d.value),
        equipment: equipment.map(e => e.value),
        zones: zones.map(z => z.value),
        effects: effects.map(e => e.value),
        problems: problems.map(p => p.value),
        directions
      })
      setCategories(directions)
    } catch (err) {
      console.error('Error loading dictionaries:', err)
    }
  }

  const filtered = procedures.filter(p => {
    const matchesSearch = p.name?.toLowerCase().includes(search.toLowerCase())
    const matchesCategory = category === 'Все' || p.category === category
    return matchesSearch && matchesCategory
  })

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
        {canEdit && (
          <button className="btn btn-primary" onClick={() => { setEditingProcedure(null); setShowWizard(true) }}>
            <Plus size={18} />Добавить процедуру
          </button>
        )}
      </div>

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
          <div key={procedure.id} className="procedure-card glass-card clickable" onClick={() => { setEditingProcedure(procedure); setShowWizard(true) }}>
            <div className="procedure-header">
              <h4>{procedure.name}</h4>
              {procedure.direction && <span className="direction-badge">{procedure.direction}</span>}
            </div>
            <div className="procedure-meta">
              {procedure.duration && <div className="meta-item"><Clock size={16} /><span>{procedure.duration}</span></div>}
              {procedure.method_type && <div className="meta-item"><Scissors size={16} /><span>{procedure.method_type}</span></div>}
            </div>
            {procedure.photos && procedure.photos.length > 0 && (
              <div className="procedure-photos">
                {procedure.photos.slice(0, 3).map((photo, idx) => (
                  <div key={photo.id || idx} className="procedure-photo-thumb">
                    {photo.data ? (
                      <img src={`data:${photo.content_type};base64,${photo.data}`} alt="" />
                    ) : (
                      <ImageIcon size={16} />
                    )}
                  </div>
                ))}
                {procedure.photos.length > 3 && <span className="more-photos">+{procedure.photos.length - 3}</span>}
              </div>
            )}
            <div className="procedure-fields">
              {procedure.description && <p className="field-line">{procedure.description.substring(0, 100)}{procedure.description.length > 100 ? '...' : ''}</p>}
              {procedure.indications && <p className="field-line"><strong>Показания:</strong> {procedure.indications.substring(0, 80)}{procedure.indications.length > 80 ? '...' : ''}</p>}
            </div>
            {canEdit && (
              <div className="procedure-actions">
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setEditingProcedure(procedure); setShowWizard(true) }}><Edit2 size={16} /></button>
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

      {showWizard && (
        <ProcedureWizard
          initialData={editingProcedure}
          dictionaries={dictionaries}
          onSave={async (data) => {
            try {
              if (editingProcedure) {
                await proceduresApi.update(editingProcedure.id, data)
              } else {
                await proceduresApi.create(data)
              }
              loadData()
              setShowWizard(false)
              success('Процедура сохранена')
            } catch (err) {
              console.error('Error saving procedure:', err)
              error('Ошибка сохранения')
            }
          }}
          onCancel={() => setShowWizard(false)}
        />
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
