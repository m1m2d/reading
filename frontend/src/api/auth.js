import request from './request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function refreshToken(token) {
  return request.post('/auth/refresh', { token })
}

export function getMe() {
  return request.get('/auth/me')
}
