import request from './request'

export function getUser(id) {
  return request.get(`/users/${id}`)
}

export function getUserContributions(id) {
  return request.get(`/users/${id}/contributions`)
}

export function toggleFollow(id) {
  return request.post(`/users/${id}/follow`)
}

export function getFollowing(params) {
  return request.get('/users/me/following', { params })
}

export function uploadAvatar(file, onProgress) {
  const form = new FormData()
  form.append('avatar', file)
  return request.post('/users/me/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => onProgress && e.total && onProgress(Math.round((e.loaded / e.total) * 100))
  })
}
