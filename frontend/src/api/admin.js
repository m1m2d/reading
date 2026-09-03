import request from './request'

export function adminBooks(params) {
  return request.get('/admin/books', { params })
}

export function reviewBook(id, status, reason) {
  return request.patch(`/admin/books/${id}/review`, { status, reason })
}

export function deleteBook(id) {
  return request.delete(`/admin/books/${id}`)
}

export function getCategories() {
  return request.get('/categories/tree')
}

export function createCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}

export function listUsers(params) {
  return request.get('/admin/users', { params })
}

export function setUserStatus(id, status) {
  return request.patch(`/admin/users/${id}/status`, { status })
}

export function userActions(id, params) {
  return request.get(`/admin/users/${id}/actions`, { params })
}

export function listComments(params) {
  return request.get('/admin/comments', { params })
}

export function adminDeleteComment(id) {
  return request.delete(`/admin/comments/${id}`)
}

export function getConfig() {
  return request.get('/admin/config')
}

export function updateConfig(items) {
  return request.put('/admin/config', items)
}

export function backendMetrics() {
  return request.get('/admin/monitor/backend')
}

export function frontendMonitors(params) {
  return request.get('/admin/monitor/frontend', { params })
}

export function systemLogs(params) {
  return request.get('/admin/monitor/logs', { params })
}
