import { useState, useEffect } from 'react'
import { X, Upload } from 'lucide-react'

export default function VideoPlayer({ productId, onDelete, onUpload }) {
  const [videoSrc, setVideoSrc] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let blobUrl = null
    
    const fetchVideo = async () => {
      setLoading(true)
      setError(false)
      setVideoSrc(null)
      
      try {
        const response = await fetch(`http://localhost:3001/api/products/${productId}/video`)
        if (!response.ok) throw new Error('Video not found')
        
        const arrayBuffer = await response.arrayBuffer()
        const blob = new Blob([arrayBuffer], { type: 'video/mp4' })
        blobUrl = URL.createObjectURL(blob)
        setVideoSrc(blobUrl)
      } catch (err) {
        console.error('Video fetch error:', err)
        setError(true)
      } finally {
        setLoading(false)
      }
    }
    
    fetchVideo()
    
    return () => {
      if (blobUrl) URL.revokeObjectURL(blobUrl)
    }
  }, [productId])

  if (loading) {
    return (
      <div className="video-card">
        <div style={{ width: '100%', height: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f3f4f6' }}>
          <span className="spinner" style={{ width: 24, height: 24 }}></span>
        </div>
        <button type="button" className="video-delete-btn" onClick={onDelete}>
          <X size={14} />
        </button>
      </div>
    )
  }

  if (error) {
    return (
      <div className="video-card">
        <div style={{ width: '100%', height: '200px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#f9fafb', gap: 8 }}>
          <span style={{ fontSize: 13, color: '#9ca3af' }}>Видео не найдено</span>
        </div>
        <button type="button" className="video-delete-btn" onClick={onDelete}>
          <X size={14} />
        </button>
      </div>
    )
  }

  return (
    <div className="video-card">
      <video
        src={videoSrc}
        controls
        className="video-preview"
        style={{ width: '100%', maxHeight: '200px' }}
        playsInline
        onError={() => setError(true)}
      />
      <button type="button" className="video-delete-btn" onClick={onDelete}>
        <X size={14} />
      </button>
    </div>
  )
}
