import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as accountApi from '../api/account'

export const useAccountStore = defineStore('account', () => {
  const accounts = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchAccounts() {
    loading.value = true
    error.value = null
    try {
      accounts.value = await accountApi.getAllAccounts()
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createAccount(data) {
    loading.value = true
    error.value = null
    try {
      const newAccount = await accountApi.createAccount(data)
      accounts.value.push(newAccount)
      return newAccount
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateAccount(id, data) {
    loading.value = true
    error.value = null
    try {
      const updated = await accountApi.updateAccount(id, data)
      const index = accounts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        accounts.value[index] = updated
      }
      return updated
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteAccount(id) {
    loading.value = true
    error.value = null
    try {
      await accountApi.deleteAccount(id)
      accounts.value = accounts.value.filter(a => a.id !== id)
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    accounts,
    loading,
    error,
    fetchAccounts,
    createAccount,
    updateAccount,
    deleteAccount
  }
})
