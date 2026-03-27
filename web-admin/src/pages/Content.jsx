import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Search, Plus, Edit2, Trash2, BookOpen, Tag, Calendar, User, X } from 'lucide-react'
import './Content.css'

const STORAGE_KEY_ARTICLES = 'aura_articles'
const STORAGE_KEY_USERS = 'aura_users'

const defaultArticles = [
  { id: 1, title: 'Как подобрать увлажняющий крем', category: 'Уход за кожей', tags: ['увлажнение', 'сухая кожа'], author: '@skin_expert', date: '20.03.2026' },
  { id: 2, title: 'Витамин C: польза и применение', category: 'Ингредиенты', tags: ['витамин C', 'антиоксидант'], author: '@beauty_consultant', date: '18.03.2026' },
  { id: 3, title: 'SPF: почему важен каждый день', category: 'Защита', tags: ['spf', 'защита от солнца'], author: '@derma_pro', date: '15.03.2026' },
]

const defaultCosmetologists = [
  { id: 1, name: 'Анна Петрова', nickname: '@skin_expert', role: 'cosmetologist' },
  { id: 2, name: 'Елена Волкова', nickname: '@beauty_consultant', role: 'cosmetologist' },
  { id: 3, name: 'Мария Соколова', nickname: '@derma_pro', role: 'cosmetologist' },
]

const contentCategories = ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни']

function loadArticles() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_ARTICLES)
    return stored ? JSON.parse(stored) : defaultArticles
  } catch {
    return defaultArticles
  }
}

function saveArticles(data) {
  localStorage.setItem(STORAGE_KEY_ARTICLES, JSON.stringify(data))
}

function loadCosmetologists() {
  try {
    const storedUsers = localStorage.getItem(STORAGE_KEY_USERS)
    if (storedUsers) {
      const users = JSON.parse(storedUsers)
      const cosmetologists = users.filter(u => u.role === 'cosmetologist')
      if (cosmetologists.length > 0) {
        return cosmetologists.map(u => ({
          id: u.id,
          name: u.name,
          nickname: u.nickname || `@${u.name.toLowerCase().replace(/\s+/g, '_')}`,
          role: 'cosmetologist'
        }))
      }
    }
    return defaultCosmetologists
  } catch {
    return defaultCosmetologists
  }
}

export default function Content() {
  const { user, hasPermission } = useAuth()
  const [articles, setArticles] = useState(loadArticles)
  const [cosmetologists, setCosmetologists] = useState(loadCosmetologists)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState(null)
  const [editingArticle, setEditingArticle] = useState(null)
  const [form, setForm] = useState({ title: '', category: 'Уход за кожей', tags: '', author: '@skin_expert' })

  const canCreate = hasPermission('content_create')
  const canEdit = hasPermission('content_edit_own')

  useEffect(() => {
    saveArticles(articles)
  }, [articles])

  useEffect(() => {
    setCosmetologists(loadCosmetologists())
  }, [])

  const filtered = articles.filter(a => a.title.toLowerCase().includes(search.toLowerCase()))

  const openAddModal = () => {
    setEditingArticle(null)
    const defaultAuthor = cosmetologists[0]?.nickname || '@skin_expert'
    setForm({ title: '', category: 'Уход за кожей', tags: '', author: defaultAuthor })
    setModalOpen(true)
  }

  const openEditModal = (article) => {
    setEditingArticle(article)
    setForm({
      title: article.title,
      category: article.category,
      tags: article.tags.join(', '),
      author: article.author
    })
    setModalOpen(true)
  }

  const handleSave = () => {
    if (!form.title) return

    const tags = form.tags.split(',').map(t => t.trim()).filter(Boolean)
    const today = new Date().toLocaleDateString('ru-RU')

    if (editingArticle) {
      setArticles(prev => prev.map(a => 
        a.id === editingArticle.id 
          ? { ...a, title: form.title, category: form.category, tags, author: form.author }
          : a
      ))
    } else {
      const newArticle = {
        id: Date.now(),
        title: form.title,
        category: form.category,
        tags,
        author: form.author,
        date: today
      }
      setArticles(prev => [newArticle, ...prev])
    }
    setModalOpen(false)
  }

  const handleDelete = () => {
    if (deleteModal) {
      setArticles(prev => prev.filter(a => a.id !== deleteModal.id))
      setDeleteModal(null)
    }
  }

  return (
    <div className="content-page">
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
            <div className="article-icon"><BookOpen size={24} /></div>
            <div className="article-content">
              <h4>{article.title}</h4>
              <div className="article-meta">
                <span className="meta-tag"><Tag size={14} />{article.category}</span>
                <span className="meta-tag"><User size={14} />{article.author}</span>
                <span className="meta-tag"><Calendar size={14} />{article.date}</span>
              </div>
              <div className="article-tags">
                {article.tags.map(tag => (
                  <span key={tag} className="tag">{tag}</span>
                ))}
              </div>
            </div>
            {(canEdit || user?.role === 'admin') && (
              <div className="article-actions">
                <button className="btn btn-ghost btn-sm" onClick={() => openEditModal(article)}><Edit2 size={16} /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => setDeleteModal(article)}><Trash2 size={16} /></button>
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
                <label>Заголовок</label>
                <input className="input" value={form.title} onChange={e => setForm({...form, title: e.target.value})} placeholder="Название статьи" />
              </div>
              <div className="form-group">
                <label>Категория</label>
                <select className="input" value={form.category} onChange={e => setForm({...form, category: e.target.value})}>
                  {contentCategories.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Автор</label>
                <select className="input" value={form.author} onChange={e => setForm({...form, author: e.target.value})}>
                  {cosmetologists.map(cos => (
                    <option key={cos.id} value={cos.nickname}>{cos.nickname} ({cos.name})</option>
                  ))}
                </select>
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Теги (через запятую)</label>
                <input className="input" value={form.tags} onChange={e => setForm({...form, tags: e.target.value})} placeholder="увлажнение, сухая кожа" />
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
