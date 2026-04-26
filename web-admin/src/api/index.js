const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3002/api'

// Get token from localStorage (set by AuthContext)
function getToken() {
  return localStorage.getItem('aura_token')
}

async function request(endpoint, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Request failed', detail: response.statusText }))
    let message = error.detail || error.error || 'Request failed'
    if (Array.isArray(message)) {
      message = message
        .map((item) => {
          if (typeof item === 'string') return item
          if (!item || typeof item !== 'object') return ''
          const field = Array.isArray(item.loc) ? item.loc.join('.') : ''
          const text = item.msg || item.message || ''
          return [field, text].filter(Boolean).join(': ')
        })
        .filter(Boolean)
        .join('; ')
    }
    if (typeof message !== 'string') {
      message = JSON.stringify(message)
    }
    throw new Error(message || 'Request failed')
  }
  return response.json()
}

export const productsApi = {
  getAll: () => request('/products'),
  getById: (id) => request(`/products/${id}`),
  create: (data) => request('/products', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/products/${id}`, { method: 'DELETE' }),
  
  uploadPhoto: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = { 'Accept': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/photos`, {
      method: 'POST',
      headers,
      body: formData
    })
    if (!response.ok) {
      throw new Error('Upload failed')
    }
    return response.json()
  },
  
  deletePhoto: async (productId, photoId) => {
    const token = getToken()
    const headers = { 'Accept': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/photos/${photoId}`, {
      method: 'DELETE',
      headers
    })
    if (!response.ok) {
      throw new Error('Delete failed')
    }
    return response.json()
  },

  getPhotos: async (productId) => {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/photos`, { headers })
    if (!response.ok) return []
    return response.json()
  },

  getVideo: async (productId) => {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/video`, { headers })
    if (!response.ok) return null
    return response.json()
  },

  uploadVideo: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/video`, {
      method: 'POST',
      headers,
      body: formData
    })
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Upload failed')
    }
    return response.json()
  },
  
  deleteVideo: async (productId) => {
    const token = getToken()
    const headers = { 'Accept': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/${productId}/video`, {
      method: 'DELETE',
      headers
    })
    if (!response.ok) {
      throw new Error('Delete failed')
    }
    return response.json()
  },
  
  getVideoUrl: (productId) => `${API_URL}/products/${productId}/video`,
  getPhotoUrl: (productId, photoId) => `${API_URL}/products/${productId}/photos/${photoId}`,
  parseUrl: (url) => request('/products/parse', { method: 'POST', body: JSON.stringify({ url }) }),
  
  export: () => {
    window.open(`${API_URL}/products/export`, '_blank')
  },
  
  import: async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/products/import`, {
      method: 'POST',
      headers,
      body: formData
    })
    if (!response.ok) {
      const error = await response.json().catch(() => ({ detail: 'Import failed' }))
      throw new Error(error.detail || 'Import failed')
    }
    return response.json()
  },
}

export const dictionariesApi = {
  get: (key) => request(`/dictionaries/${key}`),
  create: (key, value) => request(`/dictionaries/${key}`, { method: 'POST', body: JSON.stringify({ value }) }),
  update: (key, oldValue, newValue) => request(`/dictionaries/${key}`, { method: 'PUT', body: JSON.stringify({ oldValue, newValue }) }),
  delete: (key, value) => request(`/dictionaries/${key}/${encodeURIComponent(value)}`, { method: 'DELETE' }),
  updateBrand: (brand) => request('/dictionaries/brands', { method: 'PUT', body: JSON.stringify(brand) }),
}

export const proceduresApi = {
  getAll: () => request('/procedures'),
  getById: (id) => request(`/procedures/${id}`),
  create: (data) => request('/procedures', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/procedures/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/procedures/${id}`, { method: 'DELETE' }),
  
  getMethodTypes: () => request('/procedures/dictionaries/method-types'),
  getDurations: () => request('/procedures/dictionaries/durations'),
  getEquipment: () => request('/procedures/dictionaries/equipment'),
  getZones: () => request('/procedures/dictionaries/zones'),
  getEffects: () => request('/procedures/dictionaries/effects'),
  getProblems: () => request('/procedures/dictionaries/problems'),
  
  addDictionaryValue: async (dictType, value) => {
    return request(`/procedures/dictionaries/${dictType}?value=${encodeURIComponent(value)}`, { 
      method: 'POST'
    })
  },
  
  deleteDictionaryValue: async (dictType, value) => {
    return request(`/procedures/dictionaries/${dictType}/${encodeURIComponent(value)}`, { 
      method: 'DELETE'
    })
  },
  
  uploadPhoto: async (procedureId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/procedures/${procedureId}/photos`, {
      method: 'POST',
      headers,
      body: formData
    })
    if (!response.ok) throw new Error('Upload failed')
    return response.json()
  },
  
  deletePhoto: async (procedureId, photoId) => {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/procedures/${procedureId}/photos/${photoId}`, {
      method: 'DELETE',
      headers
    })
    if (!response.ok) throw new Error('Delete failed')
    return response.json()
  },

  getPhotos: async (procedureId) => {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/procedures/${procedureId}/photos`, { headers })
    if (!response.ok) return []
    return response.json()
  },
  
  getPhotoUrl: (procedureId, photoId) => `${API_URL}/procedures/${procedureId}/photos/${photoId}`,
}

export const contentApi = {
  getAll: () => request('/content'),
  create: (data) => request('/content', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/content/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/content/${id}`, { method: 'DELETE' }),
  uploadCardImage: async (contentId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${API_URL}/content/${contentId}/card-image`, {
      method: 'POST',
      headers,
      body: formData
    })
    if (!response.ok) throw new Error('Upload failed')
    return response.json()
  },
  deleteCardImage: (contentId) => request(`/content/${contentId}/card-image`, { method: 'DELETE' }),
}

export const usersApi = {
  getAll: () => request('/users?role=all'),
  getCosmetologists: () => request('/users?role=cosmetologist'),
  create: (data) => request('/users', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/users/${id}`, { method: 'DELETE' }),
}

export const analyticsApi = {
  getDashboard: () => request('/analytics/dashboard'),
}

export const reportsApi = {
  getSummary: () => request('/reports/summary'),
}

export const knowledgeApi = {
  listSources: () => request('/assistant/knowledge/sources'),
  updateSource: (id, data) => request(`/assistant/knowledge/sources/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  reindex: () => request('/assistant/knowledge/reindex', { method: 'POST' }),

  uploadAdminDocument: async (file, { scope = 'both', weight = 1.0 } = {}) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    const query = new URLSearchParams({ scope, weight: String(weight) })
    const response = await fetch(`${API_URL}/assistant/knowledge/admin-documents?${query.toString()}`, {
      method: 'POST',
      headers,
      body: formData,
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ detail: 'Upload failed' }))
      throw new Error(error.detail || 'Upload failed')
    }

    return response.json()
  },

  uploadUserDocument: async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    const response = await fetch(`${API_URL}/assistant/knowledge/user-documents`, {
      method: 'POST',
      headers,
      body: formData,
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ detail: 'Upload failed' }))
      throw new Error(error.detail || 'Upload failed')
    }

    return response.json()
  },
}
