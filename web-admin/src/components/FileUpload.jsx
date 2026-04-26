import { useState, useRef } from 'react'
import { Upload, X, Image, Video } from 'lucide-react'
import './FileUpload.css'

export default function FileUpload({ 
  type = 'image',
  onUpload, 
  onDelete,
  files = [],
  maxFiles = 5
}) {
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef(null)

  const handleDrop = async (e) => {
    e.preventDefault()
    const file = e.dataTransfer.files[0]
    if (file) await uploadFile(file)
  }

  const handleFileSelect = async (e) => {
    const file = e.target.files[0]
    if (file) await uploadFile(file)
  }

  const uploadFile = async (file) => {
    const isImage = type === 'image'
    if (isImage && !file.type.startsWith('image/')) return
    if (!isImage && file.type !== 'video/mp4') return
    
    if (files.length >= maxFiles) return
    
    setUploading(true)
    try {
      await onUpload(file)
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (fileId) => {
    await onDelete(fileId)
  }

  return (
    <div className="file-upload">
      <div 
        className="file-upload-zone"
        onDrop={handleDrop}
        onDragOver={(e) => e.preventDefault()}
        onClick={() => inputRef.current?.click()}
      >
        <input 
          ref={inputRef}
          type="file"
          accept={type === 'image' ? 'image/*' : 'video/mp4'}
          onChange={handleFileSelect}
          hidden
        />
        {uploading ? (
          <span className="spinner"></span>
        ) : (
          <>
            {type === 'image' ? <Image size={24} /> : <Video size={24} />}
            <span>{type === 'image' ? 'Перетащите фото' : 'Перетащите видео (MP4)'}</span>
          </>
        )}
      </div>
      
      {files.length > 0 && (
        <div className="file-list">
          {files.map((file, idx) => (
            <div key={file.id || idx} className="file-item">
              {type === 'image' ? (
                <img src={`data:${file.content_type};base64,${file.data}`} alt={file.filename} />
              ) : (
                <video src={file.url} />
              )}
              <button type="button" onClick={() => handleDelete(file.id)} className="file-remove">
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
