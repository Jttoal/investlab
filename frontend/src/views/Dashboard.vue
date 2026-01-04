<template>
  <div class="dashboard">
    <h1 class="dashboard__title">投资仪表盘</h1>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="dashboardData">
      <!-- 概览卡片 -->
      <div class="dashboard__overview">
        <div class="stat-card">
          <div class="stat-card__label">账户数量</div>
          <div class="stat-card__value">{{ dashboardData.totalAccounts }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">策略数量</div>
          <div class="stat-card__value">{{ dashboardData.totalStrategies }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">标的数量</div>
          <div class="stat-card__value">{{ dashboardData.totalAssets }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">交易记录</div>
          <div class="stat-card__value">{{ dashboardData.totalTrades }}</div>
        </div>
      </div>

      <!-- 策略汇总 -->
      <div class="card">
        <h2 class="card__title">策略汇总</h2>
        <table class="table" v-if="dashboardData.strategySummaries.length > 0">
          <thead>
            <tr>
              <th>策略名称</th>
              <th>类型</th>
              <th>标的数</th>
              <th>交易数</th>
              <th>总投入</th>
              <th>当前市值</th>
              <th>盈亏</th>
              <th>盈亏率</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="strategy in dashboardData.strategySummaries" :key="strategy.strategyId">
              <td>{{ strategy.strategyName }}</td>
              <td>
                <span class="badge badge--info">{{ getStrategyTypeLabel(strategy.strategyType) }}</span>
              </td>
              <td>{{ strategy.assetCount }}</td>
              <td>{{ strategy.tradeCount }}</td>
              <td>{{ formatMoney(strategy.totalInvested) }}</td>
              <td>{{ formatMoney(strategy.totalValue) }}</td>
              <td :class="getProfitClass(strategy.profitLoss)">
                {{ formatMoney(strategy.profitLoss) }}
              </td>
              <td :class="getProfitClass(strategy.profitLoss)">
                {{ formatPercent(strategy.profitLossPercent) }}
              </td>
              <td>
                <button 
                  class="btn btn--primary btn--sm" 
                  @click="goToStrategy(strategy.strategyId)"
                >
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无策略数据</div>
      </div>

      <!-- 账户汇总 -->
      <div class="card">
        <h2 class="card__title">账户汇总</h2>
        <table class="table" v-if="dashboardData.accountSummaries.length > 0">
          <thead>
          <tr>
            <th>账户名称</th>
            <th>券商</th>
            <th>余额</th>
            <th>币种</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="account in dashboardData.accountSummaries" :key="account.accountId">
            <td>{{ account.accountName }}</td>
            <td>{{ account.broker }}</td>
            <td>{{ formatMoney(account.balance) }}</td>
            <td>{{ account.currency }}</td>
          </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无账户数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '../stores/dashboard'
import { storeToRefs } from 'pinia'

const router = useRouter()
const dashboardStore = useDashboardStore()
const { dashboardData, loading } = storeToRefs(dashboardStore)

onMounted(() => {
  dashboardStore.fetchDashboard()
})

function formatMoney(value) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

function formatPercent(value) {
  return `${value >= 0 ? '+' : ''}${formatMoney(value)}%`
}

function getProfitClass(value) {
  return value >= 0 ? 'profit-positive' : 'profit-negative'
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

function goToStrategy(id) {
  router.push(`/strategies/${id}`)
}
</script>

<style scoped>
.dashboard__title {
  margin-bottom: 2rem;
  color: #2c3e50;
}

.dashboard__overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.stat-card__label {
  color: #6c757d;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.stat-card__value {
  font-size: 2rem;
  font-weight: bold;
  color: #2c3e50;
}

.profit-positive {
  color: #28a745;
  font-weight: 500;
}

.profit-negative {
  color: #dc3545;
  font-weight: 500;
}

.btn--sm {
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
}
</style>
