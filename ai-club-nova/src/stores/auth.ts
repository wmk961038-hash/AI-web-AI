import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api, refreshCsrf, type Account } from '../api'

export const useAuth = defineStore('auth', () => {
  const user = ref<Account | null>(null)

  async function load() {
    try {
      user.value = (await api.get('/auth/me')).data
    } catch {
      user.value = null
    }
  }

  async function login(username: string, password: string) {
    user.value = (await api.post('/auth/login', { username, password })).data
    await refreshCsrf()
  }

  async function logout() {
    await api.post('/auth/logout')
    user.value = null
    await refreshCsrf()
  }

  return { user, load, login, logout }
})
