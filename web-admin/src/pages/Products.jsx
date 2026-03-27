import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, X, Image, FileText, Tag, DollarSign, Settings, Check, ChevronDown, ChevronRight, Package, AlertTriangle, ExternalLink } from 'lucide-react'
import './Products.css'

const STORAGE_KEY = 'aura_products'

const STORAGE_KEYS = {
  brands: 'aura_brands',
  categories: 'aura_categories',
  segments: 'aura_segments',
  volumes: 'aura_volumes'
}

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л']
}

const defaultProducts = [
  { id: 1, name: 'Hydrating Serum', brand: 'Aura', category: 'Сыворотки', description: 'Увлажняющая сыворотка с гиалуроновой кислотой', images: [], volume: '30мл', segment: 'Люкс' },
  { id: 2, name: 'Vitamin C Cream', brand: 'Aura', category: 'Увлажнение', description: 'Антиоксидантный крем с витамином C', images: [], volume: '50мл', segment: 'Бюджетная' },
  { id: 3, name: 'Barrier Repair', brand: 'Aura', category: 'Уход', description: 'Восстанавливающий крем для кожного барьера', images: [], volume: '50мл', segment: 'Профессиональная' },
  { id: 4, name: 'SPF 50+ Protection', brand: 'Aura', category: 'SPF', description: 'Солнцезащитный крем SPF 50+', images: [], volume: '50мл', segment: 'Люкс' },
  { id: 5, name: 'Cleansing Foam', brand: 'Aura', category: 'Очищение', description: 'Мягкая пенка для умывания', images: [], volume: '150мл', segment: 'Бюджетная' },
]

function isValidProduct(p) {
  return p && typeof p === 'object' && typeof p.name === 'string' && typeof p.brand === 'string'
}

function loadProducts() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) return defaultProducts
    const parsed = JSON.parse(stored)
    if (Array.isArray(parsed) && parsed.every(isValidProduct)) return parsed
    return defaultProducts
  } catch { return defaultProducts }
}

function loadEnums() {
  try {
    const result = {}
    for (const [key, storageKey] of Object.entries(STORAGE_KEYS)) {
      const stored = localStorage.getItem(storageKey)
      const parsed = stored ? JSON.parse(stored) : null
      result[key] = Array.isArray(parsed) && parsed.every(v => typeof v === 'string') ? parsed : [...defaultEnums[key]]
    }
    return result
  } catch { return { ...defaultEnums } }
}

function saveProducts(data) { localStorage.setItem(STORAGE_KEY, JSON.stringify(data)) }
function saveEnums(enums) {
  for (const [key, storageKey] of Object.entries(STORAGE_KEYS)) {
    localStorage.setItem(storageKey, JSON.stringify(enums[key]))
  }
}

const dictConfig = {
  brands: { label: 'Бренды', icon: Package, color: '#9333EA' },
  categories: { label: 'Категории', icon: Tag, color: '#059669' },
  segments: { label: 'Сегменты', icon: Check, color: '#D97706' },
  volumes: { label: 'Объёмы', icon: Image, color: '#2563EB' }
}

export default function Products() {
  const { hasPermission, user } = useAuth()
  const { success, error, info } = useToast()
  const [products, setProducts] = useState(loadProducts)
  const [enums, setEnums] = useState(loadEnums)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Все')
  const [showModal, setShowModal] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [deleteModal, setDeleteModal] = useState(null)
  const [showDictPanel, setShowDictPanel] = useState(false)
  const [expandedDict, setExpandedDict] = useState(null)
  const [newValue, setNewValue] = useState('')
  const [editValue, setEditValue] = useState('')
  const [editingValue, setEditingValue] = useState(null)
  const [fullPageDict, setFullPageDict] = useState(null)
  const [confirmAction, setConfirmAction] = useState(null)

  const canEdit = hasPermission('products')
  const canManageEnums = user?.role === 'admin'

  const filteredProducts = products.filter(product => {
    const matchesSearch = product.name.toLowerCase().includes(search.toLowerCase()) || 
                         product.brand.toLowerCase().includes(search.toLowerCase()) ||
                         product.description?.toLowerCase().includes(search.toLowerCase())
    const matchesCategory = category === 'Все' || product.category === category
    return matchesSearch && matchesCategory
  })

  const handleDelete = (product) => setDeleteModal(product)
  const confirmDelete = () => {
    if (deleteModal) {
      setProducts(prev => prev.filter(p => p.id !== deleteModal.id))
      setDeleteModal(null)
      success(`Продукт "${deleteModal.name}" удалён`)
    }
  }
  const handleEdit = (product) => { setEditingProduct(product); setShowModal(true) }
  const openAddModal = () => { setEditingProduct(null); setShowModal(true) }

  const handleSave = (e) => {
    e.preventDefault()
    const formData = new FormData(e.target)
    const product = {
      id: editingProduct?.id || Date.now(),
      name: formData.get('name'),
      brand: formData.get('brand'),
      category: formData.get('category'),
      description: formData.get('description') || '',
      images: formData.get('images')?.split('\n').filter(url => url.trim()) || [],
      volume: formData.get('volume'),
      segment: formData.get('segment')
    }
    if (editingProduct) {
      setProducts(prev => prev.map(p => p.id === editingProduct.id ? product : p))
      success('Продукт обновлён')
    } else {
      setProducts(prev => [product, ...prev])
      success('Продукт добавлен')
    }
    setShowModal(false)
    setEditingProduct(null)
  }

  const getSegmentClass = (segment) => {
    const classes = { 'Бюджетная': 'segment-budget', 'Люкс': 'segment-lux', 'Профессиональная': 'segment-pro', 'Космецевтика': 'segment-cosmeceutical' }
    return classes[segment] || 'segment-budget'
  }

  const toggleDict = (key) => setExpandedDict(expandedDict === key ? null : key)

  const checkDuplicates = (key, value) => {
    const values = enums[key] || []
    return values.filter(v => v.toLowerCase().includes(value.toLowerCase()))
  }

  const addValue = () => {
    if (!newValue.trim()) return
    const duplicates = checkDuplicates(expandedDict, newValue)
    if (duplicates.length > 0) {
      error(`Найдены похожие значения: ${duplicates.join(', ')}`)
      return
    }
    setEnums(prev => ({ ...prev, [expandedDict]: [...prev[expandedDict], newValue.trim()] }))
    info(`Добавлено: ${newValue}`)
    setNewValue('')
  }

  const deleteValue = (key, value) => {
    setConfirmAction({ type: 'deleteEnum', key, value, label: `удалить "${value}" из ${dictConfig[key].label.toLowerCase()}` })
  }

  const confirmActionHandler = () => {
    if (!confirmAction) return
    if (confirmAction.type === 'deleteEnum') {
      setEnums(prev => ({ ...prev, [confirmAction.key]: prev[confirmAction.key].filter(v => v !== confirmAction.value) }))
      info(`Удалено: ${confirmAction.value}`)
    }
    setConfirmAction(null)
  }

  const startEdit = (key, value) => {
    setEditingValue({ key, value })
    setEditValue(value)
  }

  const saveEdit = () => {
    if (!editValue.trim()) return
    const duplicates = checkDuplicates(editingValue.key, editValue).filter(v => v !== editingValue.value)
    if (duplicates.length > 0) {
      error(`Такое значение уже есть: ${duplicates.join(', ')}`)
      return
    }
    setEnums(prev => ({
      ...prev,
      [editingValue.key]: prev[editingValue.key].map(v => v === editingValue.value ? editValue.trim() : v)
    }))
    success(`Изменено: ${editingValue.value} → ${editValue}`)
    setEditingValue(null)
    setEditValue('')
  }

  const openFullPage = (key) => setFullPageDict(key)
  const closeFullPage = () => setFullPageDict(null)

  return (
    <div className="products-page">
      <div className="page-header">
        <div>
          <h2>Продукты</h2>
          <p>Управление косметическими средствами</p>
        </div>
        <div className="header-actions">
          {canManageEnums && (
            <button className={`btn ${showDictPanel ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setShowDictPanel(!showDictPanel)}>
              <Settings size={16} />{showDictPanel ? 'Скрыть' : 'Справочники'}
            </button>
          )}
          {canEdit && (
            <button className="btn btn-primary" onClick={openAddModal}>
              <Plus size={18} />Добавить
            </button>
          )}
        </div>
      </div>

      {showDictPanel && canManageEnums && (
        <div className="dict-panel glass-card">
          <div className="dict-panel-header">
            <h3>Справочники</h3>
            <p>Управление значениями справочников для продуктов</p>
          </div>
          <div className="dict-accordion">
            {Object.entries(dictConfig).map(([key, config]) => {
              const Icon = config.icon
              const values = enums[key] || []
              const isExpanded = expandedDict === key
              return (
                <div key={key} className="dict-item">
                  <button className="dict-item-header" onClick={() => toggleDict(key)} style={{ borderColor: isExpanded ? config.color : 'transparent' }}>
                    <div className="dict-header-left">
                      <div className="dict-icon" style={{ background: `${config.color}20`, color: config.color }}><Icon size={18} /></div>
                      <span className="dict-label">{config.label}</span>
                      <span className="dict-count">{values.length}</span>
                    </div>
                    <div className="dict-header-right">
                      {values.length > 10 && <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); openFullPage(key) }}><ExternalLink size={14} />Открыть все</button>}
                      <ChevronDown size={18} className={`dict-chevron ${isExpanded ? 'rotated' : ''}`} />
                    </div>
                  </button>
                  {isExpanded && (
                    <div className="dict-item-content">
                      <div className="dict-values-list">
                        {values.map((value, idx) => (
                          <div key={idx} className="dict-value-row">
                            {editingValue?.key === key && editingValue?.value === value ? (
                              <div className="dict-edit-row">
                                <input className="input input-sm" value={editValue} onChange={e => setEditValue(e.target.value)} onKeyDown={e => e.key === 'Enter' && saveEdit()} autoFocus />
                                <button className="btn btn-sm btn-primary" onClick={saveEdit}><Check size={14} /></button>
                                <button className="btn btn-sm btn-ghost" onClick={() => { setEditingValue(null); setEditValue('') }}><X size={14} /></button>
                              </div>
                            ) : (
                              <span className="dict-value">{value}</span>
                            )}
                            <div className="dict-value-actions">
                              <button className="btn btn-icon btn-ghost" onClick={() => startEdit(key, value)} title="Редактировать"><Edit2 size={14} /></button>
                              <button className="btn btn-icon btn-ghost btn-danger" onClick={() => deleteValue(key, value)} title="Удалить"><Trash2 size={14} /></button>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div className="dict-add-row">
                        <input 
                          className="input input-sm" 
                          placeholder="Добавить значение..." 
                          value={key === expandedDict ? newValue : ''} 
                          onChange={e => { setExpandedDict(key); setNewValue(e.target.value) }} 
                          onKeyDown={e => e.key === 'Enter' && addValue()} 
                        />
                        <button className="btn btn-sm btn-primary" onClick={addValue} disabled={!newValue.trim()}><Plus size={14} />Добавить</button>
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="filters-bar glass-card">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input type="text" placeholder="Поиск..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
        <div className="category-tabs">
          <button key="all" className={`category-tab ${category === 'Все' ? 'active' : ''}`} onClick={() => setCategory('Все')}>Все</button>
          {enums.categories.map((cat, idx) => (
            <button key={cat || idx} className={`category-tab ${category === cat ? 'active' : ''}`} onClick={() => setCategory(cat)}>{cat}</button>
          ))}
        </div>
      </div>

      <div className="table-card glass-card">
        <table className="table">
          <thead>
            <tr><th>Название</th><th>Бренд</th><th>Категория</th><th>Объём</th><th>Сегмент</th>{canEdit && <th>Действия</th>}</tr>
          </thead>
          <tbody>
            {filteredProducts.map((product, idx) => (
              <tr key={product?.id || idx} onClick={() => handleEdit(product)} style={{cursor: canEdit ? 'pointer' : 'default'}}>
                <td><div className="product-name">{product.name}</div>{product.description && <div className="product-desc">{product.description.substring(0, 50)}...</div>}</td>
                <td><span className="brand-badge">{product.brand}</span></td>
                <td><span className="category-badge">{product.category}</span></td>
                <td>{product.volume}</td>
                <td><span className={`segment-badge ${getSegmentClass(product.segment)}`}>{product.segment}</span></td>
                {canEdit && (
                  <td>
                    <div className="action-buttons">
                      <button className="btn btn-ghost btn-sm" onClick={e => { e.stopPropagation(); handleEdit(product) }}><Edit2 size={16} /></button>
                      <button className="btn btn-ghost btn-sm" onClick={e => { e.stopPropagation(); handleDelete(product) }}><Trash2 size={16} /></button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filteredProducts.length === 0 && <div className="empty-state glass-card"><p>Продукты не найдены</p></div>}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal glass-card modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingProduct ? 'Редактировать' : 'Добавить продукт'}</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setShowModal(false)}><X size={20} /></button>
            </div>
            <form onSubmit={handleSave}>
              <div className="form-grid">
                <div className="form-group"><label>Название *</label><input name="name" defaultValue={editingProduct?.name} className="input" required /></div>
                <div className="form-group">
                  <label>Бренд *</label>
                  <select name="brand" defaultValue={editingProduct?.brand || 'Aura'} className="input">
                    {enums.brands.map(brand => <option key={brand} value={brand}>{brand}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Категория</label>
                  <select name="category" defaultValue={editingProduct?.category || 'Увлажнение'} className="input">
                    {enums.categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Объём</label>
                  <select name="volume" defaultValue={editingProduct?.volume || '50мл'} className="input">
                    {enums.volumes.map(vol => <option key={vol} value={vol}>{vol}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Сегмент</label>
                  <select name="segment" defaultValue={editingProduct?.segment || 'Бюджетная'} className="input">
                    {enums.segments.map(seg => <option key={seg} value={seg}>{seg}</option>)}
                  </select>
                </div>
                <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Описание</label><textarea name="description" defaultValue={editingProduct?.description || ''} className="input textarea" rows="3" /></div>
                <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Фотографии (URL)</label><textarea name="images" defaultValue={editingProduct?.images?.join('\n') || ''} className="input textarea" rows="4" placeholder="URL на строку" /></div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-ghost" onClick={() => setShowModal(false)}>Отмена</button>
                <button type="submit" className="btn btn-primary">Сохранить</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteModal && (
        <div className="modal-overlay" onClick={() => setDeleteModal(null)}>
          <div className="modal glass-card confirm-modal" onClick={e => e.stopPropagation()}>
            <div className="confirm-icon"><AlertTriangle size={32} /></div>
            <h3>Удалить продукт?</h3>
            <p>Вы уверены, что хотите удалить "{deleteModal.name}"? Это действие нельзя отменить.</p>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setDeleteModal(null)}>Отмена</button>
              <button className="btn btn-danger" onClick={confirmDelete}>Удалить</button>
            </div>
          </div>
        </div>
      )}

      {confirmAction && (
        <div className="modal-overlay" onClick={() => setConfirmAction(null)}>
          <div className="modal glass-card confirm-modal" onClick={e => e.stopPropagation()}>
            <div className="confirm-icon"><AlertTriangle size={32} /></div>
            <h3>Подтверждение</h3>
            <p>Вы уверены, что хотите {confirmAction.label}?</p>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setConfirmAction(null)}>Отмена</button>
              <button className="btn btn-danger" onClick={confirmActionHandler}>Подтвердить</button>
            </div>
          </div>
        </div>
      )}

      {fullPageDict && (
        <div className="modal-overlay fullpage-dict-overlay" onClick={closeFullPage}>
          <div className="modal glass-card fullpage-dict" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{dictConfig[fullPageDict].label}</h3>
              <button className="btn btn-ghost btn-sm" onClick={closeFullPage}><X size={20} /></button>
            </div>
            <div className="fullpage-dict-content">
              <div className="fullpage-search">
                <Search className="search-icon" />
                <input type="text" placeholder="Фильтр..." className="search-input" />
              </div>
              <div className="fullpage-list">
                {(enums[fullPageDict] || []).map((value, idx) => (
                  <div key={idx} className="fullpage-item">
                    <span>{value}</span>
                    <div className="fullpage-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => { closeFullPage(); startEdit(fullPageDict, value) }}><Edit2 size={14} /></button>
                      <button className="btn btn-ghost btn-sm btn-danger" onClick={() => { deleteValue(fullPageDict, value) }}><Trash2 size={14} /></button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-primary" onClick={closeFullPage}>Закрыть</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
