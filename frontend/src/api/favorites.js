import request from './request'

export function toggleFavorite(bookId) {
  return request.post('/favorites/toggle', { bookId })
}

export function listFavorites(page, size) {
  return request.get('/favorites', { params: { page, size } })
}
