import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, X, Image, Tag, Check, Package, AlertTriangle, Link as LinkIcon, FlaskConical, FileText, Settings, Palette, MapPin, Factory, Clock, Droplets, Play, Download, Upload } from 'lucide-react'
import { productsApi, dictionariesApi } from '../api'
import Select from '../components/Select'
import VideoPlayer from '../components/VideoPlayer'
import './Products.css'

const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'],
  countries: ['Франция', 'Германия', 'Италия', 'Испания', 'Япония', 'Корея', 'США', 'Великобритания', 'Швейцария', 'Швеция', 'Россия', 'Китай', 'Израиль', 'Таиланд', 'Индия', 'Бразилия', 'Австралия', 'Канада']
}

const productEnums = {
  product_types: ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Эмульсия', 'Масло', 'Гель', 'Пилинг', 'Маска', 'Бальзам', 'Спрей', 'Мист'],
  for_whom: ['Универсальный', 'Мужчинам', 'Женщинам'],
  purposes: ['Увлажнение', 'Очищение', 'Питание', 'Антивозрастной', 'Отбеливание', 'Защита от солнца', 'Проблемная кожа', 'Восстановление', 'Матирование', 'Тонирование'],
  skin_types: ['Сухая', 'Жирная', 'Комбинированная', 'Нормальная', 'Чувствительная', 'Проблемная'],
  application_times: ['Утро', 'Вечер', 'Утро/Вечер'],
  areas: ['Лицо', 'Тело', 'Волосы', 'Губы', 'Руки', 'Веки', 'Зона вокруг глаз']
}

export default function Products() {
  const { hasPermission } = useAuth()
  const { success, error, danger } = useToast()
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
    what_is_it: '',
    brand: '',
    product_type: '',
    for_whom: '',
      purpose: [],
      skin_type: [],
    application_time: '',
    area: '',
    active_ingredient: '',
    volume: '',
    segment: '',
    composition: '',
    application_info: '',
    country: '',
    description: '',
    photos: [],
    has_video: false,
    video: null
  })
  const [mediaModal, setMediaModal] = useState(null)
  const [draggedPhotoIndex, setDraggedPhotoIndex] = useState(null)
  const [uploadingPhoto, setUploadingPhoto] = useState(false)
  const [uploadingVideo, setUploadingVideo] = useState(false)
  const [url, setUrl] = useState('')
  const [parsing, setParsing] = useState(false)
  const [importing, setImporting] = useState(false)

  const canEdit = hasPermission('products')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const productsData = await productsApi.getAll().catch(e => { console.error('products error:', e); return [] })
      const brandsData = await dictionariesApi.get('brands').catch(e => { console.error('brands error:', e); return defaultEnums.brands })
      const categoriesData = await dictionariesApi.get('categories').catch(e => { console.error('categories error:', e); return defaultEnums.categories })
      const segmentsData = await dictionariesApi.get('segments').catch(e => { console.error('segments error:', e); return defaultEnums.segments })
      const volumesData = await dictionariesApi.get('volumes').catch(e => { console.error('volumes error:', e); return defaultEnums.volumes })
      const countriesData = await dictionariesApi.get('countries').catch(e => { console.error('countries error:', e); return [] })
      const productTypesData = await dictionariesApi.get('product_types').catch(e => { console.error('product_types error:', e); return productEnums.product_types })
      const forWhomData = await dictionariesApi.get('for_whom').catch(e => { console.error('for_whom error:', e); return productEnums.for_whom })
      const purposesData = await dictionariesApi.get('purposes').catch(e => { console.error('purposes error:', e); return productEnums.purposes })
      const skin_typesData = await dictionariesApi.get('skin_types').catch(e => { console.error('skin_types error:', e); return productEnums.skin_types })
      const applicationTimesData = await dictionariesApi.get('application_times').catch(e => { console.error('application_times error:', e); return productEnums.application_times })
      const areasData = await dictionariesApi.get('areas').catch(e => { console.error('areas error:', e); return productEnums.areas })
      
      setProducts(productsData)
      setEnums({
        brands: brandsData,
        categories: categoriesData,
        segments: segmentsData,
        volumes: volumesData,
        countries: countriesData,
        product_types: productTypesData,
        for_whom: forWhomData,
        purposes: purposesData,
        skin_types: skin_typesData,
        application_times: applicationTimesData,
        areas: areasData
      })
    } catch (err) {
      error('Ошибка загрузки данных')
      setProducts([])
      setEnums({...defaultEnums, ...productEnums})
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
        danger(`Продукт "${deleteModal.name}" удалён`)
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
      what_is_it: product.what_is_it || '',
      brand: product.brand || '',
      product_type: product.product_type || '',
      for_whom: product.for_whom || '',
      purpose: Array.isArray(product.purpose) ? product.purpose : (product.purpose ? [product.purpose] : []),
      skin_type: Array.isArray(product.skin_type) ? product.skin_type : (product.skin_type ? [product.skin_type] : []),
      application_time: product.application_time || '',
      area: product.area || '',
      active_ingredient: product.active_ingredient || '',
      volume: product.volume || '',
      segment: product.segment || '',
      composition: product.composition || '',
      application_info: product.application_info || '',
      country: product.country || '',
      description: product.description || '',
      photos: product.photos || [],
      has_video: product.has_video || false,
      video: product.video || null
    })
    setShowModal(true) 
  }
  const openAddModal = async () => {
    try {
      // Create empty product first to get ID for photo/video uploads
      const firstBrand = enums.brands[0]
      const brandValue = typeof firstBrand === 'object' ? firstBrand.value : firstBrand
      const created = await productsApi.create({
        name: '',
        brand: brandValue || '',
        volume: enums.volumes[0] || ''
      })
      setProducts(prev => [created, ...prev])
      handleEdit(created)
    } catch (err) {
      error('Ошибка создания продукта')
    }
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSelectChange = (name, value) => {
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleParseUrl = async () => {
    if (!url) return
    setParsing(true)
    try {
      let data = null
      
      if (url.includes('wildberries.ru')) {
        const wbMatch = url.match(/catalog\/(\d+)/)
        if (wbMatch) {
          const productId = wbMatch[1]
          data = await parseWildberries(productId)
        }
      }
      
      if (!data || !data.name) {
        data = await productsApi.parseUrl(url)
      }
      
      if (data?.name) {
        setFormData(prev => ({
          ...prev,
          name: data.name || prev.name,
          brand: data.brand || prev.brand,
          category: data.category || prev.category,
          description: data.description || prev.description,
          volume: data.volume || prev.volume,
          images: data.images?.join('\n') || prev.images
        }))
        success('Данные успешно получены')
      } else {
        error('Не удалось автоматически получить данные. Пожалуйста, заполните поля вручную.')
      }
    } catch (err) {
      error('Не удалось распарсить ссылку. Заполните поля вручную.')
    } finally {
      setParsing(false)
    }
  }
  
  const parseWildberries = async (productId) => {
    try {
      const response = await fetch(`https://www.wildberries.ru/catalog/${productId}/detail.aspx`)
      const text = await response.text()
      const parser = new DOMParser()
      const doc = parser.parseFromString(text, 'text/html')
      
      const result = { name: null, brand: null, description: null, images: [], volume: null }
      
      const ogTitle = doc.querySelector('meta[property="og:title"]')
      if (ogTitle) result.name = ogTitle.content
      
      if (!result.name) {
        const title = doc.querySelector('title')
        if (title) result.name = title.textContent.split(' — ')[0].split(' | ')[0].trim()
      }
      
      const h1 = doc.querySelector('h1')
      if (h1 && !result.name) result.name = h1.textContent.trim()
      
      const brandEl = doc.querySelector('[data-link="text__brandName"]') || doc.querySelector('.brand-name')
      if (brandEl) result.brand = brandEl.textContent.trim()
      
      const descEl = doc.querySelector('meta[name="description"]')
      if (descEl) result.description = descEl.content
      
      const images = doc.querySelectorAll('img')
      images.forEach(img => {
        const src = img.src || img.dataset?.src
        if (src && src.includes('wb')) {
          if (!result.images.includes(src) && result.images.length < 5) {
            result.images.push(src)
          }
        }
      })
      
      const priceEl = doc.querySelector('[class*="price"]') || doc.querySelector('.product-prices__price')
      if (priceEl) {
        const volMatch = priceEl.textContent.match(/(\d+\s*(?:мл|ml|г|g|л|l))/i)
        if (volMatch) result.volume = volMatch[1]
      }
      
      return result
    } catch (e) {
      console.error('Parse error:', e)
      return null
    }
  }

  const handlePhotoUpload = async (file) => {
    if (!editingProduct) {
      error('Сначала сохраните продукт')
      return
    }
    const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    if (!validTypes.includes(file.type)) {
      error('Поддерживаются только JPG, PNG, WebP, GIF')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      error('Максимальный размер файла - 10MB')
      return
    }
    try {
      setUploadingPhoto(true)
      const result = await productsApi.uploadPhoto(editingProduct.id, file)
      setFormData(prev => ({
        ...prev,
        photos: [...prev.photos, result]
      }))
      success('Фото загружено')
    } catch (err) {
      error('Ошибка загрузки фото')
    } finally {
      setUploadingPhoto(false)
    }
  }

  const handlePhotoDelete = async (photoId) => {
    try {
      setUploadingPhoto(true)
      await productsApi.deletePhoto(editingProduct.id, photoId)
      setFormData(prev => ({
        ...prev,
        photos: prev.photos.filter(p => p.id !== photoId)
      }))
      success('Фото удалено')
    } catch (err) {
      error('Ошибка удаления')
    } finally {
      setUploadingPhoto(false)
    }
  }

  const handleVideoUpload = async (file) => {
    if (!editingProduct) {
      error('Сначала сохраните продукт')
      return
    }
    const validTypes = ['video/mp4', 'video/webm']
    if (!validTypes.includes(file.type)) {
      error('Поддерживаются только MP4, WebM')
      return
    }
    if (file.size > 50 * 1024 * 1024) {
      error('Максимальный размер файла - 50MB')
      return
    }
    try {
      setUploadingVideo(true)
      await productsApi.uploadVideo(editingProduct.id, file)
      // Refresh product to get actual video path from backend
      const freshProduct = await productsApi.getById(editingProduct.id)
      setEditingProduct(freshProduct)
      setFormData(prev => ({ ...prev, has_video: true, video: freshProduct.video }))
      success('Видео загружено')
    } catch (err) {
      console.error('Video upload error:', err)
      error('Ошибка загрузки видео: ' + err.message)
    } finally {
      setUploadingVideo(false)
    }
  }

  const handleVideoDelete = async () => {
    try {
      setUploadingVideo(true)
      await productsApi.deleteVideo(editingProduct.id)
      setFormData(prev => ({ ...prev, has_video: false, video: null }))
      success('Видео удалено')
    } catch (err) {
      error('Ошибка удаления')
    } finally {
      setUploadingVideo(false)
    }
  }

  const handlePhotoDragStart = (index) => {
    setDraggedPhotoIndex(index)
  }

  const handlePhotoDragOver = (e) => {
    e.preventDefault()
  }

  const handlePhotoDrop = async (dropIndex) => {
    if (draggedPhotoIndex === null || draggedPhotoIndex === dropIndex) {
      setDraggedPhotoIndex(null)
      return
    }
    const newPhotos = [...formData.photos]
    const [draggedPhoto] = newPhotos.splice(draggedPhotoIndex, 1)
    newPhotos.splice(dropIndex, 0, draggedPhoto)
    setDraggedPhotoIndex(null)
    setFormData(prev => ({ ...prev, photos: newPhotos }))
    
    try {
      const product = { ...formData, photos: newPhotos }
      await productsApi.update(editingProduct.id, product)
      success('Порядок фото обновлён')
    } catch (err) {
      error('Ошибка сохранения порядка')
    }
  }

  const handleSave = async (e) => {
    e.preventDefault()
    const product = {
      name: formData.name,
      what_is_it: formData.what_is_it,
      brand: formData.brand,
      product_type: formData.product_type,
      for_whom: formData.for_whom,
      purpose: formData.purpose,
      skin_type: formData.skin_type,
      application_time: formData.application_time,
      area: formData.area,
      active_ingredient: formData.active_ingredient,
      volume: formData.volume,
      segment: formData.segment,
      composition: formData.composition,
      application_info: formData.application_info,
      country: formData.country,
      description: formData.description,
      photos: formData.photos,
      has_video: formData.has_video,
      video: formData.video
    }
    try {
      const updated = await productsApi.update(editingProduct.id, product)
      setProducts(prev => prev.map(p => p.id === editingProduct.id ? updated : p))
      success('Продукт сохранён')
      setShowModal(false)
      setEditingProduct(null)
    } catch (err) {
      console.error('Save error:', err)
      error('Ошибка сохранения: ' + err.message)
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
          <button className="btn btn-ghost" onClick={() => productsApi.export()}>
            <Download size={18} />Экспорт
          </button>
          <label className="btn btn-ghost" style={{cursor: 'pointer'}}>
            <Upload size={18} />Импорт
            <input type="file" accept=".csv" style={{display: 'none'}} onChange={async (e) => {
              const file = e.target.files?.[0]
              if (!file) return
              setImporting(true)
              try {
                const result = await productsApi.import(file)
                success(`Импортировано ${result.created} продуктов`)
                loadData()
              } catch (err) {
                error('Ошибка импорта: ' + err.message)
              } finally {
                setImporting(false)
              }
            }} disabled={importing} />
          </label>
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
            <tr><th>Название</th><th>Бренд</th><th>Тип</th><th>Объём</th><th>Сегмент</th>{canEdit && <th>Действия</th>}</tr>
          </thead>
          <tbody>
            {filteredProducts.map((product, idx) => (
              <tr key={product?.id || idx} onClick={() => handleEdit(product)} style={{cursor: canEdit ? 'pointer' : 'default'}}>
                <td><div className="product-name">{product.name}</div>{product.description && <div className="product-desc">{product.description?.substring(0, 50)}...</div>}</td>
                <td><span className="brand-badge">{product.brand}</span></td>
                <td><span className="category-badge">{product.product_type}</span></td>
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
              <h3>Продукт</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setShowModal(false)}><X size={20} /></button>
            </div>
            <form onSubmit={handleSave} className="product-form">
              <div className="form-grid" style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
                {/* Секция 1: О продукте */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(139, 92, 246, 0.1)', color: '#8B5CF6' }}><FlaskConical size={16} /></div>
                    <span>О продукте</span>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 'var(--spacing-md)' }}>
                    <div className="form-group"><label>Название *</label><input name="name" value={formData.name} onChange={handleInputChange} className="input" placeholder="Введите название продукта" required /></div>
                    <div className="form-group"><label>Что это?</label><input name="what_is_it" value={formData.what_is_it} onChange={handleInputChange} className="input" placeholder="Например: увлажняющий крем" /></div>
                    <div className="form-group"><Select label="Бренд *" name="brand" value={formData.brand} onChange={handleSelectChange} options={enums.brands} searchable /></div>
                    <div className="form-group"><Select label="Сегмент" name="segment" value={formData.segment} onChange={handleSelectChange} options={enums.segments} placeholder="Выберите сегмент" searchable /></div>
                  </div>
                </div>

                {/* Секция 2: Описание */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(107, 114, 128, 0.1)', color: '#6B7280' }}><FileText size={16} /></div>
                    <span>Описание</span>
                  </div>
                  <div className="form-group"><label>Описание</label><textarea name="description" value={formData.description} onChange={handleInputChange} className="input textarea" rows="3" /></div>
                </div>

                {/* Секция 3: Характеристики */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}><Settings size={16} /></div>
                    <span>Характеристики</span>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 'var(--spacing-md)' }}>
                    <Select label="Тип продукта" name="product_type" value={formData.product_type} onChange={handleSelectChange} options={enums.product_types || productEnums.product_types} placeholder="Выберите тип" searchable />
                    <Select label="Для кого" name="for_whom" value={formData.for_whom} onChange={handleSelectChange} options={enums.for_whom || productEnums.for_whom} placeholder="Выберите" searchable />
                    <Select label="Назначение" name="purpose" value={formData.purpose} onChange={handleSelectChange} options={enums.purposes || productEnums.purposes} placeholder="Выберите назначение" multiple={true} searchable />
                    <Select label="Тип кожи" name="skin_type" value={formData.skin_type} onChange={handleSelectChange} options={enums.skin_types || productEnums.skin_types} placeholder="Выберите тип кожи" multiple={true} searchable />
                    <Select label="Время применения" name="application_time" value={formData.application_time} onChange={handleSelectChange} options={enums.application_times || productEnums.application_times} placeholder="Выберите время" searchable />
                    <Select label="Область применения" name="area" value={formData.area} onChange={handleSelectChange} options={enums.areas || productEnums.areas} placeholder="Выберите область" searchable />
                    <div className="form-group"><label>Активный компонент</label><input name="active_ingredient" value={formData.active_ingredient} onChange={handleInputChange} className="input" placeholder="Например: гиалуроновая кислота" /></div>
                    <Select label="Объём" name="volume" value={formData.volume} onChange={handleSelectChange} options={enums.volumes} placeholder="Выберите объём" searchable />
                  </div>
                </div>

                {/* Секция 4: Применение */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(59, 130, 246, 0.1)', color: '#3B82F6' }}><Droplets size={16} /></div>
                    <span>Применение</span>
                  </div>
                  <div className="form-group"><label>Информация о применении</label><textarea name="application_info" value={formData.application_info} onChange={handleInputChange} className="input textarea" rows="2" placeholder="Как наносить, частота использования..." /></div>
                </div>

                {/* Секция 5: Состав */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(107, 114, 128, 0.1)', color: '#6B7280' }}><FileText size={16} /></div>
                    <span>Состав</span>
                  </div>
                  <div className="form-group"><label>Состав</label><textarea name="composition" value={formData.composition} onChange={handleInputChange} className="input textarea" rows="3" placeholder="Список ингредиентов..." /></div>
                </div>

                {/* Секция 8: Медиа (только при редактировании) */}
                {editingProduct && (
                  <div className="form-section" style={{ gridColumn: 'span 2' }}>
                    <div className="form-section-header">
                      <div className="form-section-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#F59E0B' }}><Image size={16} /></div>
                      <span>Медиа</span>
                    </div>
                      <div className="form-group">
                        <label>Фотографии</label>
                        <div className="media-upload-section">
                          {formData.photos?.length > 0 && (
                            <div className="photo-grid">
                              {formData.photos.map((photo, idx) => (
                                <div 
                                  key={photo.id || idx} 
                                  className={`photo-card ${draggedPhotoIndex === idx ? 'dragging' : ''}`}
                                  draggable
                                  onDragStart={() => handlePhotoDragStart(idx)}
                                  onDragOver={handlePhotoDragOver}
                                  onDrop={() => handlePhotoDrop(idx)}
                                  onClick={() => setMediaModal({ type: 'image', src: photo.data && photo.data.length > 0 ? `data:${photo.content_type};base64,${photo.data}` : null })}
                                >
                                  <div className="photo-preview">
                                    {photo.data && photo.data.length > 0 ? (
                                      <img 
                                        src={`data:${photo.content_type};base64,${photo.data}`}
                                        alt="" 
                                      />
                                    ) : (
                                      <div className="photo-placeholder" title="Нажмите для просмотра">
                                        <Image size={32} />
                                        <span className="photo-filename">{photo.filename?.split('.')[0] || 'Фото'}</span>
                                        <span className="photo-type">{photo.content_type?.split('/')[1]?.toUpperCase() || ''}</span>
                                      </div>
                                    )}
                                  </div>
                                  <button type="button" className="photo-delete-btn" onClick={(e) => { e.stopPropagation(); handlePhotoDelete(photo.id) }}>
                                    <X size={14} />
                                  </button>
                                </div>
                              ))}
                            </div>
                          )}
                        <label className={`upload-zone ${uploadingPhoto ? 'uploading' : ''}`}>
                          {uploadingPhoto ? (
                            <div className="upload-content">
                              <span className="spinner" style={{ width: 24, height: 24 }}></span>
                              <span className="upload-text">Загрузка...</span>
                            </div>
                          ) : (
                            <>
                              <input 
                                type="file" 
                                accept="image/*" 
                                onChange={e => e.target.files[0] && handlePhotoUpload(e.target.files[0])} 
                                className="hidden-input"
                              />
                              <div className="upload-content">
                                <Image size={24} className="upload-icon" />
                                <span className="upload-text">Добавить фото</span>
                                <span className="upload-hint">PNG, JPG, WebP до 10MB</span>
                              </div>
                            </>
                          )}
                        </label>
                      </div>
                    </div>
                    <div className="form-group" style={{ marginTop: 'var(--spacing-md)' }}>
                      <label>Видео</label>
                      <div className="media-upload-section">
                        {formData.has_video ? (
                          <VideoPlayer productId={editingProduct.id} onDelete={handleVideoDelete} />
                        ) : (
                          <label className={`upload-zone ${uploadingVideo ? 'uploading' : ''}`}>
                            {uploadingVideo ? (
                              <div className="upload-content">
                                <span className="spinner" style={{ width: 24, height: 24 }}></span>
                                <span className="upload-text">Загрузка...</span>
                              </div>
                            ) : (
                              <>
                                <input 
                                  type="file" 
                                  accept="video/mp4" 
                                  onChange={e => e.target.files[0] && handleVideoUpload(e.target.files[0])} 
                                  className="hidden-input"
                                />
                                <div className="upload-content">
                                  <Image size={24} className="upload-icon" />
                                  <span className="upload-text">Добавить видео</span>
                                  <span className="upload-hint">MP4, WebM до 50MB</span>
                                </div>
                              </>
                            )}
                          </label>
                        )}
                      </div>
                    </div>
                  </div>
                )}

                {/* Секция 9: Импорт */}
                <div className="form-section" style={{ gridColumn: 'span 2' }}>
                  <div className="form-section-header">
                    <div className="form-section-icon" style={{ background: 'rgba(5, 150, 105, 0.1)', color: '#059669' }}><LinkIcon size={16} /></div>
                    <span>Импорт ссылки</span>
                  </div>
                  <div className="url-parser-section" style={{ border: 'none', padding: 0, margin: 0, background: 'transparent' }}>
                    <div className="url-parser-input-group">
                      <input 
                        name="url" 
                        value={url} 
                        onChange={e => setUrl(e.target.value)} 
                        className="input" 
                        placeholder="Вставьте ссылку на продукт (Wildberries, Ozon...)" 
                      />
                      <button 
                        type="button" 
                        className="btn btn-primary" 
                        onClick={handleParseUrl} 
                        disabled={!url || parsing}
                      >
                        {parsing ? (
                          <><span className="spinner"></span>Загрузка...</>
                        ) : (
                          <>Импорт</>
                        )}
                      </button>
                    </div>
                    <p className="url-parser-hint">Автоматический импорт недоступен из-за защиты маркетплейсов. Скопируйте название и бренд с сайта вручную</p>
                  </div>
                </div>
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

      {mediaModal && (
        <div className="modal-overlay" onClick={() => setMediaModal(null)}>
          <div className="modal media-modal" onClick={e => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={() => setMediaModal(null)}>
              <X size={24} />
            </button>
            {mediaModal.type === 'image' ? (
              <img src={mediaModal.src} alt="Preview" className="media-modal-image" />
            ) : (
              <video src={mediaModal.src} controls className="media-modal-video" />
            )}
          </div>
        </div>
      )}
    </div>
  )
}
