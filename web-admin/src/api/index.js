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
