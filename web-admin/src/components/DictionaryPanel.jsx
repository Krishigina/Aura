import { useState } from 'react'
import { Plus, Edit2, Trash2, X, Check, ChevronDown, ExternalLink, Info } from 'lucide-react'

function pluralize(n, one, two, five) {
  const absN = Math.abs(n)
  if (absN % 100 >= 11 && absN % 100 <= 19) return five
  const mod10 = absN % 10
  if (mod10 === 1) return one
  if (mod10 >= 2 && mod10 <= 4) return two
  return five
}

export default function DictionaryPanel({ 
  dictionaries, 
  onAdd, 
  onDelete, 
  onUpdate, 
  canEdit = true,
  config = {},
  onFullPage,
  onEditBrand
}) {
  const [expandedDict, setExpandedDict] = useState(null)
  const [newValue, setNewValue] = useState('')
  const [editValue, setEditValue] = useState('')
  const [editingValue, setEditingValue] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)

  const toggleDict = (key) => setExpandedDict(expandedDict === key ? null : key)

  const handleAdd = (key) => {
    if (!newValue.trim()) return
    const values = dictionaries[key] || []
    const duplicates = values.filter(v => {
      const vValue = typeof v === 'object' ? v.value : v
      return vValue.toLowerCase().includes(newValue.toLowerCase())
    })
    if (duplicates.length > 0) {
      alert(`Найдены похожие значения: ${duplicates.map(d => typeof d === 'object' ? d.value : d).join(', ')}`)
      return
    }
    onAdd(key, newValue.trim())
    setNewValue('')
  }

  const handleDelete = (key, value) => {
    setConfirmDelete({ key, value })
  }

  const confirmDeleteHandler = () => {
    if (confirmDelete) {
      onDelete(confirmDelete.key, confirmDelete.value)
      setConfirmDelete(null)
    }
  }

  const startEdit = (key, value) => {
    setEditingValue({ key, value })
    setEditValue(typeof value === 'object' ? value.value : value)
  }

  const saveEdit = (key, oldValue) => {
    if (!editValue.trim()) return
    const values = dictionaries[key] || []
    const duplicates = values.filter(v => {
      const vValue = typeof v === 'object' ? v.value : v
      return vValue.toLowerCase().includes(editValue.toLowerCase()) && vValue !== oldValue
    })
    if (duplicates.length > 0) {
      alert(`Такое значение уже есть: ${duplicates.map(d => typeof d === 'object' ? d.value : d).join(', ')}`)
      return
    }
    onUpdate(key, oldValue, editValue.trim())
    setEditingValue(null)
    setEditValue('')
  }

  return (
    <>
      <div className="dict-panel glass-card">
        <div className="dict-accordion">
          {Object.entries(dictionaries).map(([key, values]) => {
            const dictConfigItem = config[key] || {}
            const Icon = dictConfigItem.icon
            const isExpanded = expandedDict === key
            const listValues = values || []
            
            return (
              <div key={key} className="dict-item">
                <div className="dict-item-header" onClick={() => toggleDict(key)} style={{ borderColor: isExpanded ? dictConfigItem.color : 'transparent', cursor: 'pointer' }}>
                  <div className="dict-header-left">
                    {Icon && <div className="dict-icon" style={{ background: `${dictConfigItem.color}20`, color: dictConfigItem.color }}><Icon size={18} /></div>}
                    <span className="dict-label">{dictConfigItem.label || key}</span>
                    <span className="dict-count">{listValues.length}</span>
                  </div>
                  <div className="dict-header-right">
                    {listValues.length > 0 && onFullPage && (
                      <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); onFullPage(key) }}>
                        <ExternalLink size={14} />Открыть все
                      </button>
                    )}
                    <ChevronDown size={18} className={`dict-chevron ${isExpanded ? 'rotated' : ''}`} />
                  </div>
                </div>
                {isExpanded && (
                  <div className="dict-item-content">
                    <div className="dict-values-list">
                      {listValues.filter(v => !newValue || (typeof v === 'string' ? v.toLowerCase().includes(newValue.toLowerCase()) : v.value?.toLowerCase().includes(newValue.toLowerCase()))).map((item, idx) => {
                        const value = typeof item === 'string' ? item : item.value
                        const description = typeof item === 'string' ? null : item.description
                        return (
                        <div key={idx} className="dict-value-row">
                          {editingValue?.key === key && editingValue?.value === value ? (
                            <div className="dict-edit-row">
                              <input className="input input-sm" value={editValue} onChange={e => setEditValue(e.target.value)} onKeyDown={e => e.key === 'Enter' && saveEdit(key, value)} autoFocus />
                              <button className="btn btn-sm btn-primary" onClick={() => saveEdit(key, value)}><Check size={14} /></button>
                              <button className="btn btn-sm btn-ghost" onClick={() => { setEditingValue(null); setEditValue('') }}><X size={14} /></button>
                            </div>
                          ) : (
                            <div className="dict-value-container">
                              <span className="dict-value">{value}</span>
                              {key === 'brands' && typeof item === 'object' && item.description && (
                                <span className="dict-value-desc" onClick={() => onEditBrand?.(item)} title="Редактировать описание"><Info size={12} />{item.description.substring(0, 30)}{item.description.length > 30 ? '...' : ''}</span>
                              )}
                              {key === 'brands' && (
                                <button className="btn btn-icon btn-ghost btn-sm" onClick={() => onEditBrand?.(typeof item === 'object' ? item : { value })} title="О бренде"><Info size={14} /></button>
                              )}
                            </div>
                          )}
                          {canEdit && (
                            <div className="dict-value-actions">
                              <button className="btn btn-icon btn-ghost" onClick={() => startEdit(key, value)} title="Редактировать"><Edit2 size={14} /></button>
                              <button className="btn btn-icon btn-ghost btn-danger" onClick={() => handleDelete(key, value)} title="Удалить"><Trash2 size={14} /></button>
                            </div>
                          )}
                        </div>
                      )})}
                    </div>
                    {canEdit && (
                      <div className="dict-add-row">
                        <input 
                          className="input input-sm" 
                          placeholder="Добавить значение..." 
                          value={key === expandedDict ? newValue : ''} 
                          onChange={e => { 
                            setExpandedDict(key)
                            setNewValue(e.target.value)
                          }} 
                          onKeyDown={e => e.key === 'Enter' && handleAdd(key)} 
                        />
                        <button className="btn btn-sm btn-primary" onClick={() => handleAdd(key)} disabled={!newValue.trim()}><Plus size={14} />Добавить</button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {confirmDelete && (
        <div className="modal-overlay" onClick={() => setConfirmDelete(null)}>
          <div className="modal glass-card confirm-modal" onClick={e => e.stopPropagation()}>
            <div className="confirm-icon"><Trash2 size={32} /></div>
            <h3>Подтверждение</h3>
            <p>Вы уверены, что хотите удалить "{confirmDelete.value}"?</p>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setConfirmDelete(null)}>Отмена</button>
              <button className="btn btn-danger" onClick={confirmDeleteHandler}>Удалить</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
