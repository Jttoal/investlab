<template>
  <div class="page">
    <div class="header">
      <h2 @click="goBack" class="clickable-title">
        <span class="back-icon">←</span> 家庭账单
      </h2>
      <span class="subtitle">统计分析</span>
    </div>

    <section class="card">
      <h3>汇总统计</h3>
      <div class="filters">
        <label>
          开始日期
          <input v-model="filters.startDate" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="filters.endDate" type="date" />
        </label>
        <button :disabled="loading" @click="loadSummary">
          {{ loading ? '加载中...' : '查询' }}
        </button>
      </div>

      <div v-if="summary.length" class="summary-container">
        <!-- 总览卡片 -->
        <div class="overview-cards">
          <div class="overview-card">
            <div class="card-label">普通支出</div>
            <div class="card-value neg">{{ formatAmount(totals.ordinaryExpense) }}</div>
          </div>
          <div class="overview-card">
            <div class="card-label">普通收入</div>
            <div class="card-value pos">{{ formatAmount(totals.ordinaryIncome) }}</div>
          </div>
          <div class="overview-card">
            <div class="card-label">投资买入</div>
            <div class="card-value neg">{{ formatAmount(totals.investmentBuy) }}</div>
          </div>
          <div class="overview-card">
            <div class="card-label">投资赎回</div>
            <div class="card-value pos">{{ formatAmount(totals.investmentRedeem) }}</div>
          </div>
        </div>

        <!-- 详细表格 -->
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>月份</th>
                <th>类型</th>
                <th>方向</th>
                <th>金额合计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in summary" :key="idx">
                <td>{{ row.month }}</td>
                <td>{{ displayCategory(row.category) }}</td>
                <td>{{ displayDirection(row.direction) }}</td>
                <td :class="row.totalAmount < 0 ? 'neg' : 'pos'">{{ formatAmount(row.totalAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-else class="empty">暂无汇总数据</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSummary } from '../api/householdBills'

const router = useRouter()

// 获取默认日期范围：上一年1月1日到今天
function getDefaultDateRange() {
  const today = new Date()
  const lastYear = today.getFullYear() - 1
  const startDate = `${lastYear}-01-01`
  const endDate = today.toISOString().split('T')[0]
  console.log('默认日期范围:', { startDate, endDate })
  return { startDate, endDate }
}

const defaultDates = getDefaultDateRange()

const filters = ref({
  startDate: defaultDates.startDate,
  endDate: defaultDates.endDate
})

const summary = ref([])
const loading = ref(false)

const totals = computed(() => {
  const result = {
    ordinaryExpense: 0,
    ordinaryIncome: 0,
    investmentBuy: 0,
    investmentRedeem: 0
  }
  
  summary.value.forEach(row => {
    if (row.category === 'ordinary') {
      if (row.direction === 'expense') {
        result.ordinaryExpense += Number(row.totalAmount)
      } else if (row.direction === 'income') {
        result.ordinaryIncome += Number(row.totalAmount)
      }
    } else if (row.category === 'investment') {
      if (row.direction === 'buy') {
        result.investmentBuy += Number(row.totalAmount)
      } else if (row.direction === 'redeem') {
        result.investmentRedeem += Number(row.totalAmount)
      }
    }
  })
  
  return result
})

async function loadSummary() {
  loading.value = true
  try {
    const data = await getSummary({
      startDate: filters.value.startDate || undefined,
      endDate: filters.value.endDate || undefined
    })
    summary.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/household-bills')
}

function formatAmount(v) {
  if (v === null || v === undefined) return '0.00'
  const num = Number(v)
  if (Number.isNaN(num)) return v
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function displayCategory(c) {
  if (c === 'investment') return '投资'
  if (c === 'ordinary') return '普通'
  return c
}

function displayDirection(d) {
  const map = {
    expense: '支出',
    income: '收入',
    buy: '买入',
    redeem: '赎回'
  }
  return map[d] || d
}

// 自动加载
loadSummary()
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.clickable-title {
  margin: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: color 0.2s;
}

.clickable-title:hover {
  color: #2563eb;
}

.back-icon {
  font-size: 1.2rem;
}

.subtitle {
  color: #6b7280;
  font-size: 1rem;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.filters {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

label {
  display: flex;
  flex-direction: column;
  font-size: 0.9rem;
  gap: 0.25rem;
}

input,
button {
  padding: 0.45rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}

button {
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.summary-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.overview-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  text-align: center;
}

.card-label {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.card-value {
  font-size: 1.5rem;
  font-weight: 600;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid #e5e7eb;
  padding: 0.5rem 0.6rem;
  text-align: left;
}

th {
  background: #f9fafb;
}

.empty {
  color: #9ca3af;
  text-align: center;
  padding: 2rem;
}

.neg {
  color: #dc2626;
}

.pos {
  color: #16a34a;
}
</style>
