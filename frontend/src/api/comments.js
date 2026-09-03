import request from './request'

export function listComments(bookId) {
  return request.get(`/books/${bookId}/comments`)
}

export function addComment(data) {
  return request.post('/comments', data)
}

export function toggleLike(commentId) {
  return request.post(`/comments/${commentId}/like`)
}

export function deleteComment(commentId) {
  return request.delete(`/comments/${commentId}`)
}
