import http from '../utils/http'

export function listSettings(keyword) {
  return http.get('/api/v1/settings', { params: { keyword } })
}

export function createSetting(data) {
  return http.post('/api/v1/settings', data)
}

export function updateSetting(id, data) {
  return http.put(`/api/v1/settings/${id}`, data)
}

export function deleteSetting(id) {
  return http.delete(`/api/v1/settings/${id}`)
}
