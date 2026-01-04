<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal large">
      <div class="modal__header">
        <h2 class="modal__title">账户交易 - {{ account?.name }}</h2>
        <button class="modal__close" @click="$emit('close')">&times;</button>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="table-wrapper">
        <table class="table" v-if="trades.length > 0">
          <thead>
            <tr>
              <th>日期</th>
              <th>类型</th>
              <th>策略</th>
              <th>标的</th>
              <th>价格</th>
              <th>数量</th>
              <th>手续费</th>
              <th>备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="trade in trades" :key="trade.id">
              <td>{{ formatDate(trade.tradeDate) }}</td>
              <td><span class="badge" :class="getTradeTypeBadge(trade.type)">{{ getTradeTypeLabel(trade.type) }}</span></td>
              <td>{{ trade.strategyId }}</td>
              <td>{{ trade.assetId }}</td>
              <td>{{ formatMoney(trade.price) }}</td>
              <td>{{ trade.quantity }}</td>
              <td>{{ formatMoney(trade.fee) }}</td>
              <td>{{ trade.note || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无交易</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getAccountTrades } from '../api/account'

const props = defineProps({
  account: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close'])

const trades = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    trades.value = await getAccountTrades(props.account.id)
  } finally {
    loading.value = false
  }
})

function formatMoney(value) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

function getTradeTypeLabel(type) {
  const labels = { buy: '买入', sell: '卖出', dividend: '分红' }
  return labels[type] || type
}

function getTradeTypeBadge(type) {
  const badges = { buy: 'badge--success', sell: 'badge--danger', dividend: 'badge--info' }
  return badges[type] || 'badge--info'
}
</script>

<style scoped>
.modal.large {
  max-width: 960px;
}
.table-wrapper {
  max-height: 70vh;
  overflow: auto;
}
</style>
