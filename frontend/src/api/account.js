import http from '../utils/http'

export function getAllAccounts() {
  return http.get('/api/v1/accounts')
}

export function getAccountById(id) {
  return http.get(`/api/v1/accounts/${id}`)
}

export function createAccount(data) {
  return http.post('/api/v1/accounts', data)
}

export function updateAccount(id, data) {
  return http.put(`/api/v1/accounts/${id}`, data)
}

export function deleteAccount(id) {
  return http.delete(`/api/v1/accounts/${id}`)
}

export function searchAccounts(name) {
  return http.get('/api/v1/accounts/search', { params: { name } })
}

export function getAccountTrades(id) {
  return http.get(`/api/v1/accounts/${id}/trades`)
}
