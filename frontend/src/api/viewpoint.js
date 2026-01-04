import http from '../utils/http'

export function getAllViewpoints(params) {
  return http.get('/api/v1/viewpoints', { params })
}

export function getViewpointById(id) {
  return http.get(`/api/v1/viewpoints/${id}`)
}

export function createViewpoint(data) {
  return http.post('/api/v1/viewpoints', data)
}

export function updateViewpoint(id, data) {
  return http.put(`/api/v1/viewpoints/${id}`, data)
}

export function deleteViewpoint(id) {
  return http.delete(`/api/v1/viewpoints/${id}`)
}
