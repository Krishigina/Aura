import { useEffect, useMemo, useState } from 'react'
import { knowledgeApi } from '../api'

function ScopeBadge({ scope }) {
  const color = scope === 'rag' ? '#7c3aed' : scope === 'recommendations' ? '#0ea5e9' : '#16a34a'
  return (
    <span style={{ padding: '4px 10px', borderRadius: 999, background: `${color}22`, color, fontSize: 12, fontWeight: 600 }}>
      {scope}
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
      alert('Документ загружен и добавлен в Global KB')
    } catch (e) {
      alert(e.message || 'Не удалось загрузить документ')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, gap: 12, flexWrap: 'wrap' }}>
        <div>
          <h2 style={{ margin: 0 }}>Источники знаний</h2>
          <p style={{ marginTop: 6, color: '#64748b' }}>Управляйте тем, какие источники влияют на рекомендации и ответы RAG-ассистента.</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select value={filter} onChange={(e) => setFilter(e.target.value)} style={{ padding: '8px 10px', borderRadius: 8 }}>
            <option value="all">Все</option>
            <option value="global">Global KB</option>
            <option value="personal">Personal KB</option>
          </select>
          <select value={uploadScope} onChange={(e) => setUploadScope(e.target.value)} style={{ padding: '8px 10px', borderRadius: 8 }}>
            <option value="both">scope: both</option>
            <option value="rag">scope: rag</option>
            <option value="recommendations">scope: recommendations</option>
          </select>
          <input
            type="number"
            min={0}
            max={10}
            step={0.1}
            value={uploadWeight}
            onChange={(e) => setUploadWeight(e.target.value)}
            style={{ width: 90, padding: '8px 10px', borderRadius: 8, border: '1px solid #d1d5db' }}
          />
          <label style={{ display: 'inline-flex', alignItems: 'center', padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 8, cursor: uploading ? 'not-allowed' : 'pointer', background: uploading ? '#e5e7eb' : '#fff' }}>
            {uploading ? 'Загрузка...' : 'Загрузить PDF в Global KB'}
            <input type="file" accept=".pdf,.txt,.md" onChange={uploadAdminDocument} disabled={uploading} style={{ display: 'none' }} />
          </label>
          <button onClick={runReindex} disabled={reindexing || loading} style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #d1d5db', background: reindexing ? '#e5e7eb' : '#ffffff', cursor: 'pointer' }}>
            {reindexing ? 'Идет переиндексация...' : 'Переиндексировать Global KB'}
          </button>
        </div>
      </div>

      {error && <div style={{ marginBottom: 16, color: '#b91c1c' }}>{error}</div>}

      {loading ? (
        <div>Загрузка...</div>
      ) : (
        <div style={{ overflowX: 'auto', background: '#fff', border: '1px solid #e5e7eb', borderRadius: 12 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Название</th>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Тип</th>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Владелец</th>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Scope</th>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Weight</th>
                <th style={{ padding: 12, borderBottom: '1px solid #e5e7eb' }}>Enabled</th>
              </tr>
            </thead>
            <tbody>
              {visibleItems.map((item) => (
                <tr key={item.id}>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>{item.title}</td>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>{item.source_type}</td>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>{item.owner_user_id == null ? 'global' : `user:${item.owner_user_id}`}</td>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <ScopeBadge scope={item.scope} />
                      <select
                        value={item.scope}
                        disabled={savingId === item.id}
                        onChange={(e) => updateSource(item.id, { scope: e.target.value })}
                        style={{ padding: '6px 8px', borderRadius: 8 }}
                      >
                        <option value="both">both</option>
                        <option value="rag">rag</option>
                        <option value="recommendations">recommendations</option>
                      </select>
                    </div>
                  </td>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>
                    <input
                      type="number"
                      min={0}
                      max={10}
                      step={0.1}
                      value={item.weight}
                      disabled={savingId === item.id}
                      onChange={(e) => patchItem(item.id, { weight: Number(e.target.value || 0) })}
                      onBlur={(e) => updateSource(item.id, { weight: Number(e.target.value || 0) })}
                      style={{ width: 80, padding: '6px 8px', borderRadius: 8, border: '1px solid #d1d5db' }}
                    />
                  </td>
                  <td style={{ padding: 12, borderBottom: '1px solid #f1f5f9' }}>
                    <label style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                      <input
                        type="checkbox"
                        checked={item.enabled}
                        disabled={savingId === item.id}
                        onChange={(e) => updateSource(item.id, { enabled: e.target.checked })}
                      />
                      {item.enabled ? 'on' : 'off'}
                    </label>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
