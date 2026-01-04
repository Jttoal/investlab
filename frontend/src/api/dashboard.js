import http from '../utils/http'

export function getDashboard() {
  return http.get('/api/v1/dashboard')
}

export function getStrategyDetail(strategyId) {
  return http.get(`/api/v1/dashboard/strategies/${strategyId}`)
}
