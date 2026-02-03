import http from '../utils/http'

export function getAllAssets(strategyId) {
  return http.get('/api/v1/assets', { params: { strategyId } })
}

export function getAssetById(id) {
  return http.get(`/api/v1/assets/${id}`)
}

export function createAsset(data) {
  return http.post('/api/v1/assets', data)
}

export function updateAsset(id, data) {
  return http.put(`/api/v1/assets/${id}`, data)
}

export function deleteAsset(id) {
  return http.delete(`/api/v1/assets/${id}`)
}

export function searchAssets(symbol) {
  return http.get('/api/v1/assets/search', { params: { symbol } })
}

export function getAssetDetail(id) {
  return http.get(`/api/v1/assets/${id}/detail`)
}

export function getAssetKline(id, period = 'day') {
  return http.get(`/api/v1/assets/${id}/kline`, { params: { period } })
}

export function getAssetKlineBundle(id, { period = 'day', start, end } = {}) {
  const params = { period }
  if (start) params.start = start
  if (end) params.end = end
  return http.get(`/api/v1/assets/${id}/kline/bundle`, { params })
}

export function getAssetConfig(id) {
  return http.get(`/api/v1/assets/${id}/config`)
}

export function updateAssetConfig(id, config) {
  return http.put(`/api/v1/assets/${id}/config`, config)
}
