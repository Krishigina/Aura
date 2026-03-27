import { useState } from 'react'
import { Search, Plus, Edit2, Trash2, X, Check, ExternalLink } from 'lucide-react'

export default function DictionaryPanel({ 
  dictionaries, 
  onAdd, 
  onDelete, 
  onUpdate, 
  canEdit = true,
  config = {},
  onFullPage,
  fullPageDict,
  onCloseFullPage,
  fullPageFilter,
  onFullPageFilterChange
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
    const duplicates = values.filter(v => v.toLowerCase().includes(newValue.toLowerCase()))
    if (duplicates.length > 0) {
      alert(`Найдены похожие значения: ${duplicates.join(', ')}`)
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
    setEditValue(value)
  }

  const saveEdit = (key, oldValue) => {
    if (!editValue.trim()) return
    const values = dictionaries[key] || []
    const duplicates = values.filter(v => v.toLowerCase().includes(editValue.toLowerCase()) && v !== oldValue)
    if (duplicates.length > 0) {
      alert(`Такое значение уже есть: ${duplicates.join(', ')}`)
      return
    }
    onUpdate(key, oldValue, editValue.trim())
    setEditingValue(null)
    setEditValue('')
  }

  const filteredFullPageValues = fullPageDict && dictionaries[fullPageDict] 
    ? dictionaries[fullPageDict].filter(v => !fullPageFilter || v.toLowerCase().includes(fullPageFilter.toLowerCase()))
    : []

  return (
    <>
      <div className="dict-panel glass-card">
        <div className="dict-panel-header">
          <h3>Справочники</h3>
          <p>Управление значениями справочников</p>
        </div>
        <div className="dict-accordion">
          {Object.entries(dictionaries).map(([key, values]) => {
            const dictConfigItem = config[key] || {}
            const Icon = dictConfigItem.icon
            const isExpanded = expandedDict === key
            const listValues = values || []
            
            return (
              <div key={key} className="dict-item">
                <button className="dict-item-header" onClick={() => toggleDict(key)} style={{ borderColor: isExpanded ? dictConfigItem.color : 'transparent' }}>
                  <div className="dict-header-left">
                    {Icon && <div className="dict-icon" style={{ background: `${dictConfigItem.color}20`, color: dictConfigItem.color }}><Icon size={18} /></div>}
                    <span className="dict-label">{dictConfigItem.label || key}</span>
                    <span className="dict-count">{listValues.length}</span>
                  </div>
                  <div className="dict-header-right">
                    {listValues.length > 10 && onFullPage && (
                      <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); onFullPage(key) }}>
                        <ExternalLink size={14} />Открыть все
                      </button>
                    )}
                    <Check size={18} className={`dict-chevron ${isExpanded ? 'rotated' : ''}`} />
                  </div>
                </button>
                {isExpanded && (
                  <div className="dict-item-content">
                    <div className="dict-values-list">
                      {listValues.filter(v => !newValue || v.toLowerCase().includes(newValue.toLowerCase())).map((value, idx) => (
                        <div key={idx} className="dict-value-row">
                          {editingValue?.key === key && editingValue?.value === value ? (
                            <div className="dict-edit-row">
                              <input className="input input-sm" value={editValue} onChange={e => setEditValue(e.target.value)} onKeyDown={e => e.key === 'Enter' && saveEdit(key, value)} autoFocus />
                              <button className="btn btn-sm btn-primary" onClick={() => saveEdit(key, value)}><Check size={14} /></button>
                              <button className="btn btn-sm btn-ghost" onClick={() => { setEditingValue(null); setEditValue('') }}><X size={14} /></button>
                            </div>
                          ) : (
                            <span className="dict-value">{value}</span>
                          )}
                          {canEdit && (
                            <div className="dict-value-actions">
                              <button className="btn btn-icon btn-ghost" onClick={() => startEdit(key, value)} title="Редактировать"><Edit2 size={14} /></button>
                              <button className="btn btn-icon btn-ghost btn-danger" onClick={() => handleDelete(key, value)} title="Удалить"><Trash2 size={14} /></button>
                            </div>
                          )}
                        </div>
                      ))}
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

      {fullPageDict && (
        <div className="modal-overlay fullpage-dict-overlay" onClick={onCloseFullPage}>
          <div className="modal glass-card fullpage-dict" onClick={e => e.stopPropagation()}>
            <div className="fullpage-dict-header">
              <div className="fullpage-dict-icon" style={{ background: `${config[fullPageDict]?.color}20`, color: config[fullPageDict]?.color }}>
                {(() => {
                  const Icon = config[fullPageDict]?.icon
                  return Icon ? <Icon size={24} /> : null
                })()}
              </div>
              <div className="fullpage-dict-title-group">
                <h3 className="fullpage-dict-title">{config[fullPageDict]?.label || fullPageDict}</h3>
                <p className="fullpage-dict-subtitle">Управление значениями справочника</p>
              </div>
              <button className="btn btn-ghost btn-sm" onClick={onCloseFullPage}><X size={20} /></button>
            </div>
            <div className="fullpage-dict-content">
              <div className="fullpage-search">
                <Search className="search-icon" />
                <input 
                  type="text" 
                  placeholder="Поиск..." 
                  className="search-input" 
                  value={fullPageFilter}
                  onChange={e => onFullPageFilterChange(e.target.value)}
                  autoFocus
                />
              </div>
              <div className="fullpage-list">
                {filteredFullPageValues.length === 0 ? (
                  <div className="fullpage-empty">Ничего не найдено</div>
                ) : (
                  filteredFullPageValues.map((value, idx) => (
                    <div key={idx} className="fullpage-item">
                      <span>{value}</span>
                      {canEdit && (
                        <div className="fullpage-actions">
                          <button className="btn btn-ghost btn-sm" onClick={() => { onCloseFullPage(); startEdit(fullPageDict, value) }}><Edit2 size={14} /></button>
                          <button className="btn btn-ghost btn-sm btn-danger" onClick={() => { onCloseFullPage(); handleDelete(fullPageDict, value) }}><Trash2 size={14} /></button>
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
            <div className="fullpage-footer">
              <span className="fullpage-count">{dictionaries[fullPageDict]?.length || 0} значений</span>
              <button className="btn btn-primary" onClick={onCloseFullPage}>Закрыть</button>
            </div>
          </div>
        </div>
      )}

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
