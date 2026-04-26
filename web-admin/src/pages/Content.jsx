import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Search, Plus, Edit2, Trash2, BookOpen, Tag, Calendar, User, X, ChevronLeft, Image as ImageIcon } from 'lucide-react'
import { contentApi, dictionariesApi, usersApi } from '../api'
import Select from '../components/Select'
import AdvancedRichTextEditor from '../components/AdvancedRichTextEditor'
import './Content.css'

export default function Content() {
  const { user, hasPermission } = useAuth()
  const { success, error } = useToast()
  const [articles, setArticles] = useState([])
  const [cosmetologists, setCosmetologists] = useState([])
  const [contentCategories, setContentCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showEditor, setShowEditor] = useState(false)
  const [editingArticle, setEditingArticle] = useState(null)
  const [deleteModal, setDeleteModal] = useState(null)
  const [form, setForm] = useState({ 
    title: '', 
    category: '', 
    tags: '', 
    author_id: null,
    author_name: '',
    body: '',
    image_url: '',
    published: false
  })
  const [cardImageFile, setCardImageFile] = useState(null)
  const [cardImageRemoved, setCardImageRemoved] = useState(false)
  const [uploadingImage, setUploadingImage] = useState(false)

  const canCreate = hasPermission('content_create')
  const canEdit = hasPermission('content_edit_own')
  const isCosmetologist = user?.role === 'cosmetologist'

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [articlesData, usersData, contentCategoriesData] = await Promise.all([
        contentApi.getAll().catch(() => []),
        usersApi.getCosmetologists().catch(() => []),
        dictionariesApi.get('contentCategories').catch(() => []),
      ])
      setArticles(articlesData)
      setContentCategories(Array.isArray(contentCategoriesData) ? contentCategoriesData : [])
      const cosmetologistsList = usersData.map(u => ({
        id: u.id,
        name: u.name,
        nickname: u.nickname || u.name
      }))
      setCosmetologists(cosmetologistsList)
    } catch (err) {
      setArticles([])
      setContentCategories([])
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
      author_id: isCosmetologist ? user.id : null,
      author_name: isCosmetologist ? (user.nickname || user.name) : '',
      body: '',
      image_url: '',
      published: false
    })
    setCardImageFile(null)
    setCardImageRemoved(false)
    setShowEditor(true)
  }

  const parseTags = (tags) => {
    if (!tags) return []
    if (Array.isArray(tags)) return tags
    try { return JSON.parse(tags) } catch { return tags.split(',').map(t => t.trim()).filter(Boolean) }
  }

  const openEditModal = (article) => {
    setEditingArticle(article)
    const parsedTags = parseTags(article.tags).join(', ')
    setForm({
      title: article.title || '',
      category: article.category || '',
      tags: parsedTags,
      author_id: article.author_id || null,
      author_name: article.author_name || '',
      body: article.body || '',
      image_url: article.image_url || '',
      published: article.published || false
    })
    setCardImageFile(null)
    setCardImageRemoved(false)
    setShowEditor(true)
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSelectChange = (name, value) => {
    if (name === 'author_id') {
      const selectedAuthor = cosmetologists.find(c => c.id === value)
      setForm(prev => ({ ...prev, author_id: value, author_name: selectedAuthor ? `${selectedAuthor.nickname} (${selectedAuthor.name})` : '' }))
    } else {
      setForm(prev => ({ ...prev, [name]: value }))
    }
  }

  const handleRichTextChange = (html) => {
    setForm(prev => ({ ...prev, body: html }))
  }

  const handleImageUpload = async (file) => {
    if (!file) return
    if (file.size > 10 * 1024 * 1024) return

    setUploadingImage(true)
    try {
      const previewUrl = URL.createObjectURL(file)
      setCardImageFile(file)
      setCardImageRemoved(false)
      setForm(prev => ({ ...prev, image_url: previewUrl }))
    } catch (err) {
      console.error('Error uploading image:', err)
    } finally {
      setUploadingImage(false)
    }
  }

  const handleSave = async () => {
    if (!form.title) return

    const tags = form.tags.split(',').map(t => t.trim()).filter(Boolean)
    
    const hasNewImage = Boolean(cardImageFile)
    const existingImageUrl = !hasNewImage && !cardImageRemoved ? (form.image_url || null) : null
    
    const articleData = {
      title: form.title,
      category: form.category,
      tags,
      author_id: form.author_id,
      author_name: form.author_name,
      body: form.body || '',
      image_url: existingImageUrl,
      published: form.published
    }

    try {
      let articleId
      if (editingArticle) {
        const updated = await contentApi.update(editingArticle.id, articleData)
        articleId = editingArticle.id
        success('Статья обновлена')
      } else {
        const created = await contentApi.create(articleData)
        articleId = created.id
        success('Статья создана')
      }
      
      // Upload new image if we have one
      if (cardImageRemoved && editingArticle && !hasNewImage) {
        await contentApi.deleteCardImage(articleId).catch(() => null)
      }

      if (hasNewImage) {
        try {
          const imgResult = await contentApi.uploadCardImage(articleId, cardImageFile)
          // Update article with new image URL
          await contentApi.update(articleId, { ...articleData, image_url: imgResult.url })
        } catch (imgErr) {
          console.error('Error uploading card image:', imgErr)
        }
      }
      
      // Reload articles to get updated data
      await loadData()
      setShowEditor(false)
      setEditingArticle(null)
      setCardImageFile(null)
      setCardImageRemoved(false)
    } catch (err) {
      console.error('Error saving article:', err)
      error('Ошибка сохранения: ' + err.message)
    }
  }

  const handleDelete = async () => {
    if (deleteModal) {
      try {
        await contentApi.delete(deleteModal.id)
        setArticles(prev => prev.filter(a => a.id !== deleteModal.id))
        success('Статья удалена')
      } catch (err) {
        console.error('Error deleting article:', err)
        error('Ошибка удаления')
      }
      setDeleteModal(null)
    }
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
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
      {!showEditor ? (
        <>
          <div className="page-header">
            <div>
              <h2>База знаний</h2>
              <p>Управление статьями и контентом</p>
            </div>
            {canCreate && (
              <button className="btn btn-primary" onClick={openAddModal}><Plus size={18} />Добавить статью</button>
            )}
          </div>

          <div className="filters-bar glass-card">
            <div className="search-wrapper">
              <Search className="search-icon" />
              <input type="text" placeholder="Поиск статей..." value={search} onChange={e => setSearch(e.target.value)} className="search-input" />
            </div>
          </div>

          <div className="articles-list">
            {filtered.map(article => (
              <div key={article.id} className="article-card glass-card clickable" onClick={() => openEditModal(article)}>
                {article.image_url && (
                  <div className="article-thumbnail">
                    <img src={article.image_url.startsWith('/api/') ? `http://localhost:3002${article.image_url}` : article.image_url} alt="" />
                  </div>
                )}
                <div className="article-content">
                  <h4>{article.title}</h4>
                  <div className="article-meta">
                    {article.category && <span className="meta-tag"><Tag size={14} />{article.category}</span>}
                    {article.author_name && <span className="meta-tag"><User size={14} />{article.author_name}</span>}
                    <span className="meta-tag"><Calendar size={14} />{formatDate(article.created_at)}</span>
                  </div>
                  {article.tags && (() => {
                    let tagsArray = []
                    try {
                      if (Array.isArray(article.tags)) {
                        tagsArray = article.tags
                      } else if (typeof article.tags === 'string') {
                        tagsArray = JSON.parse(article.tags)
                      }
                    } catch {
                      tagsArray = article.tags.split(',').map(t => t.trim()).filter(Boolean)
                    }
                    return (
                      <div className="article-tags">
                        {tagsArray.map((tag, i) => (
                          <span key={i} className="tag">{tag}</span>
                        ))}
                      </div>
                    )
                  })()}
                </div>
                {(canEdit || user?.role === 'admin') && (
                  <div className="article-actions">
                    <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); openEditModal(article) }}><Edit2 size={16} /></button>
                    <button className="btn btn-ghost btn-sm btn-danger" onClick={(e) => { e.stopPropagation(); setDeleteModal(article) }}><Trash2 size={16} /></button>
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
        </>
      ) : (
        <>
          <div className="page-header">
            <button className="btn btn-ghost" onClick={() => setShowEditor(false)}>
              <ChevronLeft size={18} />Назад к списку
            </button>
          </div>

          <div className="content-editor glass-card">
            <div className="editor-header">
              <h3>{editingArticle ? 'Редактирование статьи' : 'Новая статья'}</h3>
            </div>

            <div className="editor-form">
              <div className="form-group full-width">
                <label>Заголовок *</label>
                <input className="input" name="title" value={form.title} onChange={handleInputChange} placeholder="Название статьи" required />
              </div>

              <div className="form-row">
                <Select label="Категория" name="category" value={form.category} onChange={handleSelectChange} options={contentCategories} placeholder="Выберите категорию" />
                
                {isCosmetologist ? (
                  <div className="form-group">
                    <label>Автор</label>
                    <input className="input" value={form.author_name || `${user.nickname || user.name} (${user.name})`} disabled />
                  </div>
                ) : (
                  <Select 
                    label="Автор" 
                    name="author_id" 
                    value={form.author_id} 
                    onChange={handleSelectChange} 
                    options={[
                      ...cosmetologists.map(c => ({ value: c.id, label: `${c.nickname} (${c.name})` })),
                      ...(form.author_id && !cosmetologists.find(c => c.id === form.author_id) 
                        ? [{ value: form.author_id, label: form.author_name || 'Неизвестный автор' }] 
                        : [])
                    ]} 
                    placeholder="Выберите автора" 
                  />
                )}
              </div>

              <div className="form-group full-width">
                <label>Теги (через запятую)</label>
                <input className="input" name="tags" value={form.tags} onChange={handleInputChange} placeholder="увлажнение, сухая кожа, уход" />
              </div>

              <div className="form-group full-width">
                <label>Изображение для карточки</label>
                <div className="media-upload-section">
                  {form.image_url ? (
                    <div className="image-preview-container">
                      <img src={form.image_url.startsWith('/api/') ? `http://localhost:3002${form.image_url}` : form.image_url} alt="" className="image-preview" />
                      <button
                        type="button"
                        className="photo-delete-btn"
                        onClick={() => {
                          setForm(prev => ({ ...prev, image_url: '' }))
                          setCardImageFile(null)
                          setCardImageRemoved(true)
                        }}
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ) : (
                    <label className={`upload-zone ${uploadingImage ? 'uploading' : ''}`}>
                      {uploadingImage ? (
                        <div className="upload-content">
                          <span className="spinner" style={{ width: 24, height: 24 }}></span>
                          <span className="upload-text">Загрузка...</span>
                        </div>
                      ) : (
                        <>
                          <input 
                            type="file" 
                            accept="image/*" 
                            onChange={e => e.target.files[0] && handleImageUpload(e.target.files[0])} 
                            className="hidden-input"
                          />
                          <div className="upload-content">
                            <ImageIcon size={24} className="upload-icon" />
                            <span className="upload-text">Добавить изображение</span>
                            <span className="upload-hint">PNG, JPG, WebP до 10MB</span>
                          </div>
                        </>
                      )}
                    </label>
                  )}
                </div>
              </div>

              <div className="form-group full-width">
                <label>Содержание</label>
                <AdvancedRichTextEditor
                  value={form.body}
                  onChange={handleRichTextChange}
                  placeholder="Напишите содержание статьи..."
                  rows={12}
                  contentId={editingArticle?.id}
                />
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input type="checkbox" name="published" checked={form.published} onChange={handleInputChange} />
                  Опубликовано
                </label>
              </div>
            </div>

            <div className="editor-footer">
              <button className="btn btn-ghost" onClick={() => setShowEditor(false)}>Отмена</button>
              <button className="btn btn-primary" onClick={handleSave}>Сохранить</button>
            </div>
          </div>
        </>
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
