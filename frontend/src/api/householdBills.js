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

// 新增统计接口
export function getMonthlySummary(params) {
  return http.get('/api/v1/household-bills/statistics/monthly', { params })
}

export function getYearlySummary(params) {
  return http.get('/api/v1/household-bills/statistics/yearly', { params })
}

export function getMonthlyTrend(params) {
  return http.get('/api/v1/household-bills/statistics/monthly-trend', { params })
}

export function getYearlyTrend(params) {
  return http.get('/api/v1/household-bills/statistics/yearly-trend', { params })
}

export function updateTransactionSummary(id, txnTypeRaw, userId = 0) {
  return http.patch(`/api/v1/household-bills/transactions/${id}/summary?userId=${userId}`, {
    txnTypeRaw
  })
}

export function getAccountNames(userId = 0) {
  return http.get('/api/v1/household-bills/account-names', {
    params: { userId }
  })
}

export function getRules(userId = 0) {
  return http.get('/api/v1/household-bills/rules', {
    params: { userId }
  })
}

export function saveRules(rules, userId = 0) {
  return http.put('/api/v1/household-bills/rules', rules, {
    params: { userId }
  })
}

export function previewRerun(userId = 0) {
  return http.post('/api/v1/household-bills/rules/preview-rerun', null, {
    params: { userId }
  })
}

export function executeRerun(changeIds, userId = 0) {
  return http.post('/api/v1/household-bills/rules/execute-rerun', {
    changeIds
  }, {
    params: { userId }
  })
}
