import { useState, useRef, useEffect } from 'react'
import { ChevronDown, Check, X } from 'lucide-react'
import './Select.css'

export default function Select({ label, name, value, onChange, options, required, placeholder, multiple = false, searchable = false }) {
  const [isOpen, setIsOpen] = useState(false)
  const [search, setSearch] = useState('')
  const wrapperRef = useRef(null)
  const inputRef = useRef(null)

  const values = multiple ? (value || []) : [value].filter(Boolean)

  const filteredOptions = options.filter(opt => 
    opt.toLowerCase().includes(search.toLowerCase())
  )

  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setIsOpen(false)
        setSearch('')
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isOpen])

  const handleSelect = (option) => {
    if (multiple) {
      const newValues = values.includes(option)
        ? values.filter(v => v !== option)
        : [...values, option]
      onChange(name, newValues)
    } else {
      onChange(name, option)
      setIsOpen(false)
      setSearch('')
    }
  }

  const handleRemove = (option, e) => {
    e.stopPropagation()
    const newValues = values.filter(v => v !== option)
    onChange(name, newValues)
  }

  const displayValue = () => {
    if (multiple) {
      if (!values.length) return placeholder || 'Выберите...'
      if (values.length === 1) return values[0]
      return `${values.length} выбрано`
    }
    return value || placeholder || 'Выберите...'
  }

  return (
    <div className="form-group">
      {label && <label>{label}{required && <span className="required">*</span>}</label>}
      <div className="custom-select-wrapper" ref={wrapperRef}>
        <button
          type="button"
          className={`custom-select-trigger ${isOpen ? 'open' : ''} ${value || (multiple && values.length) ? 'has-value' : ''}`}
          onClick={() => setIsOpen(!isOpen)}
        >
          {multiple && values.length > 0 ? (
            <div className="multiple-values">
              {values.slice(0, 2).map(v => (
                <span key={v} className="multi-tag">
                  {v}
                  <X size={12} onClick={(e) => handleRemove(v, e)} />
                </span>
              ))}
              {values.length > 2 && <span className="multi-tag-more">+{values.length - 2}</span>}
            </div>
          ) : (
            <span className="select-value">{displayValue()}</span>
          )}
          <ChevronDown size={18} className="select-arrow" />
        </button>
        
        {isOpen && (
          <div className="custom-select-dropdown">
            {(multiple || searchable) && (
              <div className="select-search-wrapper">
                <input
                  ref={inputRef}
                  type="text"
                  className="select-search-input"
                  placeholder="Поиск..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
            )}
            <div className="select-options">
              {filteredOptions.length === 0 ? (
                <div className="select-no-results">Ничего не найдено</div>
              ) : (
                filteredOptions.map((option, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className={`select-option ${values.includes(option) ? 'selected' : ''}`}
                    onClick={() => handleSelect(option)}
                  >
                    {multiple && <span className={`checkbox ${values.includes(option) ? 'checked' : ''}`}></span>}
                    <span>{option}</span>
                    {!multiple && values.includes(option) && <Check size={16} className="option-check" />}
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
