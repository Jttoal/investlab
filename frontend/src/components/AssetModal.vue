<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal__header">
        <h2 class="modal__title">{{ isEditing ? '编辑标的' : '添加标的' }}</h2>
        <button class="modal__close" @click="$emit('close')">&times;</button>
      </div>
      
      <form @submit.prevent="submitForm">
        <div class="form-group">
          <label class="form-label">股票代码 *</label>
          <input 
            v-model="formData.symbol" 
            class="form-input" 
            required 
            placeholder="例如:000300"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">股票名称 *</label>
          <input 
            v-model="formData.name" 
            class="form-input" 
            required 
            placeholder="例如:沪深300ETF"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">市场 *</label>
          <select v-model="formData.market" class="form-select" required>
            <option value="CN">A股</option>
            <option value="HK">港股</option>
            <option value="US">美股</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">📉目标低于此价格时提醒</label>
          <input 
            v-model.number="formData.targetLow" 
            type="number" 
            step="0.01"
            class="form-input" 
            placeholder="低于此价格时提醒"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">📈目标于此价格时提醒</label>
          <input 
            v-model.number="formData.targetHigh" 
            type="number" 
            step="0.01"
            class="form-input" 
            placeholder="高于此价格时提醒"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea 
            v-model="formData.note" 
            class="form-textarea" 
            placeholder="记录一些关于这个标的的想法..."
          ></textarea>
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
import * as assetApi from '../api/asset'

const props = defineProps({
  strategyId: {
    type: Number,
    required: true
  },
  asset: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const isEditing = computed(() => !!props.asset)

const formData = ref({
  symbol: '',
  name: '',
  market: 'CN',
  strategyId: props.strategyId,
  targetLow: null,
  targetHigh: null,
  note: ''
})

onMounted(() => {
  if (props.asset) {
    formData.value = {
      symbol: props.asset.symbol,
      name: props.asset.name,
      market: props.asset.market,
      strategyId: props.strategyId,
      targetLow: props.asset.targetLow,
      targetHigh: props.asset.targetHigh,
      note: props.asset.note || ''
    }
  }
})

async function submitForm() {
  try {
    if (isEditing.value) {
      await assetApi.updateAsset(props.asset.id, formData.value)
    } else {
      await assetApi.createAsset(formData.value)
    }
    emit('saved')
  } catch (error) {
    console.error('操作失败:', error)
  }
}
</script>
