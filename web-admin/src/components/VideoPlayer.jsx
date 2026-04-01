import { useState, useEffect } from 'react'
import { X } from 'lucide-react'

export default function VideoPlayer({ productId, onDelete }) {
  const [videoSrc, setVideoSrc] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let objectUrl = null
    
    const fetchVideo = async () => {
      setLoading(true)
      setError(false)
      setVideoSrc(null)
      
      try {
        const url = `http://localhost:3001/api/products/${productId}/video`
        console.log('Fetching video from:', url)
        const response = await fetch(url)
        console.log('Video response status:', response.status)
        
        if (!response.ok) {
          throw new Error(`Video not found: ${response.status}`)
        }
        
        const data = await response.json()
        console.log('Video data received:', data)
        
        if (!data || !data.data) {
          throw new Error('No video data')
        }
        
        // Convert base64 to blob
        const byteCharacters = atob(data.data)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], { type: 'video/mp4' })
        objectUrl = URL.createObjectURL(blob)
        setVideoSrc(objectUrl)
      } catch (err) {
        console.error('Video fetch error:', err)
        setError(true)
      } finally {
        setLoading(false)
      }
    }
    
    fetchVideo()
    
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [productId])

  if (loading) {
    return (
      <div className="video-card">
        <div style={{ width: '100%', height: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f3f4f6' }}>
          <span className="spinner" style={{ width: 24, height: 24 }}></span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="video-card">
        <div style={{ width: '100%', height: '200px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#f9fafb', gap: 8 }}>
          <span style={{ fontSize: 13, color: '#9ca3af' }}>Видео не найдено</span>
        </div>
        {onDelete && (
          <button type="button" className="video-delete-btn" onClick={onDelete}>
            <X size={14} />
          </button>
        )}
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
      {onDelete && (
        <button type="button" className="video-delete-btn" onClick={onDelete}>
          <X size={14} />
        </button>
      )}
    </div>
  )
}
