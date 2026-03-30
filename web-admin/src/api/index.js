const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3001/api'

async function request(endpoint, options = {}) {
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Request failed' }))
    throw new Error(error.error || 'Request failed')
  }
  return response.json()
}

export const productsApi = {
  getAll: () => request('/products'),
  create: (data) => request('/products', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/products/${id}`, { method: 'DELETE' }),
  
  uploadPhoto: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${API_URL}/products/${productId}/photos`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: formData
    })
    if (!response.ok) {
      throw new Error('Upload failed')
    }
    return response.json()
  },
  
  deletePhoto: async (productId, photoId) => {
    const response = await fetch(`${API_URL}/products/${productId}/photos/${photoId}`, {
      method: 'DELETE',
      headers: { 'Accept': 'application/json' }
    })
    if (!response.ok) {
      throw new Error('Delete failed')
    }
    return response.json()
  },
  
  uploadVideo: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${API_URL}/products/${productId}/video`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: formData
    })
    if (!response.ok) {
      throw new Error('Upload failed')
    }
    return response.json()
  },
  
  deleteVideo: async (productId) => {
    const response = await fetch(`${API_URL}/products/${productId}/video`, {
      method: 'DELETE',
      headers: { 'Accept': 'application/json' }
    })
    if (!response.ok) {
      throw new Error('Delete failed')
    }
    return response.json()
  },
  
  getVideoUrl: (productId) => `${API_URL}/products/${productId}/video`,
  parseUrl: (url) => request('/products/parse', { method: 'POST', body: JSON.stringify({ url }) }),
}

export const dictionariesApi = {
  get: (key) => request(`/dictionaries/${key}`),
  create: (key, value) => request(`/dictionaries/${key}`, { method: 'POST', body: JSON.stringify({ value }) }),
  update: (key, oldValue, newValue) => request(`/dictionaries/${key}`, { method: 'PUT', body: JSON.stringify({ oldValue, newValue }) }),
  delete: (key, value) => request(`/dictionaries/${key}/${encodeURIComponent(value)}`, { method: 'DELETE' }),
}

export const proceduresApi = {
  getAll: () => request('/procedures'),
  create: (data) => request('/procedures', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/procedures/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/procedures/${id}`, { method: 'DELETE' }),
}

export const contentApi = {
  getAll: () => request('/content'),
  create: (data) => request('/content', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/content/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/content/${id}`, { method: 'DELETE' }),
}

export const usersApi = {
  getAll: () => request('/users'),
  create: (data) => request('/users', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/users/${id}`, { method: 'DELETE' }),
}
