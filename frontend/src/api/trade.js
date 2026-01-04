import http from '../utils/http'

export function getAllTrades(params) {
  return http.get('/api/v1/trades', { params })
}

export function getTradeById(id) {
  return http.get(`/api/v1/trades/${id}`)
}

export function createTrade(data) {
  return http.post('/api/v1/trades', data)
}

export function updateTrade(id, data) {
  return http.put(`/api/v1/trades/${id}`, data)
}

export function deleteTrade(id) {
  return http.delete(`/api/v1/trades/${id}`)
}
