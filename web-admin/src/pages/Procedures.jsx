import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, ChevronLeft } from 'lucide-react'
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
    const matchesCategory = category === 'Все' || p.direction === category
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

  const parseArrayField = (field) => {
    if (!field) return []
    if (Array.isArray(field)) return field
    try {
      return JSON.parse(field)
    } catch {
      return []
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
      {showWizard ? (
        <>
          <div className="page-header">
            <button className="btn btn-ghost" onClick={() => { setShowWizard(false); setEditingProcedure(null) }}>
              <ChevronLeft size={18} />Назад к списку
            </button>
          </div>
          <ProcedureWizard
            initialData={editingProcedure}
            dictionaries={dictionaries}
            onSave={async (data) => {
              try {
                if (editingProcedure) {
                  await proceduresApi.update(editingProcedure.id, data)
                } else {
                  const created = await proceduresApi.create(data)
                  // Upload locally-stored photos after creation
                  if (created?.id && data.photos?.length > 0) {
                    for (const photo of data.photos) {
                      if (photo.data && photo.content_type) {
                        const byteCharacters = atob(photo.data)
                        const byteNumbers = new Array(byteCharacters.length)
                        for (let i = 0; i < byteCharacters.length; i++) {
                          byteNumbers[i] = byteCharacters.charCodeAt(i)
                        }
                        const byteArray = new Uint8Array(byteNumbers)
                        const blob = new Blob([byteArray], { type: photo.content_type })
                        const file = new File([blob], photo.filename || 'photo.jpg', { type: photo.content_type })
                        await proceduresApi.uploadPhoto(created.id, file)
                      }
                    }
                  }
                }
                loadData()
                setShowWizard(false)
                setEditingProcedure(null)
                success('Процедура сохранена')
              } catch (err) {
                console.error('Error saving procedure:', err)
                error('Ошибка сохранения')
              }
            }}
            onCancel={() => { setShowWizard(false); setEditingProcedure(null) }}
          />
        </>
      ) : (
        <>
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

          <div className="procedures-table-wrapper">
            <table className="procedures-table">
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Направление</th>
                  <th>Метод</th>
                  <th>Решаемые проблемы</th>
                  <th>Эффекты</th>
                  {canEdit && <th></th>}
                </tr>
              </thead>
              <tbody>
                {filtered.map(procedure => (
                  <tr key={procedure.id} className="clickable" onClick={() => { setEditingProcedure(procedure); setShowWizard(true) }}>
                    <td>
                      <div className="procedure-name-cell">
                        <strong>{procedure.name}</strong>
                      </div>
                    </td>
                    <td>{procedure.direction && <span className="direction-badge">{procedure.direction}</span>}</td>
                    <td>{procedure.method_type || '—'}</td>
                    <td>
                      <div className="tag-list">
                        {parseArrayField(procedure.problems).slice(0, 2).map(p => (
                          <span key={p} className="tag">{p}</span>
                        ))}
                        {parseArrayField(procedure.problems).length > 2 && <span className="tag-more">+{parseArrayField(procedure.problems).length - 2}</span>}
                      </div>
                    </td>
                    <td>
                      <div className="tag-list">
                        {parseArrayField(procedure.effects).slice(0, 2).map(e => (
                          <span key={e} className="tag">{e}</span>
                        ))}
                        {parseArrayField(procedure.effects).length > 2 && <span className="tag-more">+{parseArrayField(procedure.effects).length - 2}</span>}
                      </div>
                    </td>
                    {canEdit && (
                      <td className="actions-cell">
                        <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setEditingProcedure(procedure); setShowWizard(true) }} title="Редактировать">
                          <Edit2 size={16} />
                        </button>
                        <button className="btn btn-ghost btn-sm btn-danger" onClick={(e) => { e.stopPropagation(); setDeleteModal(procedure) }} title="Удалить">
                          <Trash2 size={16} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filtered.length === 0 && (
            <div className="empty-state glass-card">
              <p>Процедуры не найдены</p>
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
                  <button className="btn btn-primary btn-danger" onClick={handleDelete}>Удалить</button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
