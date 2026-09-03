import { defineStore } from 'pinia'
import { login as apiLogin, refreshToken, getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('cloudread_token') || '',
    user: JSON.parse(localStorage.getItem('cloudread_user') || 'null'),
    isRefreshing: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin(username, password)
      this.setSession(data)
      return data
    },
    async refresh() {
      if (!this.token) return
      this.isRefreshing = true
      try {
        const data = await refreshToken(this.token)
        this.setSession(data)
        return data
      } finally {
        this.isRefreshing = false
      }
    },
    async fetchMe() {
      if (this.token) {
        this.user = await getMe()
        localStorage.setItem('cloudread_user', JSON.stringify(this.user))
      }
    },
    setSession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('cloudread_token', data.token)
      localStorage.setItem('cloudread_user', JSON.stringify(data.user))
    },
    setAvatar(url) {
      if (this.user) {
        this.user.avatarUrl = url
        localStorage.setItem('cloudread_user', JSON.stringify(this.user))
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('cloudread_token')
      localStorage.removeItem('cloudread_user')
    }
  }
})
