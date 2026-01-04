import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as strategyApi from '../api/strategy'

export const useStrategyStore = defineStore('strategy', () => {
  const strategies = ref([])
  const currentStrategy = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchStrategies(type) {
    loading.value = true
    error.value = null
    try {
      strategies.value = await strategyApi.getAllStrategies(type)
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchStrategyById(id) {
    loading.value = true
    error.value = null
    try {
      currentStrategy.value = await strategyApi.getStrategyById(id)
      return currentStrategy.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createStrategy(data) {
    loading.value = true
    error.value = null
    try {
      const newStrategy = await strategyApi.createStrategy(data)
      strategies.value.push(newStrategy)
      return newStrategy
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateStrategy(id, data) {
    loading.value = true
    error.value = null
    try {
      const updated = await strategyApi.updateStrategy(id, data)
      const index = strategies.value.findIndex(s => s.id === id)
      if (index !== -1) {
        strategies.value[index] = updated
      }
      if (currentStrategy.value?.id === id) {
        currentStrategy.value = updated
      }
      return updated
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteStrategy(id) {
    loading.value = true
    error.value = null
    try {
      await strategyApi.deleteStrategy(id)
      strategies.value = strategies.value.filter(s => s.id !== id)
      if (currentStrategy.value?.id === id) {
        currentStrategy.value = null
      }
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    strategies,
    currentStrategy,
    loading,
    error,
    fetchStrategies,
    fetchStrategyById,
    createStrategy,
    updateStrategy,
    deleteStrategy
  }
})
