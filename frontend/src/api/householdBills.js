import http from '../utils/http'

export function uploadBill(file, userId = 0) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', userId)
  return http.post('/api/v1/household-bills/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getUploadStatus(id) {
  return http.get(`/api/v1/household-bills/uploads/${id}`)
}

export function listTransactions(params) {
  return http.get('/api/v1/household-bills/transactions', { params })
}

export function getSummary(params) {
  return http.get('/api/v1/household-bills/summary', { params })
}
