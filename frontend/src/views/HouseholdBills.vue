<template>
  <div class="page">
    <h2>家庭账单</h2>

    <section class="card">
      <h3>上传账单 PDF</h3>
      <div class="form-row">
        <input type="file" accept="application/pdf" @change="onFileChange" />
        <input
          v-model="userId"
          type="number"
          min="0"
          class="input"
          placeholder="用户ID（可选，默认0）"
        />
        <button :disabled="uploading || !selectedFile" @click="handleUpload">
          {{ uploading ? '上传中...' : '上传' }}
        </button>
      </div>
      <p class="tips">支持单个 PDF，最大 50MB。上传后后台解析，自动去重与分类。</p>
      <div v-if="uploadResult" class="upload-result">
        <div>文件ID：{{ uploadResult.id }}</div>
        <div>状态：{{ uploadResult.status }}</div>
        <div>总行数：{{ uploadResult.totalRows }}，插入：{{ uploadResult.insertedRows }}，去重：{{ uploadResult.dedupRows }}，失败：{{ uploadResult.failedRows }}</div>
        <div v-if="uploadResult.errorMsg">错误：{{ uploadResult.errorMsg }}</div>
      </div>
    </section>

    <section class="card">
      <h3>流水查询</h3>
      <div class="filters">
        <label>
          开始日期
          <input v-model="filters.startDate" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="filters.endDate" type="date" />
        </label>
        <label>
          类型
          <select v-model="filters.category">
            <option value="">全部</option>
            <option value="ordinary">普通</option>
            <option value="investment">投资</option>
          </select>
        </label>
        <label>
          方向
          <select v-model="filters.direction">
            <option value="">全部</option>
            <option value="expense">支出</option>
            <option value="income">收入</option>
            <option value="buy">买入</option>
            <option value="redeem">赎回</option>
          </select>
        </label>
        <label>
          账户名
          <input v-model="filters.accountName" type="text" placeholder="账户名" />
        </label>
        <label>
          关键词
          <input v-model="filters.keyword" type="text" placeholder="摘要/对手信息" />
        </label>
        <button :disabled="loadingList" @click="loadTransactions">
          {{ loadingList ? '加载中...' : '查询' }}
        </button>
      </div>

      <div class="table-wrapper" v-if="transactions.length">
        <table>
          <thead>
            <tr>
              <th>日期</th>
              <th>币种</th>
              <th>金额</th>
              <th>账户名</th>
              <th>余额</th>
              <th>摘要</th>
              <th>对手信息</th>
              <th>类型</th>
              <th>方向</th>
              <th>去重键</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in transactions" :key="item.id">
              <td>{{ item.txnDate }}</td>
              <td>{{ item.currency }}</td>
              <td :class="item.amount < 0 ? 'neg' : 'pos'">{{ formatAmount(item.amount) }}</td>
              <td>{{ item.accountName || '-' }}</td>
              <td>{{ formatAmount(item.balance) }}</td>
              <td>{{ item.txnTypeRaw }}</td>
              <td>{{ item.counterparty }}</td>
              <td>{{ displayCategory(item.category) }}</td>
              <td>{{ displayDirection(item.direction) }}</td>
              <td class="mono">{{ item.dedupKey }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">暂无数据</p>
    </section>

    <section class="card">
      <h3>汇总</h3>
      <div class="filters">
        <label>
          开始日期
          <input v-model="summaryFilters.startDate" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="summaryFilters.endDate" type="date" />
        </label>
        <button :disabled="loadingSummary" @click="loadSummary">
          {{ loadingSummary ? '加载中...' : '汇总' }}
        </button>
      </div>
      <div class="table-wrapper" v-if="summary.length">
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
      <p v-else class="empty">暂无汇总</p>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadBill, getUploadStatus, listTransactions, getSummary } from '../api/householdBills'

const selectedFile = ref(null)
const uploading = ref(false)
const uploadResult = ref(null)
const userId = ref(0)

const filters = ref({
  startDate: '',
  endDate: '',
  category: '',
  direction: '',
  accountName: '',
  keyword: ''
})

const summaryFilters = ref({
  startDate: '',
  endDate: ''
})

const transactions = ref([])
const summary = ref([])
const loadingList = ref(false)
const loadingSummary = ref(false)

function onFileChange(e) {
  const file = e.target.files[0]
  selectedFile.value = file || null
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const res = await uploadBill(selectedFile.value, userId.value || 0)
    uploadResult.value = res
  } catch (e) {
    console.error(e)
  } finally {
    uploading.value = false
  }
}

async function loadTransactions() {
  loadingList.value = true
  try {
    const data = await listTransactions({
      startDate: filters.value.startDate || undefined,
      endDate: filters.value.endDate || undefined,
      category: filters.value.category || undefined,
      direction: filters.value.direction || undefined,
      accountName: filters.value.accountName || undefined,
      keyword: filters.value.keyword || undefined
    })
    transactions.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingList.value = false
  }
}

async function loadSummary() {
  loadingSummary.value = true
  try {
    const data = await getSummary({
      startDate: summaryFilters.value.startDate || undefined,
      endDate: summaryFilters.value.endDate || undefined
    })
    summary.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingSummary.value = false
  }
}

function formatAmount(v) {
  if (v === null || v === undefined) return ''
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
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.form-row {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

label {
  display: flex;
  flex-direction: column;
  font-size: 0.9rem;
  gap: 0.25rem;
}

input,
select,
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

.tips {
  color: #6b7280;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.upload-result {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: #f3f4f6;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  line-height: 1.6;
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
}

.neg {
  color: #dc2626;
}

.pos {
  color: #16a34a;
}

.mono {
  font-family: monospace;
  font-size: 0.85rem;
}
</style>
