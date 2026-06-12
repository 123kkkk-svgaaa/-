import api from './index'

export function getNonce(address) {
  return api.post('/auth/nonce', null, { params: { address } })
}

export function login(address, sig) {
  return api.post('/auth/login', { address, signature: sig })
}
