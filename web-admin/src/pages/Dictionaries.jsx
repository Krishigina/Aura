import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { BookA, Package, Scissors, FileText, Users, Settings as SettingsIcon } from 'lucide-react'
import { dictionariesApi } from '../api'
import DictionaryPanel from '../components/DictionaryPanel'
import './Dictionaries.css'

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'],
  procedureCategories: ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход'],
  contentCategories: ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'],
}

const dictGroups = [
  {
    id: 'products',
    label: 'Продукты',
    icon: Package,
    color: '#8B5CF6',
    dictionaries: ['brands', 'categories', 'segments', 'volumes']
  },
  {
    id: 'procedures',
    label: 'Процедуры',
    icon: Scissors,
    color: '#EC4899',
    dictionaries: ['procedureCategories']
  },
  {
    id: 'content',
    label: 'Контент',
    icon: FileText,
    color: '#10B981',
    dictionaries: ['contentCategories']
  },
]

const dictConfig = {
  brands: { label: 'Бренды', icon: Package, color: '#9333EA' },
  categories: { label: 'Категории', icon: Package, color: '#059669' },
  segments: { label: 'Сегменты', icon: Package, color: '#D97706' },
  volumes: { label: 'Объёмы', icon: Package, color: '#2563EB' },
  procedureCategories: { label: 'Категории процедур', icon: Scissors, color: '#EC4899' },
  contentCategories: { label: 'Категории контента', icon: FileText, color: '#10B981' },
}

export default function Dictionaries() {
  const { user } = useAuth()
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
      const [brands, categories, segments, volumes, procedureCategories, contentCategories] = await Promise.all([
        dictionariesApi.get('brands').catch(() => defaultEnums.brands),
        dictionariesApi.get('categories').catch(() => defaultEnums.categories),
        dictionariesApi.get('segments').catch(() => defaultEnums.segments),
        dictionariesApi.get('volumes').catch(() => defaultEnums.volumes),
        dictionariesApi.get('procedureCategories').catch(() => defaultEnums.procedureCategories),
        dictionariesApi.get('contentCategories').catch(() => defaultEnums.contentCategories),
      ])
      setDictionaries({
        brands,
        categories,
        segments,
        volumes,
        procedureCategories,
        contentCategories
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
    } catch (err) {
      console.error('Error adding dictionary value:', err)
    }
  }

  const handleDictDelete = async (key, value) => {
    try {
      await dictionariesApi.delete(key, value)
      setDictionaries(prev => ({ ...prev, [key]: prev[key].filter(v => v !== value) }))
    } catch (err) {
      console.error('Error deleting dictionary value:', err)
    }
  }

  const handleDictUpdate = async (key, oldValue, newValue) => {
    try {
      await dictionariesApi.update(key, oldValue, newValue)
      setDictionaries(prev => ({ ...prev, [key]: prev[key].map(v => v === oldValue ? newValue : v) }))
    } catch (err) {
      console.error('Error updating dictionary value:', err)
    }
  }

  const openFullPage = (key) => setFullPageDict(key)
  const closeFullPage = () => { setFullPageDict(null); setFullPageFilter('') }

  const toggleGroup = (groupId) => {
    setExpandedGroup(expandedGroup === groupId ? null : groupId)
  }

  if (loading) {
    return (
      <div className="dictionaries-page">
        <div className="loading-state">Загрузка...</div>
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
                  <span className="dict-group-count">{group.dictionaries.length} справочников</span>
                </div>
                <SettingsIcon size={18} className={`dict-group-chevron ${isExpanded ? 'rotated' : ''}`} />
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
                    fullPageDict={fullPageDict}
                    onCloseFullPage={closeFullPage}
                    fullPageFilter={fullPageFilter}
                    onFullPageFilterChange={setFullPageFilter}
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
