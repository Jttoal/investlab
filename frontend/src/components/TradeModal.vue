<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal__header">
        <h2 class="modal__title">记录交易</h2>
        <button class="modal__close" @click="$emit('close')">&times;</button>
      </div>
      
      <form @submit.prevent="submitForm">
        <div class="form-group">
          <label class="form-label">标的 *</label>
          <select v-model="formData.assetId" class="form-select" required>
            <option value="">请选择标的</option>
            <option v-for="item in props.assets" :key="item.asset.id" :value="item.asset.id">
              {{ item.asset.name }} ({{ item.asset.symbol }})
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">账户 *</label>
          <select v-model="formData.accountId" class="form-select" required>
            <option value="">请选择账户</option>
            <option v-for="account in accounts" :key="account.id" :value="account.id">
              {{ account.name }} - {{ account.broker }}
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">交易类型 *</label>
          <select v-model="formData.type" class="form-select" required>
            <option value="buy">买入</option>
            <option value="sell">卖出</option>
            <option value="dividend">分红</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="form-label">交易日期 *</label>
          <input 
            v-model="formData.tradeDate" 
            type="date" 
            class="form-input" 
            required 
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">价格 *</label>
          <input 
            v-model.number="formData.price" 
            type="number" 
            step="0.0001"
            class="form-input" 
            required 
            placeholder="成交价格"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">数量 *</label>
          <input 
            v-model.number="formData.quantity" 
            type="number" 
            class="form-input" 
            required 
            placeholder="交易数量"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">手续费</label>
          <input 
            v-model.number="formData.fee" 
            type="number" 
            step="0.01"
            class="form-input" 
            placeholder="0.00"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea 
            v-model="formData.note" 
            class="form-textarea" 
            placeholder="记录交易的原因和想法..."
          ></textarea>
        </div>
        
        <div class="modal__footer">
          <button type="button" class="btn btn--secondary" @click="$emit('close')">
            取消
          </button>
          <button type="submit" class="btn btn--primary">
            保存
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import * as tradeApi from '../api/trade'
import * as accountApi from '../api/account'

const props = defineProps({
  strategyId: {
    type: Number,
    required: true
  },
  assets: {
    type: Array,
    required: true
  },
  trade: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const accounts = ref([])
const isEditing = computed(() => !!props.trade)

const formData = ref({
  strategyId: props.strategyId,
  accountId: '',
  assetId: '',
  type: 'buy',
  price: 0,
  quantity: 0,
  fee: 0,
  tradeDate: new Date().toISOString().split('T')[0],
  note: ''
})

onMounted(async () => {
  try {
    accounts.value = await accountApi.getAllAccounts()
  } catch (error) {
    console.error('加载账户失败:', error)
  }
  if (props.trade) {
    formData.value = {
      strategyId: props.trade.strategyId,
      accountId: props.trade.accountId,
      assetId: props.trade.assetId,
      type: props.trade.type,
      price: props.trade.price,
      quantity: props.trade.quantity,
      fee: props.trade.fee,
      tradeDate: props.trade.tradeDate,
      note: props.trade.note || ''
    }
  }
})

async function submitForm() {
  try {
    if (isEditing.value) {
      await tradeApi.updateTrade(props.trade.id, formData.value)
    } else {
      await tradeApi.createTrade(formData.value)
    }
    emit('saved')
  } catch (error) {
    console.error('操作失败:', error)
  }
}
</script>
