import { useRef, useEffect, useState } from 'react'
import { Bold, Italic, List, ListOrdered, Heading2, Table, Image, Link, Undo, Redo, X } from 'lucide-react'
import './AdvancedRichTextEditor.css'

export default function AdvancedRichTextEditor({ value, onChange, placeholder, rows = 8, contentId }) {
  const editorRef = useRef(null)
  const fileInputRef = useRef(null)
  const [showTableDialog, setShowTableDialog] = useState(false)
  const [tableRows, setTableRows] = useState(3)
  const [tableCols, setTableCols] = useState(3)

  useEffect(() => {
    if (editorRef.current && editorRef.current.innerHTML !== value) {
      editorRef.current.innerHTML = value || ''
      attachImageDeleteHandlers()
    }
  }, [value])

  const handleInput = () => {
    if (editorRef.current && onChange) {
      onChange(editorRef.current.innerHTML)
    }
  }

  const attachImageDeleteHandlers = () => {
    if (!editorRef.current) return
    const images = editorRef.current.querySelectorAll('img')
    images.forEach(img => {
      if (!img.parentElement.classList.contains('image-container')) {
        const container = document.createElement('div')
        container.className = 'image-container'
        container.style.position = 'relative'
        container.style.display = 'inline-block'
        
        const deleteBtn = document.createElement('button')
        deleteBtn.type = 'button'
        deleteBtn.className = 'image-delete-btn'
        deleteBtn.innerHTML = '×'
        deleteBtn.onclick = (e) => {
          e.preventDefault()
          e.stopPropagation()
          container.remove()
          handleInput()
        }
        
        img.parentNode.insertBefore(container, img)
        container.appendChild(img)
        container.appendChild(deleteBtn)
      }
    })
  }

  const execCommand = (command, value = null) => {
    document.execCommand(command, false, value)
    editorRef.current?.focus()
    setTimeout(attachImageDeleteHandlers, 100)
  }

  const insertTable = () => {
    let tableHTML = '<table style="width:100%;border-collapse:collapse;margin:16px 0;">'
    for (let i = 0; i < tableRows; i++) {
      tableHTML += '<tr>'
      for (let j = 0; j < tableCols; j++) {
        if (i === 0) {
          tableHTML += '<th style="border:1px solid #e2e8f0;padding:8px 12px;background:#f8fafc;font-weight:600;">Заголовок</th>'
        } else {
          tableHTML += '<td style="border:1px solid #e2e8f0;padding:8px 12px;">Ячейка</td>'
        }
      }
      tableHTML += '</tr>'
    }
    tableHTML += '</table><p></p>'
    
    editorRef.current?.focus()
    document.execCommand('insertHTML', false, tableHTML)
    setShowTableDialog(false)
    handleInput()
  }

  const insertImage = (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      const base64 = reader.result
      const imgHTML = `<div class="image-container" style="position:relative;display:inline-block;"><img src="${base64}" style="max-width:100%;height:auto;margin:16px 0;border-radius:8px;" /><button type="button" class="image-delete-btn" onclick="event.preventDefault();event.stopPropagation();this.parentElement.remove();">×</button></div><p></p>`
      editorRef.current?.focus()
      document.execCommand('insertHTML', false, imgHTML)
      handleInput()
    }
    reader.readAsDataURL(file)
    e.target.value = ''
  }

  const insertLink = () => {
    const url = prompt('Введите URL ссылки:')
    if (url) {
      execCommand('createLink', url)
    }
  }

  return (
    <div className="advanced-rich-text-editor">
      <div className="rich-text-toolbar">
        <div className="toolbar-group">
          <button type="button" onClick={() => execCommand('undo')} title="Отменить" className="toolbar-btn">
            <Undo size={16} />
          </button>
          <button type="button" onClick={() => execCommand('redo')} title="Повторить" className="toolbar-btn">
            <Redo size={16} />
          </button>
        </div>
        <div className="toolbar-divider" />
        <div className="toolbar-group">
          <button type="button" onClick={() => execCommand('bold')} title="Жирный" className="toolbar-btn">
            <Bold size={16} />
          </button>
          <button type="button" onClick={() => execCommand('italic')} title="Курсив" className="toolbar-btn">
            <Italic size={16} />
          </button>
          <button type="button" onClick={() => execCommand('formatBlock', 'h2')} title="Заголовок" className="toolbar-btn">
            <Heading2 size={16} />
          </button>
        </div>
        <div className="toolbar-divider" />
        <div className="toolbar-group">
          <button type="button" onClick={() => execCommand('insertUnorderedList')} title="Маркированный список" className="toolbar-btn">
            <List size={16} />
          </button>
          <button type="button" onClick={() => execCommand('insertOrderedList')} title="Нумерованный список" className="toolbar-btn">
            <ListOrdered size={16} />
          </button>
        </div>
        <div className="toolbar-divider" />
        <div className="toolbar-group">
          <button type="button" onClick={() => setShowTableDialog(true)} title="Вставить таблицу" className="toolbar-btn">
            <Table size={16} />
          </button>
          <button type="button" onClick={() => fileInputRef.current?.click()} title="Вставить изображение" className="toolbar-btn">
            <Image size={16} />
          </button>
          <button type="button" onClick={insertLink} title="Вставить ссылку" className="toolbar-btn">
            <Link size={16} />
          </button>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={insertImage}
          style={{ display: 'none' }}
        />
      </div>
      
      <div
        ref={editorRef}
        className="rich-text-content"
        contentEditable
        onInput={handleInput}
        data-placeholder={placeholder}
        style={{ minHeight: `${rows * 1.5}rem` }}
      />

      {showTableDialog && (
        <div className="table-dialog-overlay" onClick={() => setShowTableDialog(false)}>
          <div className="table-dialog" onClick={e => e.stopPropagation()}>
            <h4>Вставить таблицу</h4>
            <div className="table-dialog-inputs">
              <label>
                Строки:
                <input type="number" min="1" max="20" value={tableRows} onChange={e => setTableRows(parseInt(e.target.value) || 1)} />
              </label>
              <label>
                Столбцы:
                <input type="number" min="1" max="10" value={tableCols} onChange={e => setTableCols(parseInt(e.target.value) || 1)} />
              </label>
            </div>
            <div className="table-dialog-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setShowTableDialog(false)}>Отмена</button>
              <button type="button" className="btn btn-primary" onClick={insertTable}>Вставить</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
