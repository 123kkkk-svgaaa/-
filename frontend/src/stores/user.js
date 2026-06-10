import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const address = ref(localStorage.getItem('walletAddress') || '')
  const token = ref(localStorage.getItem('token') || '')

  function setAuth(addr, tk) {
    address.value = addr
    token.value = tk
    localStorage.setItem('walletAddress', addr)
    localStorage.setItem('token', tk)
  }

  function logout() {
    address.value = ''
    token.value = ''
    localStorage.removeItem('walletAddress')
    localStorage.removeItem('token')
  }

  return { address, token, setAuth, logout }
})
