import { useRef, useEffect } from 'react'
import { Bold, Italic, List, ListOrdered, Heading2 } from 'lucide-react'
import './RichTextEditor.css'

export default function RichTextEditor({ value, onChange, placeholder, rows = 4 }) {
  const editorRef = useRef(null)

  useEffect(() => {
    if (editorRef.current && editorRef.current.innerHTML !== value) {
      editorRef.current.innerHTML = value || ''
    }
  }, [value])

  const handleInput = () => {
    if (editorRef.current && onChange) {
      onChange(editorRef.current.innerHTML)
    }
  }

  const execCommand = (command, value = null) => {
    document.execCommand(command, false, value)
    editorRef.current?.focus()
  }

  return (
    <div className="rich-text-editor">
      <div className="rich-text-toolbar">
        <button type="button" onClick={() => execCommand('bold')} title="Жирный" className="toolbar-btn">
          <Bold size={16} />
        </button>
        <button type="button" onClick={() => execCommand('italic')} title="Курсив" className="toolbar-btn">
          <Italic size={16} />
        </button>
        <button type="button" onClick={() => execCommand('insertUnorderedList')} title="Маркированный список" className="toolbar-btn">
          <List size={16} />
        </button>
        <button type="button" onClick={() => execCommand('insertOrderedList')} title="Нумерованный список" className="toolbar-btn">
          <ListOrdered size={16} />
        </button>
        <button type="button" onClick={() => execCommand('formatBlock', 'h3')} title="Заголовок" className="toolbar-btn">
          <Heading2 size={16} />
        </button>
      </div>
      <div
        ref={editorRef}
        className="rich-text-content"
        contentEditable
        onInput={handleInput}
        data-placeholder={placeholder}
        style={{ minHeight: `${rows * 1.5}rem` }}
      />
    </div>
  )
}
