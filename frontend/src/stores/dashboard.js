import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashboardApi from '../api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const dashboardData = ref(null)
  const strategyDetail = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchDashboard() {
    loading.value = true
    error.value = null
    try {
      dashboardData.value = await dashboardApi.getDashboard()
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchStrategyDetail(strategyId) {
    loading.value = true
    error.value = null
    try {
      strategyDetail.value = await dashboardApi.getStrategyDetail(strategyId)
      return strategyDetail.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    dashboardData,
    strategyDetail,
    loading,
    error,
    fetchDashboard,
    fetchStrategyDetail
  }
})
