import request from './request'

export function listBooks(params) {
  return request.get('/books', { params })
}

export function getBook(id) {
  return request.get(`/books/${id}`)
}

export function myUploads() {
  return request.get('/books/my')
}

export function getTrace(id) {
  return request.get(`/books/${id}/trace`)
}

export function verifyBook(id) {
  return request.post(`/books/${id}/verify`)
}

export function getContent(id) {
  return request.get(`/books/${id}/content`)
}

export function getProgress(id) {
  return request.get(`/books/${id}/progress`)
}

export function saveProgress(id, position) {
  return request.put(`/books/${id}/progress`, { position })
}

export function uploadBookFile(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/books/file', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => onProgress && onProgress(Math.round((e.loaded / e.total) * 100))
  })
}

export function initChunk(data) {
  return request.post('/books/upload/init', data)
}

export function uploadChunk(uploadId, index, chunk) {
  const form = new FormData()
  form.append('uploadId', uploadId)
  form.append('index', index)
  form.append('chunk', chunk)
  return request.post('/books/upload/chunk', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function completeChunk(uploadId, originalName) {
  const form = new FormData()
  form.append('uploadId', uploadId)
  form.append('originalName', originalName)
  return request.post('/books/upload/complete', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function commitBook(payload, coverFile) {
  const form = new FormData()
  form.append('meta', JSON.stringify(payload.meta))
  form.append('hash', payload.fileRef.hash)
  form.append('relativePath', payload.fileRef.relativePath)
  form.append('format', payload.fileRef.format)
  form.append('size', payload.fileRef.size)
  form.append('originalName', payload.fileRef.originalName)
  if (coverFile) {
    form.append('cover', coverFile)
  }
  return request.post('/books/commit', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
