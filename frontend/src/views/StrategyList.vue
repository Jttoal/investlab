<template>
  <div class="strategy-list">
    <div class="toolbar">
      <h1>策略管理</h1>
      <div class="toolbar__actions">
        <select v-model="filterType" class="form-select" @change="loadStrategies">
          <option value="">全部类型</option>
          <option value="index">指数投资</option>
          <option value="dividend">红利低波</option>
          <option value="grid">网格策略</option>
          <option value="custom">自定义</option>
        </select>
        <button class="btn btn--primary" @click="showCreateModal = true">
          新建策略
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="strategies-grid">
      <div 
        v-for="strategy in strategies" 
        :key="strategy.id" 
        class="strategy-card"
        @click="goToDetail(strategy.id)"
      >
        <div class="strategy-card__header">
          <h3 class="strategy-card__title">{{ strategy.name }}</h3>
          <span class="badge badge--info">{{ getStrategyTypeLabel(strategy.type) }}</span>
        </div>
        <p class="strategy-card__note">{{ strategy.goalNote || '暂无目标说明' }}</p>
        <div class="strategy-card__footer">
          <span class="strategy-card__date">{{ formatDate(strategy.createdAt) }}</span>
          <div class="strategy-card__actions" @click.stop>
            <button class="btn btn--secondary btn--sm" @click="editStrategy(strategy)">
              编辑
            </button>
            <button class="btn btn--danger btn--sm" @click="confirmDelete(strategy)">
              删除
            </button>
          </div>
        </div>
      </div>

      <div v-if="strategies.length === 0" class="empty-state">
        暂无策略,点击"新建策略"开始添加
      </div>
    </div>

    <!-- 创建/编辑模态框 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">{{ isEditing ? '编辑策略' : '新建策略' }}</h2>
          <button class="modal__close" @click="closeModal">&times;</button>
        </div>
        
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label class="form-label">策略名称 *</label>
            <input 
              v-model="formData.name" 
              class="form-input" 
              required 
              placeholder="例如:沪深300定投"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">策略类型 *</label>
            <select v-model="formData.type" class="form-select" required>
              <option value="index">指数投资</option>
              <option value="dividend">红利低波</option>
              <option value="grid">网格策略</option>
              <option value="custom">自定义</option>
            </select>
          </div>
          
          <div class="form-group">
            <label class="form-label">目标说明</label>
            <textarea 
              v-model="formData.goalNote" 
              class="form-textarea" 
              placeholder="描述这个策略的目标和执行方式..."
            ></textarea>
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStrategyStore } from '../stores/strategy'
import { storeToRefs } from 'pinia'

const router = useRouter()
const strategyStore = useStrategyStore()
const { strategies, loading } = storeToRefs(strategyStore)

const filterType = ref('')
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingStrategy = ref(null)

const formData = ref({
  name: '',
  type: 'index',
  goalNote: ''
})

const isEditing = computed(() => !!editingStrategy.value)

onMounted(() => {
  loadStrategies()
})

function loadStrategies() {
  strategyStore.fetchStrategies(filterType.value || undefined)
}

function goToDetail(id) {
  router.push(`/strategies/${id}`)
}

function editStrategy(strategy) {
  editingStrategy.value = strategy
  formData.value = {
    name: strategy.name,
    type: strategy.type,
    goalNote: strategy.goalNote || ''
  }
  showEditModal.value = true
}

async function submitForm() {
  try {
    if (isEditing.value) {
      await strategyStore.updateStrategy(editingStrategy.value.id, formData.value)
    } else {
      await strategyStore.createStrategy(formData.value)
    }
    closeModal()
    loadStrategies()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

function confirmDelete(strategy) {
  if (confirm(`确定要删除策略"${strategy.name}"吗?这将同时删除相关的标的、交易和观点记录。`)) {
    strategyStore.deleteStrategy(strategy.id)
  }
}

function closeModal() {
  showCreateModal.value = false
  showEditModal.value = false
  editingStrategy.value = null
  formData.value = {
    name: '',
    type: 'index',
    goalNote: ''
  }
}

function getStrategyTypeLabel(type) {
  const labels = {
    'index': '指数投资',
    'dividend': '红利低波',
    'grid': '网格策略',
    'custom': '自定义'
  }
  return labels[type] || type
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.strategies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.strategy-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.strategy-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.strategy-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.strategy-card__title {
  margin: 0;
  font-size: 1.25rem;
  color: #2c3e50;
}

.strategy-card__note {
  color: #6c757d;
  font-size: 0.875rem;
  margin: 0 0 1rem 0;
  min-height: 2.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.strategy-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.strategy-card__date {
  color: #6c757d;
  font-size: 0.875rem;
}

.strategy-card__actions {
  display: flex;
  gap: 0.5rem;
}

.btn--sm {
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
}

.form-select {
  min-width: 150px;
}
</style>
