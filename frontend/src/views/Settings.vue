<template>
  <div class="settings-page">
    <div class="toolbar">
      <h1>系统设置</h1>
      <div class="toolbar__actions">
        <input v-model="keyword" class="form-input search" placeholder="搜索 key" @keyup.enter="loadSettings" />
        <button class="btn btn--primary" @click="showModal = true">新增</button>
        <button class="btn btn--secondary" @click="loadSettings">刷新</button>
      </div>
    </div>

    <div class="card">
      <table class="table" v-if="settings.length > 0">
        <thead>
          <tr>
            <th>Key</th>
            <th>Value</th>
            <th>类型</th>
            <th>描述</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in settings" :key="item.id">
            <td>{{ item.key }}</td>
            <td>
              <pre class="value-cell">{{ item.value }}</pre>
            </td>
            <td><span class="badge badge--info">{{ item.valueType }}</span></td>
            <td>{{ item.description || '-' }}</td>
            <td>{{ formatDateTime(item.updatedAt) }}</td>
            <td class="table-actions">
              <button class="btn btn--secondary btn--sm" @click="startEdit(item)">编辑</button>
              <button class="btn btn--danger btn--sm" @click="remove(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无设置</div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal large">
        <div class="modal__header">
          <h2 class="modal__title">{{ isEditing ? '编辑设置' : '新增设置' }}</h2>
          <button class="modal__close" @click="closeModal">&times;</button>
        </div>
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label class="form-label">Key *</label>
            <input v-model="form.key" class="form-input" :disabled="isEditing" required />
          </div>
          <div class="form-group">
            <label class="form-label">类型 *</label>
            <select v-model="form.valueType" class="form-select" required>
              <option value="string">string</option>
              <option value="number">number</option>
              <option value="json">json</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Value *</label>
            <textarea v-model="form.value" class="form-textarea" rows="8" required></textarea>
            <div class="btn-row">
              <button type="button" class="btn btn--secondary btn--sm" @click="formatJson" :disabled="form.valueType !== 'json'">格式化 JSON</button>
              <span v-if="jsonError" class="error-text">{{ jsonError }}</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">描述</label>
            <input v-model="form.description" class="form-input" placeholder="可选" />
          </div>
          <div class="modal__footer">
            <button type="button" class="btn btn--secondary" @click="closeModal">取消</button>
            <button type="submit" class="btn btn--primary">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listSettings, createSetting, updateSetting, deleteSetting } from '../api/setting'

const settings = ref([])
const keyword = ref('')
const showModal = ref(false)
const editingItem = ref(null)
const jsonError = ref('')

const form = ref({
  key: '',
  value: '',
  valueType: 'string',
  description: ''
})

const isEditing = computed(() => !!editingItem.value)

onMounted(() => {
  loadSettings()
})

async function loadSettings() {
  settings.value = await listSettings(keyword.value || undefined)
}

function startEdit(item) {
  editingItem.value = item
  form.value = {
    key: item.key,
    value: item.value,
    valueType: item.valueType,
    description: item.description || ''
  }
  jsonError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingItem.value = null
  jsonError.value = ''
  form.value = {
    key: '',
    value: '',
    valueType: 'string',
    description: ''
  }
}

function formatJson() {
  if (form.value.valueType !== 'json') return
  try {
    const parsed = JSON.parse(form.value.value)
    form.value.value = JSON.stringify(parsed, null, 2)
    jsonError.value = ''
  } catch (e) {
    jsonError.value = 'JSON 格式错误'
  }
}

async function submitForm() {
  try {
    if (form.value.valueType === 'json') {
      JSON.parse(form.value.value)
    } else if (form.value.valueType === 'number') {
      if (isNaN(Number(form.value.value))) {
        alert('请输入数字')
        return
      }
    }
    if (isEditing.value) {
      await updateSetting(editingItem.value.id, form.value)
    } else {
      await createSetting(form.value)
    }
    closeModal()
    loadSettings()
  } catch (e) {
    alert(e.message || '保存失败')
  }
}

async function remove(id) {
  if (confirm('确定删除该配置吗？')) {
    await deleteSetting(id)
    loadSettings()
  }
}

function formatDateTime(ts) {
  return new Date(ts).toLocaleString('zh-CN')
}
</script>

<style scoped>
.settings-page {
  max-width: 1200px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.toolbar__actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
.search {
  width: 200px;
}
.value-cell {
  white-space: pre-wrap;
  max-height: 120px;
  overflow: auto;
  background: #f8f9fa;
  padding: 0.5rem;
  border-radius: 4px;
}
.table-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.btn-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
.error-text {
  color: #d9534f;
}
.modal.large {
  max-width: 720px;
  width: 90%;
}
</style>
