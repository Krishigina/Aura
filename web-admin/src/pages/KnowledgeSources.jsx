import { useEffect, useMemo, useState } from 'react'
import { knowledgeApi } from '../api'
import Select from '../components/Select'
import './KnowledgeSources.css'

const filterOptions = [
  { value: 'all', label: 'Все источники' },
  { value: 'global', label: 'Общая база' },
  { value: 'personal', label: 'Персональные' }
]

const scopeOptions = [
  { value: 'both', label: 'Ответы и рекомендации' },
  { value: 'rag', label: 'Только ответы' },
  { value: 'recommendations', label: 'Только рекомендации' }
]

const scopeLabels = scopeOptions.reduce((acc, option) => ({ ...acc, [option.value]: option.label }), {})

const typeLabels = {
  pdf: 'PDF',
  txt: 'Текст',
  md: 'Markdown',
  docx: 'Word',
  note: 'Заметка',
  document: 'Документ'
}

function ScopeBadge({ scope }) {
  const className = scope === 'rag' ? 'scope-rag' : scope === 'recommendations' ? 'scope-recommendations' : 'scope-both'
  return (
    <span className={`knowledge-badge ${className}`}>
      {scopeLabels[scope] || scope}
    </span>
  )
}

function TypeBadge({ type }) {
  const label = typeLabels[String(type || '').toLowerCase()] || type || 'Источник'
  return <span className="knowledge-badge source-type-badge">{label}</span>
}

function OwnerBadge({ ownerUserId }) {
  const isGlobal = ownerUserId == null
  return (
    <span className={`knowledge-badge ${isGlobal ? 'owner-global' : 'owner-personal'}`}>
      {isGlobal ? 'Общая' : `Пользователь: ${ownerUserId}`}
    </span>
  )
}

export default function KnowledgeSources() {
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [reindexing, setReindexing] = useState(false)
  const [error, setError] = useState('')
  const [items, setItems] = useState([])
  const [filter, setFilter] = useState('all')
  const [uploading, setUploading] = useState(false)
  const [uploadScope, setUploadScope] = useState('both')
  const [uploadWeight, setUploadWeight] = useState(1)

  const load = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await knowledgeApi.listSources()
      setItems(data.items || [])
    } catch (e) {
      setError(e.message || 'Не удалось загрузить источники знаний')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const visibleItems = useMemo(() => {
    if (filter === 'global') return items.filter((x) => x.owner_user_id == null)
    if (filter === 'personal') return items.filter((x) => x.owner_user_id != null)
    return items
  }, [items, filter])

  const patchItem = (id, patch) => {
    setItems((prev) => prev.map((item) => (item.id === id ? { ...item, ...patch } : item)))
  }

  const handleFilterChange = (_name, value) => setFilter(value)
  const handleUploadScopeChange = (_name, value) => setUploadScope(value)
  const handleRowScopeChange = (id, value) => updateSource(id, { scope: value })

  const updateSource = async (id, payload) => {
    try {
      setSavingId(id)
      const updated = await knowledgeApi.updateSource(id, payload)
      patchItem(id, updated)
    } catch (e) {
      alert(e.message || 'Не удалось обновить источник')
    } finally {
      setSavingId(null)
    }
  }

  const runReindex = async () => {
    try {
      setReindexing(true)
      const result = await knowledgeApi.reindex()
      await load()
      alert(`Переиндексация завершена. Индексировано: ${result.indexed_count}`)
    } catch (e) {
      alert(e.message || 'Ошибка переиндексации')
    } finally {
      setReindexing(false)
    }
  }

  const uploadAdminDocument = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    try {
      setUploading(true)
      await knowledgeApi.uploadAdminDocument(file, {
        scope: uploadScope,
        weight: Number(uploadWeight || 1),
      })
      await load()
      alert('Документ загружен и добавлен в общую базу знаний')
    } catch (e) {
      alert(e.message || 'Не удалось загрузить документ')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="knowledge-sources-page">
      <div className="page-header">
        <div>
          <h2>Источники знаний</h2>
          <p>Управляйте тем, какие источники влияют на рекомендации и ответы RAG-ассистента.</p>
        </div>
      </div>

      <div className="filters-bar glass-card knowledge-controls">
        <div className="knowledge-control-group">
          <Select name="filter" value={filter} onChange={handleFilterChange} options={filterOptions} />
          <Select name="uploadScope" value={uploadScope} onChange={handleUploadScopeChange} options={scopeOptions} />
          <input
            className="input knowledge-weight-input"
            type="number"
            min={0}
            max={10}
            step={0.1}
            value={uploadWeight}
            onChange={(e) => setUploadWeight(e.target.value)}
            aria-label="Вес загружаемого источника"
          />
        </div>
        <div className="knowledge-actions">
          <label className={`btn btn-secondary knowledge-upload-button ${uploading ? 'disabled' : ''}`}>
            {uploading ? 'Загрузка...' : 'Загрузить документ в общую базу'}
            <input type="file" accept=".pdf,.txt,.md,.docx" onChange={uploadAdminDocument} disabled={uploading} />
          </label>
          <button className="btn btn-primary" onClick={runReindex} disabled={reindexing || loading}>
            {reindexing ? 'Идет переиндексация...' : 'Переиндексировать общую базу'}
          </button>
        </div>
      </div>

      {error && <div className="knowledge-error glass-card">{error}</div>}

      {loading ? (
        <div className="loading-state">Загрузка...</div>
      ) : (
        <>
          <div className="table-card glass-card knowledge-table-card">
            <table className="table knowledge-table">
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Тип</th>
                  <th>Владелец</th>
                  <th>Область</th>
                  <th>Вес</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                {visibleItems.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div className="knowledge-source-title">{item.title}</div>
                      <div className="knowledge-source-meta">{item.owner_user_id == null ? 'Документ общей базы' : 'Персональный источник'}</div>
                    </td>
                    <td><TypeBadge type={item.source_type} /></td>
                    <td><OwnerBadge ownerUserId={item.owner_user_id} /></td>
                    <td>
                      <div className="knowledge-inline-control">
                        <ScopeBadge scope={item.scope} />
                        <Select
                          name={`scope-${item.id}`}
                          value={item.scope}
                          disabled={savingId === item.id}
                          onChange={(_name, value) => handleRowScopeChange(item.id, value)}
                          options={scopeOptions}
                        />
                      </div>
                    </td>
                    <td>
                      <input
                        className="input knowledge-weight-input"
                        type="number"
                        min={0}
                        max={10}
                        step={0.1}
                        value={item.weight}
                        disabled={savingId === item.id}
                        onChange={(e) => patchItem(item.id, { weight: Number(e.target.value || 0) })}
                        onBlur={(e) => updateSource(item.id, { weight: Number(e.target.value || 0) })}
                        aria-label={`Вес источника ${item.title}`}
                      />
                    </td>
                    <td>
                      <label className="knowledge-status-toggle">
                        <input
                          type="checkbox"
                          checked={item.enabled}
                          disabled={savingId === item.id}
                          onChange={(e) => updateSource(item.id, { enabled: e.target.checked })}
                        />
                        <span className={`knowledge-status-dot ${item.enabled ? 'enabled' : ''}`} />
                        <span>{item.enabled ? 'Включен' : 'Выключен'}</span>
                      </label>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {visibleItems.length === 0 && <div className="empty-state glass-card"><p>Источники не найдены</p></div>}
        </>
      )}
    </div>
  )
}
