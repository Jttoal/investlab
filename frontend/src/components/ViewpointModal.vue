<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal__header">
        <h2 class="modal__title">添加大V观点</h2>
        <button class="modal__close" @click="$emit('close')">&times;</button>
      </div>
      
      <form @submit.prevent="submitForm">
        <div class="form-group">
          <label class="form-label">关联标的</label>
          <select v-model="formData.assetId" class="form-select">
            <option :value="null">不关联</option>
            <option v-for="item in assets" :key="item.asset.id" :value="item.asset.id">
              {{ item.asset.name }} ({{ item.asset.symbol }})
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">标题 *</label>
          <input 
            v-model="formData.title" 
            class="form-input" 
            required 
            placeholder="观点标题或文章标题"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">摘要</label>
          <textarea 
            v-model="formData.summary" 
            class="form-textarea" 
            placeholder="记录核心观点和要点..."
          ></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea 
            v-model="formData.remark" 
            class="form-textarea" 
            placeholder="记录个人想法或补充..."
          ></textarea>
        </div>
        
        <div class="form-group">
          <label class="form-label">链接</label>
          <input 
            v-model="formData.link" 
            type="url"
            class="form-input" 
            placeholder="https://..."
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">标签</label>
          <select v-model="formData.tag" class="form-select">
            <option value="">无标签</option>
            <option value="宏观">宏观</option>
            <option value="个股">个股</option>
            <option value="调仓建议">调仓建议</option>
            <option value="风险提示">风险提示</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">观点日期 *</label>
          <input 
            v-model="formData.viewpointDate" 
            type="date" 
            class="form-input" 
            required 
          />
        </div>
        
        <div class="modal__footer">
          <button type="button" class="btn btn--secondary" @click="$emit('close')">
            取消
          </button>
          <button type="submit" class="btn btn--primary">
            {{ isEditing ? '保存' : '添加' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as viewpointApi from '../api/viewpoint'

const props = defineProps({
  strategyId: {
    type: Number,
    required: true
  },
  assets: {
    type: Array,
    default: () => []
  },
  viewpoint: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])
const isEditing = computed(() => !!props.viewpoint)

const formData = ref({
  strategyId: props.strategyId,
  assetId: null,
  title: '',
  summary: '',
  remark: '',
  link: '',
  tag: '',
  viewpointDate: new Date().toISOString().split('T')[0]
})

onMounted(() => {
  if (props.viewpoint) {
    formData.value = {
      strategyId: props.viewpoint.strategyId,
      assetId: props.viewpoint.assetId || null,
      title: props.viewpoint.title,
      summary: props.viewpoint.summary || '',
      remark: props.viewpoint.remark || '',
      link: props.viewpoint.link || '',
      tag: props.viewpoint.tag || '',
      viewpointDate: props.viewpoint.viewpointDate
    }
  }
})

async function submitForm() {
  try {
    if (isEditing.value) {
      await viewpointApi.updateViewpoint(props.viewpoint.id, formData.value)
    } else {
      await viewpointApi.createViewpoint(formData.value)
    }
    emit('saved')
  } catch (error) {
    console.error('操作失败:', error)
  }
}
</script>
