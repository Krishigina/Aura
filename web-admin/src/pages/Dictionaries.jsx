import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { FlaskConical, Sparkles, BookOpen, UserCog, ChevronDown, Plus, Edit2, Trash2, Search, X } from 'lucide-react'
import { dictionariesApi } from '../api'
import DictionaryPanel from '../components/DictionaryPanel'
import './Dictionaries.css'

function pluralize(n, one, two, five) {
  const absN = Math.abs(n)
  if (absN % 100 >= 11 && absN % 100 <= 19) return five
  const mod10 = absN % 10
  if (mod10 === 1) return one
  if (mod10 >= 2 && mod10 <= 4) return two
  return five
}

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'],
  procedureCategories: ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход'],
  contentCategories: ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'],
  userRoles: ['Пользователь', 'Косметолог', 'Менеджер', 'Администратор'],
  skinTypes: ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная'],
}

const dictGroups = [
  {
    id: 'products',
    label: 'Продукты',
    icon: FlaskConical,
    color: '#8B5CF6',
    dictionaries: ['brands', 'categories', 'segments', 'volumes']
  },
  {
    id: 'procedures',
    label: 'Процедуры',
    icon: Sparkles,
    color: '#EC4899',
    dictionaries: ['procedureCategories']
  },
  {
    id: 'content',
    label: 'Контент',
    icon: BookOpen,
    color: '#10B981',
    dictionaries: ['contentCategories']
  },
  {
    id: 'users',
    label: 'Пользователи',
    icon: UserCog,
    color: '#3B82F6',
    dictionaries: ['userRoles', 'skinTypes']
  },
]

const dictConfig = {
  brands: { label: 'Бренды', icon: FlaskConical, color: '#9333EA' },
  categories: { label: 'Категории', icon: FlaskConical, color: '#059669' },
  segments: { label: 'Сегменты', icon: FlaskConical, color: '#D97706' },
  volumes: { label: 'Объёмы', icon: FlaskConical, color: '#2563EB' },
  procedureCategories: { label: 'Категории процедур', icon: Sparkles, color: '#EC4899' },
  contentCategories: { label: 'Категории контента', icon: BookOpen, color: '#10B981' },
  userRoles: { label: 'Роли', icon: UserCog, color: '#3B82F6' },
  skinTypes: { label: 'Типы кожи', icon: UserCog, color: '#EF4444' },
}

export default function Dictionaries() {
  const { user } = useAuth()
  const { success, error } = useToast()
  const [dictionaries, setDictionaries] = useState(defaultEnums)
  const [loading, setLoading] = useState(true)
  const [expandedGroup, setExpandedGroup] = useState('products')
  const [fullPageDict, setFullPageDict] = useState(null)
  const [fullPageFilter, setFullPageFilter] = useState('')

  const canManageEnums = user?.role === 'admin'

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [brands, categories, segments, volumes, procedureCategories, contentCategories, userRoles, skinTypes] = await Promise.all([
        dictionariesApi.get('brands').catch(() => defaultEnums.brands),
        dictionariesApi.get('categories').catch(() => defaultEnums.categories),
        dictionariesApi.get('segments').catch(() => defaultEnums.segments),
        dictionariesApi.get('volumes').catch(() => defaultEnums.volumes),
        dictionariesApi.get('procedureCategories').catch(() => defaultEnums.procedureCategories),
        dictionariesApi.get('contentCategories').catch(() => defaultEnums.contentCategories),
        dictionariesApi.get('userRoles').catch(() => defaultEnums.userRoles),
        dictionariesApi.get('skinTypes').catch(() => defaultEnums.skinTypes),
      ])
      setDictionaries({
        brands,
        categories,
        segments,
        volumes,
        procedureCategories,
        contentCategories,
        userRoles,
        skinTypes
      })
    } catch (err) {
      console.error('Error loading dictionaries:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleDictAdd = async (key, value) => {
    try {
      await dictionariesApi.create(key, value)
      setDictionaries(prev => ({ ...prev, [key]: [...prev[key], value] }))
      success(`Значение "${value}" добавлено`)
    } catch (err) {
      console.error('Error adding dictionary value:', err)
      error('Не удалось добавить значение')
    }
  }

  const handleDictDelete = async (key, value) => {
    try {
      await dictionariesApi.delete(key, value)
      setDictionaries(prev => ({ ...prev, [key]: prev[key].filter(v => v !== value) }))
      success(`Значение "${value}" удалено`)
    } catch (err) {
      console.error('Error deleting dictionary value:', err)
      error('Не удалось удалить значение')
    }
  }

  const handleDictUpdate = async (key, oldValue, newValue) => {
    try {
      await dictionariesApi.update(key, oldValue, newValue)
      setDictionaries(prev => ({ ...prev, [key]: prev[key].map(v => v === oldValue ? newValue : v) }))
      success(`Значение изменено с "${oldValue}" на "${newValue}"`)
    } catch (err) {
      console.error('Error updating dictionary value:', err)
      error('Не удалось обновить значение')
    }
  }

const [fullPageEditValue, setFullPageEditValue] = useState(null)
  const [fullPageDeleteValue, setFullPageDeleteValue] = useState(null)
  const [fullPageFormValue, setFullPageFormValue] = useState('')
  const [showFullPageModal, setShowFullPageModal] = useState(false)

  const openFullPage = (key) => setFullPageDict(key)
  const closeFullPage = () => { setFullPageDict(null); setFullPageFilter('') }

  const toggleGroup = (groupId) => {
    setExpandedGroup(expandedGroup === groupId ? null : groupId)
  }

  const currentDictValues = fullPageDict ? dictionaries[fullPageDict] || [] : []
  const filteredValues = currentDictValues.filter(v => !fullPageFilter || v.toLowerCase().includes(fullPageFilter.toLowerCase()))
  const currentDictConfig = dictConfig[fullPageDict] || {}
  const DictIcon = currentDictConfig.icon

  const openAddModal = () => {
    setFullPageEditValue(null)
    setFullPageFormValue('')
    setShowFullPageModal(true)
  }

  const openEditModal = (value) => {
    setFullPageEditValue(value)
    setFullPageFormValue(value)
    setShowFullPageModal(true)
  }

  const handleFullPageSave = () => {
    if (!fullPageFormValue.trim()) return
    if (fullPageEditValue) {
      handleDictUpdate(fullPageDict, fullPageEditValue, fullPageFormValue.trim())
    } else {
      handleDictAdd(fullPageDict, fullPageFormValue.trim())
    }
    setShowFullPageModal(false)
  }

  const confirmFullPageDelete = () => {
    if (fullPageDeleteValue) {
      handleDictDelete(fullPageDict, fullPageDeleteValue)
      setFullPageDeleteValue(null)
    }
  }

  if (loading) {
    return (
      <div className="dictionaries-page">
        <div className="loading-state">Загрузка...</div>
      </div>
    )
  }

  if (fullPageDict) {
    return (
      <div className="dict-fullpage">
        <div className="page-header">
          <div>
            <button className="btn btn-ghost" onClick={closeFullPage} style={{ marginBottom: '4px', paddingLeft: 0 }}>
              <ChevronDown size={16} style={{ transform: 'rotate(90deg)' }} />Назад к справочникам
            </button>
            <h2 style={{ fontSize: '20px' }}>{currentDictConfig.label || fullPageDict}</h2>
            <p style={{ fontSize: '13px' }}>{currentDictValues.length} {pluralize(currentDictValues.length, 'значение', 'значения', 'значений')}</p>
          </div>
          {canManageEnums && (
            <button className="btn btn-primary" onClick={openAddModal}>
              <Plus size={16} />Добавить
            </button>
          )}
        </div>

        <div className="filters-bar glass-card">
          <div className="search-wrapper">
            <Search size={16} className="search-icon" />
            <input type="text" placeholder="Поиск..." value={fullPageFilter} onChange={e => setFullPageFilter(e.target.value)} className="search-input" />
          </div>
        </div>

        <div className="dict-fullpage-grid">
          {filteredValues.map((value, idx) => (
            <div key={idx} className="dict-fullpage-item glass-card">
              <span className="dict-fullpage-item-value">{value}</span>
              {canManageEnums && (
                <div className="dict-fullpage-item-actions">
                  <button className="btn btn-ghost btn-sm" onClick={() => openEditModal(value)}><Edit2 size={14} /></button>
                  <button className="btn btn-ghost btn-sm" onClick={() => setFullPageDeleteValue(value)}><Trash2 size={14} /></button>
                </div>
              )}
            </div>
          ))}
        </div>

        {filteredValues.length === 0 && <div className="empty-state glass-card"><p>Ничего не найдено</p></div>}

        {showFullPageModal && (
          <div className="modal-overlay" onClick={() => setShowFullPageModal(false)}>
            <div className="modal glass-card" onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{fullPageEditValue ? 'Редактировать' : 'Добавить'}</h3>
                <button className="btn btn-ghost btn-sm" onClick={() => setShowFullPageModal(false)}><X size={20} /></button>
              </div>
              <div className="form-group">
                <label>Значение</label>
                <input className="input" value={fullPageFormValue} onChange={e => setFullPageFormValue(e.target.value)} onKeyDown={e => e.key === 'Enter' && handleFullPageSave()} autoFocus />
              </div>
              <div className="modal-actions">
                <button className="btn btn-ghost" onClick={() => setShowFullPageModal(false)}>Отмена</button>
                <button className="btn btn-primary" onClick={handleFullPageSave}>Сохранить</button>
              </div>
            </div>
          </div>
        )}

        {fullPageDeleteValue && (
          <div className="modal-overlay" onClick={() => setFullPageDeleteValue(null)}>
            <div className="modal glass-card" onClick={e => e.stopPropagation()}>
              <div className="confirm-icon"><Trash2 size={32} /></div>
              <h3>Подтверждение</h3>
              <p>Вы уверены, что хотите удалить "{fullPageDeleteValue}"?</p>
              <div className="modal-actions">
                <button className="btn btn-ghost" onClick={() => setFullPageDeleteValue(null)}>Отмена</button>
                <button className="btn btn-danger" onClick={confirmFullPageDelete}>Удалить</button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="dictionaries-page">
      <div className="page-header">
        <div>
          <h2>Справочники</h2>
          <p>Управление справочными данными системы</p>
        </div>
      </div>

      <div className="dict-groups">
        {dictGroups.map(group => {
          const Icon = group.icon
          const isExpanded = expandedGroup === group.id
          return (
            <div key={group.id} className="dict-group">
              <button 
                className="dict-group-header" 
                onClick={() => toggleGroup(group.id)}
                style={{ borderColor: isExpanded ? group.color : 'transparent' }}
              >
                <div className="dict-group-left">
                  <div className="dict-group-icon" style={{ background: `${group.color}20`, color: group.color }}>
                    <Icon size={20} />
                  </div>
                  <span className="dict-group-label">{group.label}</span>
                  <span className="dict-group-count">{group.dictionaries.length} {pluralize(group.dictionaries.length, 'справочник', 'справочника', 'справочников')}</span>
                </div>
                <ChevronDown size={18} className={`dict-group-chevron ${isExpanded ? 'rotated' : ''}`} />
              </button>
              {isExpanded && (
                <div className="dict-group-content">
                  <DictionaryPanel
                    dictionaries={group.dictionaries.reduce((acc, key) => {
                      acc[key] = dictionaries[key] || []
                      return acc
                    }, {})}
                    config={dictConfig}
                    onAdd={handleDictAdd}
                    onDelete={handleDictDelete}
                    onUpdate={handleDictUpdate}
                    canEdit={canManageEnums}
                    onFullPage={openFullPage}
                  />
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
