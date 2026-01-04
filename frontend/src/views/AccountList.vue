<template>
  <div class="account-list">
    <div class="toolbar">
      <h1>账户管理</h1>
      <button class="btn btn--primary" @click="showCreateModal = true">
        新建账户
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="card">
      <table class="table" v-if="accounts.length > 0">
        <thead>
          <tr>
            <th>账户名称</th>
            <th>券商</th>
            <th>币种</th>
            <th>余额</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in accounts" :key="account.id">
            <td>{{ account.name }}</td>
            <td>{{ account.broker }}</td>
            <td>{{ account.currency }}</td>
            <td>{{ formatMoney(account.balanceManual) }}</td>
            <td>{{ formatDate(account.createdAt) }}</td>
            <td class="table-actions">
              <button class="btn btn--secondary btn--sm" @click="editAccount(account)">
                编辑
              </button>
              <button class="btn btn--info btn--sm" @click="openTrades(account)">
                查看交易
              </button>
              <button class="btn btn--danger btn--sm" @click="confirmDelete(account)">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        暂无账户,点击"新建账户"开始添加
      </div>
    </div>

    <!-- 创建/编辑模态框 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">{{ isEditing ? '编辑账户' : '新建账户' }}</h2>
          <button class="modal__close" @click="closeModal">&times;</button>
        </div>
        
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label class="form-label">账户名称 *</label>
            <input 
              v-model="formData.name" 
              class="form-input" 
              required 
              placeholder="例如:招商证券主账户"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">券商 *</label>
            <input 
              v-model="formData.broker" 
              class="form-input" 
              required 
              placeholder="例如:招商证券"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">币种</label>
            <select v-model="formData.currency" class="form-select">
              <option value="CNY">人民币(CNY)</option>
              <option value="USD">美元(USD)</option>
              <option value="HKD">港币(HKD)</option>
            </select>
          </div>
          
          <div class="form-group">
            <label class="form-label">账户余额</label>
            <input 
              v-model.number="formData.balanceManual" 
              type="number" 
              step="0.01"
              class="form-input" 
              placeholder="0.00"
            />
          </div>
          
          <div class="modal__footer">
            <button type="button" class="btn btn--secondary" @click="closeModal">
              取消
            </button>
            <button type="submit" class="btn btn--primary">
              {{ isEditing ? '保存' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <AccountTradesModal
      v-if="showTradesModal"
      :account="currentAccount"
      @close="closeTrades"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAccountStore } from '../stores/account'
import { storeToRefs } from 'pinia'
import AccountTradesModal from '../components/AccountTradesModal.vue'

const accountStore = useAccountStore()
const { accounts, loading } = storeToRefs(accountStore)

const showCreateModal = ref(false)
const showEditModal = ref(false)
const showTradesModal = ref(false)
const editingAccount = ref(null)
const currentAccount = ref(null)

const formData = ref({
  name: '',
  broker: '',
  currency: 'CNY',
  balanceManual: 0
})

const isEditing = computed(() => !!editingAccount.value)

onMounted(() => {
  accountStore.fetchAccounts()
})

function editAccount(account) {
  editingAccount.value = account
  formData.value = {
    name: account.name,
    broker: account.broker,
    currency: account.currency,
    balanceManual: account.balanceManual
  }
  showEditModal.value = true
}

async function submitForm() {
  try {
    if (isEditing.value) {
      await accountStore.updateAccount(editingAccount.value.id, formData.value)
    } else {
      await accountStore.createAccount(formData.value)
    }
    closeModal()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

function confirmDelete(account) {
  if (confirm(`确定要删除账户"${account.name}"吗?`)) {
    accountStore.deleteAccount(account.id)
  }
}

function openTrades(account) {
  currentAccount.value = account
  showTradesModal.value = true
}

function closeTrades() {
  showTradesModal.value = false
  currentAccount.value = null
}

function closeModal() {
  showCreateModal.value = false
  showEditModal.value = false
  editingAccount.value = null
  formData.value = {
    name: '',
    broker: '',
    currency: 'CNY',
    balanceManual: 0
  }
}

function formatMoney(value) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.table-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn--sm {
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
  margin-right: 0.5rem;
}
</style>
