import http from '../utils/http'

export function getAllStrategies(type) {
  return http.get('/api/v1/strategies', { params: { type } })
}

export function getStrategyById(id) {
  return http.get(`/api/v1/strategies/${id}`)
}

export function createStrategy(data) {
  return http.post('/api/v1/strategies', data)
}

export function updateStrategy(id, data) {
  return http.put(`/api/v1/strategies/${id}`, data)
}

export function deleteStrategy(id) {
  return http.delete(`/api/v1/strategies/${id}`)
}

export function searchStrategies(name) {
  return http.get('/api/v1/strategies/search', { params: { name } })
}

export function getStrategyConfig(id) {
  return http.get(`/api/v1/strategies/${id}/config`)
}

export function updateStrategyConfig(id, config) {
  return http.put(`/api/v1/strategies/${id}/config`, config)
}
