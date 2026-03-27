import { useState, useRef, useEffect } from 'react'
import { ChevronDown, Check } from 'lucide-react'
import './Select.css'

export default function Select({ label, name, value, onChange, options, required, placeholder }) {
  const [isOpen, setIsOpen] = useState(false)
  const [search, setSearch] = useState('')
  const wrapperRef = useRef(null)
  const inputRef = useRef(null)

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
    onChange(name, option)
    setIsOpen(false)
    setSearch('')
  }

  return (
    <div className="form-group">
      {label && <label>{label}{required && <span className="required">*</span>}</label>}
      <div className="custom-select-wrapper" ref={wrapperRef}>
        <button
          type="button"
          className={`custom-select-trigger ${isOpen ? 'open' : ''} ${value ? 'has-value' : ''}`}
          onClick={() => setIsOpen(!isOpen)}
        >
          <span className="select-value">{value || placeholder || 'Выберите...'}</span>
          <ChevronDown size={18} className="select-arrow" />
        </button>
        
        {isOpen && (
          <div className="custom-select-dropdown">
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
            <div className="select-options">
              {filteredOptions.length === 0 ? (
                <div className="select-no-results">Ничего не найдено</div>
              ) : (
                filteredOptions.map((option, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className={`select-option ${option === value ? 'selected' : ''}`}
                    onClick={() => handleSelect(option)}
                  >
                    <span>{option}</span>
                    {option === value && <Check size={16} className="option-check" />}
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
