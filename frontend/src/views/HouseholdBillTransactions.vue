<template>
  <div class="page">
    <div class="header">
      <h2>家庭账单</h2>
      <button class="btn-secondary" @click="goToSummary">查看统计</button>
    </div>

    <section class="card">
      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.value"
          :class="['tab', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 上传账单 Tab -->
      <div v-show="activeTab === 'upload'" class="tab-content">
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
          <div>账户名：{{ uploadResult.accountName || '-' }}</div>
          <div>状态：{{ uploadResult.status }}</div>
          <div>总行数：{{ uploadResult.totalRows }}，插入：{{ uploadResult.insertedRows }}，去重：{{ uploadResult.dedupRows }}，失败：{{ uploadResult.failedRows }}</div>
          <div v-if="uploadResult.errorMsg">错误：{{ uploadResult.errorMsg }}</div>
        </div>
      </div>

      <!-- 普通流水 Tab -->
      <div v-show="activeTab === 'ordinary'" class="tab-content">
        <h3>普通流水查询</h3>
        <div class="filters">
          <label>
            开始日期
            <input v-model="ordinaryFilters.startDate" type="date" />
          </label>
          <label>
            结束日期
            <input v-model="ordinaryFilters.endDate" type="date" />
          </label>
          <label>
            方向
            <select v-model="ordinaryFilters.direction">
              <option value="">全部</option>
              <option value="expense">支出</option>
              <option value="income">收入</option>
            </select>
          </label>
          <label>
            账户名
            <input v-model="ordinaryFilters.accountName" type="text" placeholder="账户名" />
          </label>
          <label>
            关键词
            <input v-model="ordinaryFilters.keyword" type="text" placeholder="摘要/对手信息" />
          </label>
          <button :disabled="loadingOrdinary" @click="loadOrdinaryTransactions">
            {{ loadingOrdinary ? '加载中...' : '查询' }}
          </button>
        </div>

        <div class="table-wrapper" v-if="ordinaryTransactions.length">
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
                <th>方向</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in ordinaryTransactions" :key="item.id">
                <td>{{ item.txnDate }}</td>
                <td>{{ item.currency }}</td>
                <td :class="item.amount < 0 ? 'neg' : 'pos'">{{ formatAmount(item.amount) }}</td>
                <td>{{ item.accountName || '-' }}</td>
                <td>{{ formatAmount(item.balance) }}</td>
                <td>{{ item.txnTypeRaw }}</td>
                <td>{{ item.counterparty }}</td>
                <td>{{ displayDirection(item.direction) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty">暂无数据</p>
      </div>

      <!-- 投资流水 Tab -->
      <div v-show="activeTab === 'investment'" class="tab-content">
        <h3>投资流水查询</h3>
        <div class="filters">
          <label>
            开始日期
            <input v-model="investmentFilters.startDate" type="date" />
          </label>
          <label>
            结束日期
            <input v-model="investmentFilters.endDate" type="date" />
          </label>
          <label>
            方向
            <select v-model="investmentFilters.direction">
              <option value="">全部</option>
              <option value="buy">买入</option>
              <option value="redeem">赎回</option>
            </select>
          </label>
          <label>
            账户名
            <input v-model="investmentFilters.accountName" type="text" placeholder="账户名" />
          </label>
          <label>
            关键词
            <input v-model="investmentFilters.keyword" type="text" placeholder="摘要/对手信息" />
          </label>
          <button :disabled="loadingInvestment" @click="loadInvestmentTransactions">
            {{ loadingInvestment ? '加载中...' : '查询' }}
          </button>
        </div>

        <div class="table-wrapper" v-if="investmentTransactions.length">
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
                <th>方向</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in investmentTransactions" :key="item.id">
                <td>{{ item.txnDate }}</td>
                <td>{{ item.currency }}</td>
                <td :class="item.amount < 0 ? 'neg' : 'pos'">{{ formatAmount(item.amount) }}</td>
                <td>{{ item.accountName || '-' }}</td>
                <td>{{ formatAmount(item.balance) }}</td>
                <td>{{ item.txnTypeRaw }}</td>
                <td>{{ item.counterparty }}</td>
                <td>{{ displayDirection(item.direction) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty">暂无数据</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { uploadBill, listTransactions } from '../api/householdBills'

const router = useRouter()

const tabs = [
  { label: '上传账单', value: 'upload' },
  { label: '普通流水', value: 'ordinary' },
  { label: '投资流水', value: 'investment' }
]

const activeTab = ref('ordinary')

// 上传相关
const selectedFile = ref(null)
const uploading = ref(false)
const uploadResult = ref(null)
const userId = ref(0)

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

// 普通流水
const ordinaryFilters = ref({
  startDate: defaultDates.startDate,
  endDate: defaultDates.endDate,
  direction: '',
  accountName: '',
  keyword: ''
})
const ordinaryTransactions = ref([])
const loadingOrdinary = ref(false)

// 投资流水
const investmentFilters = ref({
  startDate: defaultDates.startDate,
  endDate: defaultDates.endDate,
  direction: '',
  accountName: '',
  keyword: ''
})
const investmentTransactions = ref([])
const loadingInvestment = ref(false)

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

async function loadOrdinaryTransactions() {
  loadingOrdinary.value = true
  try {
    const data = await listTransactions({
      startDate: ordinaryFilters.value.startDate || undefined,
      endDate: ordinaryFilters.value.endDate || undefined,
      category: 'ordinary',
      direction: ordinaryFilters.value.direction || undefined,
      accountName: ordinaryFilters.value.accountName || undefined,
      keyword: ordinaryFilters.value.keyword || undefined
    })
    ordinaryTransactions.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingOrdinary.value = false
  }
}

async function loadInvestmentTransactions() {
  loadingInvestment.value = true
  try {
    const data = await listTransactions({
      startDate: investmentFilters.value.startDate || undefined,
      endDate: investmentFilters.value.endDate || undefined,
      category: 'investment',
      direction: investmentFilters.value.direction || undefined,
      accountName: investmentFilters.value.accountName || undefined,
      keyword: investmentFilters.value.keyword || undefined
    })
    investmentTransactions.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingInvestment.value = false
  }
}

function goToSummary() {
  router.push('/household-bills/summary')
}

function formatAmount(v) {
  if (v === null || v === undefined) return ''
  const num = Number(v)
  if (Number.isNaN(num)) return v
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.btn-secondary {
  padding: 0.5rem 1rem;
  background: #6b7280;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-secondary:hover {
  opacity: 0.8;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.tab {
  padding: 0.75rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  color: #6b7280;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab:hover {
  color: #2563eb;
}

.tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
  font-weight: 500;
}

.tab-content {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
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
