import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Plus, Edit2, Trash2, BookOpen, Tag, Calendar, User, X, Settings } from 'lucide-react'
import { contentApi, dictionariesApi, usersApi } from '../api'
import Select from '../components/Select'
import DictionaryPanel from '../components/DictionaryPanel'
import './Content.css'

const defaultContentCategories = ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни']

const contentDictConfig = {
  contentCategories: { label: 'Категории контента', icon: Settings, color: '#EC4899' }
}

export default function Content() {
  const { user, hasPermission } = useAuth()
  const [articles, setArticles] = useState([])
  const [cosmetologists, setCosmetologists] = useState([])
  const [contentCategories, setContentCategories] = useState(defaultContentCategories)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [editingArticle, setEditingArticle] = useState(null)
  const [showDictPanel, setShowDictPanel] = useState(false)
  const [fullPageDict, setFullPageDict] = useState(null)
  const [fullPageFilter, setFullPageFilter] = useState('')
  const [form, setForm] = useState({ 
    title: '', 
    category: '', 
    tags: '', 
    author: '',
    body: '',
    image_url: '',
    published: false
  })

  const canCreate = hasPermission('content_create')
  const canEdit = hasPermission('content_edit_own')
  const canManageEnums = user?.role === 'admin'

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [articlesData, usersData, contentCategoriesData] = await Promise.all([
        contentApi.getAll().catch(() => []),
        usersApi.getAll().catch(() => []),
        dictionariesApi.get('contentCategories').catch(() => defaultContentCategories),
      ])
      setArticles(articlesData)
      setContentCategories(contentCategoriesData)
      const cosmetologistsList = usersData.filter(u => u.role === 'cosmetologist').map(u => ({
        id: u.id,
        name: u.name,
        nickname: u.nickname || `@${u.name?.toLowerCase().replace(/\s+/g, '_') || 'user'}`,
        role: 'cosmetologist'
      }))
      setCosmetologists(cosmetologistsList)
    } catch (err) {
      setArticles([])
      setContentCategories(defaultContentCategories)
    } finally {
      setLoading(false)
    }
  }

  const filtered = articles.filter(a => a.title?.toLowerCase().includes(search.toLowerCase()))

  const openAddModal = () => {
    setEditingArticle(null)
    setForm({ 
      title: '', 
      category: contentCategories[0] || '', 
      tags: '', 
      author: cosmetologists[0]?.nickname || '',
      body: '',
      image_url: '',
      published: false
    })
    setModalOpen(true)
  }

  const openEditModal = (article) => {
    setEditingArticle(article)
    setForm({
      title: article.title || '',
      category: article.category || '',
      tags: article.tags?.join(', ') || '',
      author: article.author || '',
      body: article.body || '',
      image_url: article.image_url || '',
      published: article.published || false
    })
    setModalOpen(true)
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSelectChange = (name, value) => {
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleSave = async () => {
    if (!form.title) return

    const tags = form.tags.split(',').map(t => t.trim()).filter(Boolean)
    const articleData = {
      title: form.title,
      category: form.category,
      tags,
      author: form.author,
      body: form.body || '',
      image_url: form.image_url || '',
      published: form.published
    }

    try {
      if (editingArticle) {
        const updated = await contentApi.update(editingArticle.id, articleData)
        setArticles(prev => prev.map(a => a.id === editingArticle.id ? updated : a))
      } else {
        const created = await contentApi.create(articleData)
        setArticles(prev => [created, ...prev])
      }
      setModalOpen(false)
    } catch (err) {
      console.error('Error saving article:', err)
    }
  }

  const handleDelete = async () => {
    if (deleteModal) {
      try {
        await contentApi.delete(deleteModal.id)
        setArticles(prev => prev.filter(a => a.id !== deleteModal.id))
      } catch (err) {
        console.error('Error deleting article:', err)
      }
      setDeleteModal(null)
    }
  }

  const handleDictAdd = async (key, value) => {
    try {
      await dictionariesApi.create(key, value)
      setContentCategories(prev => [...prev, value])
    } catch (err) {
      console.error('Error adding dictionary value:', err)
    }
  }

  const handleDictDelete = async (key, value) => {
    try {
      await dictionariesApi.delete(key, value)
      setContentCategories(prev => prev.filter(v => v !== value))
    } catch (err) {
      console.error('Error deleting dictionary value:', err)
    }
  }

  const handleDictUpdate = async (key, oldValue, newValue) => {
    try {
      await dictionariesApi.update(key, oldValue, newValue)
      setContentCategories(prev => prev.map(v => v === oldValue ? newValue : v))
    } catch (err) {
      console.error('Error updating dictionary value:', err)
    }
  }

  const openFullPage = (key) => setFullPageDict(key)
  const closeFullPage = () => { setFullPageDict(null); setFullPageFilter('') }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleDateString('ru-RU')
  }

  if (loading) {
    return (
      <div className="content-page">
        <div className="loading-state">Загрузка...</div>
      </div>
    )
  }

  return (
    <div className="content-page">
      <div className="page-header">
        <div>
          <h2>База знаний</h2>
          <p>Управление статьями и контентом</p>
        </div>
        <div className="header-actions">
          {canManageEnums && (
            <button className={`btn ${showDictPanel ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setShowDictPanel(!showDictPanel)}>
              <Settings size={16} />{showDictPanel ? 'Скрыть' : 'Справочники'}
            </button>
          )}
          {canCreate && (
            <button className="btn btn-primary" onClick={openAddModal}><Plus size={18} />Добавить статью</button>
          )}
        </div>
      </div>

      {showDictPanel && canManageEnums && (
        <DictionaryPanel
          dictionaries={{ contentCategories }}
          config={contentDictConfig}
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
          <input type="text" placeholder="Поиск статей..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
        </div>
      </div>

      <div className="articles-list">
        {filtered.map(article => (
          <div key={article.id} className="article-card glass-card clickable" onClick={() => openEditModal(article)}>
            <div className="article-icon"><BookOpen size={24} /></div>
            <div className="article-content">
              <h4>{article.title}</h4>
              <div className="article-meta">
                <span className="meta-tag"><Tag size={14} />{article.category}</span>
                <span className="meta-tag"><User size={14} />{article.author}</span>
                <span className="meta-tag"><Calendar size={14} />{formatDate(article.created_at)}</span>
              </div>
              <div className="article-tags">
                {article.tags?.map(tag => (
                  <span key={tag} className="tag">{tag}</span>
                ))}
              </div>
            </div>
            {(canEdit || user?.role === 'admin') && (
              <div className="article-actions">
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); openEditModal(article) }}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setDeleteModal(article) }}><Trash2 size={16} /></button>
              </div>
            )}
          </div>
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="empty-state glass-card">
          <p>Статьи не найдены</p>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal glass-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingArticle ? 'Редактировать статью' : 'Новая статья'}</h3>
              <button className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}><X size={20} /></button>
            </div>
            <div className="form-grid">
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Заголовок *</label>
                <input className="input" name="title" value={form.title} onChange={handleInputChange} placeholder="Название статьи" required />
              </div>
              <Select label="Категория" name="category" value={form.category} onChange={handleSelectChange} options={contentCategories} placeholder="Выберите категорию" />
              <Select 
                label="Автор" 
                name="author" 
                value={form.author} 
                onChange={handleSelectChange} 
                options={cosmetologists.map(c => c.nickname)} 
                placeholder="Выберите автора" 
              />
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Теги (через запятую)</label>
                <input className="input" name="tags" value={form.tags} onChange={handleInputChange} placeholder="увлажнение, сухая кожа" />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Изображение (URL)</label>
                <input className="input" name="image_url" value={form.image_url} onChange={handleInputChange} placeholder="https://..." />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Содержание</label>
                <textarea className="input textarea" name="body" value={form.body} onChange={handleInputChange} rows="4" />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label className="checkbox-label">
                  <input type="checkbox" name="published" checked={form.published} onChange={handleInputChange} />
                  Опубликовано
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
            <h3>Удалить статью?</h3>
            <p style={{marginTop: '8px', color: 'var(--color-gray-500)'}}>
              Вы уверены, что хотите удалить "{deleteModal.title}"?
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
