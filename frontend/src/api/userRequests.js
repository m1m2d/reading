import request from './request'

export function submitPasswordReset(data) {
  return request.post('/user-requests/password-reset', data)
}

export function listUserRequests(params) {
  return request.get('/admin/user-requests', { params })
}

export function resetUserPassword(id, newPassword) {
  return request.post(`/admin/user-requests/${id}/reset-password`, { newPassword })
}

export function archiveUserRequest(id) {
  return request.post(`/admin/user-requests/${id}/archive`)
}
