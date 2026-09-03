import request from './request'

export function listPosts(params) {
  return request.get('/posts', { params })
}

export function getPost(id) {
  return request.get(`/posts/${id}`)
}

export function createPost(title, content, images, onProgress) {
  const form = new FormData()
  form.append('title', title)
  if (content) form.append('content', content)
  ;(images || []).forEach((file) => form.append('images', file))
  return request.post('/posts', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => onProgress && e.total && onProgress(Math.round((e.loaded / e.total) * 100))
  })
}

export function deletePost(id) {
  return request.delete(`/posts/${id}`)
}

export function togglePostLike(id) {
  return request.post(`/posts/${id}/like`)
}

export function listPostComments(postId) {
  return request.get(`/posts/${postId}/comments`)
}

export function addPostComment(postId, data) {
  return request.post(`/posts/${postId}/comments`, data)
}

export function togglePostCommentLike(commentId) {
  return request.post(`/post-comments/${commentId}/like`)
}

export function deletePostComment(id) {
  return request.delete(`/post-comments/${id}`)
}

export function receivedComments() {
  return request.get('/users/me/received-comments')
}
