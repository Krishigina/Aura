import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, X, Image, Tag, Check, Package, AlertTriangle } from 'lucide-react'
import { productsApi, dictionariesApi } from '../api'
import Select from '../components/Select'
import './Products.css'

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л']
}

export default function Products() {
  const { hasPermission } = useAuth()
  const { success, error } = useToast()
  const [products, setProducts] = useState([])
  const [enums, setEnums] = useState(defaultEnums)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Все')
  const [showModal, setShowModal] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [deleteModal, setDeleteModal] = useState(null)
  const [formData, setFormData] = useState({
    name: '',
    brand: '',
    category: '',
    volume: '',
    segment: '',
    description: '',
    images: ''
  })

  const canEdit = hasPermission('products')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [productsData, brandsData, categoriesData, segmentsData, volumesData] = await Promise.all([
        productsApi.getAll().catch(() => []),
        dictionariesApi.get('brands').catch(() => defaultEnums.brands),
        dictionariesApi.get('categories').catch(() => defaultEnums.categories),
        dictionariesApi.get('segments').catch(() => defaultEnums.segments),
        dictionariesApi.get('volumes').catch(() => defaultEnums.volumes),
      ])
      setProducts(productsData)
      setEnums({
        brands: brandsData,
        categories: categoriesData,
        segments: segmentsData,
        volumes: volumesData
      })
    } catch (err) {
      error('Ошибка загрузки данных')
      setProducts([])
      setEnums(defaultEnums)
    } finally {
      setLoading(false)
    }
  }

  const filteredProducts = products.filter(product => {
    const matchesSearch = product.name?.toLowerCase().includes(search.toLowerCase()) || 
                         product.brand?.toLowerCase().includes(search.toLowerCase()) ||
                         product.description?.toLowerCase().includes(search.toLowerCase())
    const matchesCategory = category === 'Все' || product.category === category
    return matchesSearch && matchesCategory
  })

  const handleDelete = (product) => setDeleteModal(product)
  const confirmDelete = async () => {
    if (deleteModal) {
      try {
        await productsApi.delete(deleteModal.id)
        setProducts(prev => prev.filter(p => p.id !== deleteModal.id))
        success(`Продукт "${deleteModal.name}" удалён`)
      } catch (err) {
        error('Ошибка удаления')
      }
      setDeleteModal(null)
    }
  }
  const handleEdit = (product) => { 
    setEditingProduct(product)
    setFormData({
      name: product.name || '',
      brand: product.brand || '',
      category: product.category || '',
      volume: product.volume || '',
      segment: product.segment || '',
      description: product.description || '',
      images: product.images?.join('\n') || ''
    })
    setShowModal(true) 
  }
  const openAddModal = () => { 
    setEditingProduct(null)
    setFormData({
      name: '',
      brand: enums.brands[0] || '',
      category: enums.categories[0] || '',
      volume: enums.volumes[0] || '',
      segment: enums.segments[0] || '',
      description: '',
      images: ''
    })
    setShowModal(true) 
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSelectChange = (name, value) => {
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSave = async (e) => {
    e.preventDefault()
    const product = {
      name: formData.name,
      brand: formData.brand,
      category: formData.category,
      description: formData.description || '',
      images: formData.images?.split('\n').filter(url => url.trim()) || [],
      volume: formData.volume,
      segment: formData.segment
    }
    try {
      if (editingProduct) {
        const updated = await productsApi.update(editingProduct.id, product)
        setProducts(prev => prev.map(p => p.id === editingProduct.id ? updated : p))
        success('Продукт обновлён')
      } else {
        const created = await productsApi.create(product)
        setProducts(prev => [created, ...prev])
        success('Продукт добавлен')
      }
      setShowModal(false)
      setEditingProduct(null)
    } catch (err) {
      error('Ошибка сохранения')
    }
  }

  const getSegmentClass = (segment) => {
    const classes = { 'Бюджетная': 'segment-budget', 'Люкс': 'segment-lux', 'Профессиональная': 'segment-pro', 'Космецевтика': 'segment-cosmeceutical' }
    return classes[segment] || 'segment-budget'
  }

  if (loading) {
    return (
      <div className="products-page">
        <div className="loading-state">Загрузка...</div>
      </div>
    )
  }

  return (
    <div className="products-page">
      <div className="page-header">
        <div>
          <h2>Продукты</h2>
          <p>Управление косметическими средствами</p>
        </div>
        <div className="header-actions">
          {canEdit && (
            <button className="btn btn-primary" onClick={openAddModal}>
              <Plus size={18} />Добавить
            </button>
          )}
        </div>
      </div>

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
                <td><div className="product-name">{product.name}</div>{product.description && <div className="product-desc">{product.description?.substring(0, 50)}...</div>}</td>
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
            <form onSubmit={handleSave} className="product-form">
              <div className="form-grid">
                <div className="form-group"><label>Название *</label><input name="name" value={formData.name} onChange={handleInputChange} className="input" required /></div>
                <Select label="Бренд *" name="brand" value={formData.brand} onChange={handleSelectChange} options={enums.brands} />
                <Select label="Категория" name="category" value={formData.category} onChange={handleSelectChange} options={enums.categories} placeholder="Выберите категорию" />
                <Select label="Объём" name="volume" value={formData.volume} onChange={handleSelectChange} options={enums.volumes} placeholder="Выберите объём" />
                <Select label="Сегмент" name="segment" value={formData.segment} onChange={handleSelectChange} options={enums.segments} placeholder="Выберите сегмент" />
                <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Описание</label><textarea name="description" value={formData.description} onChange={handleInputChange} className="input textarea" rows="3" /></div>
                <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Фотографии (URL)</label><textarea name="images" value={formData.images} onChange={handleInputChange} className="input textarea" rows="4" placeholder="URL на строку" /></div>
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
    </div>
  )
}
