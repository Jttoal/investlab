<template>
  <div class="strategy-detail">
    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="detail">
      <!-- 策略基本信息 -->
      <div class="card strategy-header">
        <div class="strategy-header__top">
          <div>
            <h1 class="strategy-header__title">{{ detail.strategy.name }}</h1>
            <span class="badge badge--info">{{ getStrategyTypeLabel(detail.strategy.type) }}</span>
          </div>
          <button class="btn btn--secondary" @click="goBack">返回</button>
        </div>
        <p class="strategy-header__note">{{ detail.strategy.goalNote || '暂无目标说明' }}</p>
        
        <!-- 策略汇总数据 -->
        <div class="strategy-stats">
          <div class="stat-item">
            <span class="stat-item__label">标的数量</span>
            <span class="stat-item__value">{{ detail.summary.assetCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-item__label">交易次数</span>
            <span class="stat-item__value">{{ detail.summary.tradeCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-item__label">总投入</span>
            <span class="stat-item__value">{{ formatMoney(detail.summary.totalInvested) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-item__label">当前市值</span>
            <span class="stat-item__value">{{ formatMoney(detail.summary.totalValue) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-item__label">盈亏</span>
            <span class="stat-item__value" :class="getProfitClass(detail.summary.profitLoss)">
              {{ formatMoney(detail.summary.profitLoss) }}
            </span>
          </div>
          <div class="stat-item">
            <span class="stat-item__label">盈亏率</span>
            <span class="stat-item__value" :class="getProfitClass(detail.summary.profitLoss)">
              {{ formatPercent(detail.summary.profitLossPercent) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 标签页 -->
      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.key"
          class="tabs__item"
          :class="{ 'tabs__item--active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 标的列表 -->
      <div v-show="activeTab === 'assets'" class="card">
        <div class="card-header">
          <h2 class="card__title">持仓标的</h2>
          <button class="btn btn--primary" @click="showAssetModal = true">添加标的</button>
        </div>
        
        <table class="table" v-if="detail.assets.length > 0">
          <thead>
            <tr>
              <th>代码</th>
              <th>名称</th>
              <th>市场</th>
              <th>持仓</th>
              <th>成本价</th>
              <th>当前价</th>
              <th>市值</th>
              <th>盈亏</th>
              <th>盈亏率</th>
              <th>提醒状态</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.assets" :key="item.asset.id">
              <td>{{ item.asset.symbol }}</td>
              <td>
                <router-link :to="`/assets/${item.asset.id}`">{{ item.asset.name }}</router-link>
              </td>
              <td>{{ item.asset.market }}</td>
              <td>{{ item.holding }}</td>
              <td>{{ formatMoney(item.avgCost) }}</td>
              <td>{{ formatMoney(item.currentPrice) }}</td>
              <td>{{ formatMoney(item.marketValue) }}</td>
              <td :class="getProfitClass(item.profitLoss)">
                {{ formatMoney(item.profitLoss) }}
              </td>
              <td :class="getProfitClass(item.profitLoss)">
                {{ formatPercent(item.profitLossPercent) }}
              </td>
              <td>
                <span v-if="item.alertStatus === 'below_target'" class="badge badge--warning">
                  低于目标价
                </span>
                <span v-else-if="item.alertStatus === 'above_target'" class="badge badge--success">
                  高于目标价
                </span>
                <span v-else class="badge badge--info">正常</span>
              </td>
              <td>
                <div class="note-cell" @mouseenter="showNote(item.asset.id, item.asset.note)" @mouseleave="hideNote">
                  <span>{{ truncateNote(item.asset.note) }}</span>
                  <div v-if="hoverNoteId === item.asset.id" class="note-popover">
                    {{ item.asset.note || '暂无备注' }}
                  </div>
                </div>
              </td>
              <td>
                <button class="btn btn--secondary btn--sm" @click="editAsset(item.asset)">
                  编辑
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无标的</div>
      </div>

      <!-- 交易记录 -->
      <div v-show="activeTab === 'trades'" class="card">
        <div class="card-header">
          <h2 class="card__title">交易记录</h2>
          <button class="btn btn--primary" @click="showTradeModal = true">记录交易</button>
        </div>
        
        <table class="table" v-if="detail.recentTrades.length > 0">
          <thead>
            <tr>
              <th>日期</th>
              <th>类型</th>
              <th>标的</th>
              <th>价格</th>
              <th>数量</th>
              <th>手续费</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="trade in detail.recentTrades" :key="trade.id">
              <td>{{ formatDate(trade.tradeDate) }}</td>
              <td>
                <span class="badge" :class="getTradeTypeBadge(trade.type)">
                  {{ getTradeTypeLabel(trade.type) }}
                </span>
              </td>
              <td>{{ getAssetName(trade.assetId) }}</td>
              <td>{{ formatMoney(trade.price) }}</td>
              <td>{{ trade.quantity }}</td>
              <td>{{ formatMoney(trade.fee) }}</td>
              <td>{{ trade.note || '-' }}</td>
              <td class="table-actions">
                <button class="btn btn--secondary btn--sm" @click="startEditTrade(trade)">编辑</button>
                <button class="btn btn--danger btn--sm" @click="deleteTrade(trade.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无交易记录</div>
      </div>

      <!-- 观点记录 -->
      <div v-show="activeTab === 'viewpoints'" class="card">
        <div class="card-header">
          <h2 class="card__title">大V观点</h2>
          <button class="btn btn--primary" @click="showViewpointModal = true">添加观点</button>
        </div>
        
        <div v-if="detail.recentViewpoints.length > 0" class="viewpoints-list">
          <div v-for="viewpoint in detail.recentViewpoints" :key="viewpoint.id" class="viewpoint-item">
            <div class="viewpoint-item__header">
              <h3 class="viewpoint-item__title">{{ viewpoint.title }}</h3>
              <span v-if="viewpoint.tag" class="badge badge--info">{{ viewpoint.tag }}</span>
            </div>
            <p class="viewpoint-item__summary">{{ viewpoint.summary }}</p>
            <p class="viewpoint-item__remark" v-if="viewpoint.remark">备注：{{ viewpoint.remark }}</p>
            <div class="viewpoint-item__footer">
              <span class="viewpoint-item__date">{{ formatDate(viewpoint.viewpointDate) }}</span>
              <a v-if="viewpoint.link" :href="viewpoint.link" target="_blank" class="viewpoint-item__link">
                查看原文 →
              </a>
              <div class="viewpoint-item__actions">
                <button class="btn btn--secondary btn--sm" @click="startEditViewpoint(viewpoint)">编辑</button>
                <button class="btn btn--danger btn--sm" @click="deleteViewpoint(viewpoint.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">暂无观点记录</div>
      </div>
      
      <!-- 配置 -->
      <div v-show="activeTab === 'config'" class="card">
        <div class="card-header">
          <h2 class="card__title">策略配置</h2>
        </div>
        
        <div class="config-section">
          <h3 class="config-section__title">均线设置</h3>
          <p class="config-section__desc">设置该策略的默认均线参数，标的可单独覆盖</p>
          
          <div class="config-form">
            <div class="form-row">
              <label class="form-label">均线1 (天数)</label>
              <input 
                type="number" 
                v-model.number="strategyConfig.maConfig.ma1" 
                class="form-input"
                min="1"
              />
            </div>
            <div class="form-row">
              <label class="form-label">均线2 (天数)</label>
              <input 
                type="number" 
                v-model.number="strategyConfig.maConfig.ma2" 
                class="form-input"
                min="1"
              />
            </div>
            <div class="form-row">
              <label class="form-label">均线3 (天数)</label>
              <input 
                type="number" 
                v-model.number="strategyConfig.maConfig.ma3" 
                class="form-input"
                min="1"
              />
            </div>
            <div class="form-row">
              <label class="form-label">均线4 (天数)</label>
              <input 
                type="number" 
                v-model.number="strategyConfig.maConfig.ma4" 
                class="form-input"
                min="1"
              />
            </div>
          </div>
          
          <button class="btn btn--primary" @click="saveStrategyConfig" :disabled="savingConfig">
            {{ savingConfig ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 标的模态框 -->
    <AssetModal 
      v-if="showAssetModal" 
      :strategy-id="strategyId"
      :asset="editingAsset"
      @close="closeAssetModal"
      @saved="handleAssetSaved"
    />

    <!-- 交易模态框 -->
    <TradeModal 
      v-if="showTradeModal"
      :strategy-id="strategyId"
      :assets="detail?.assets || []"
      :trade="editingTrade"
      @close="closeTradeModal"
      @saved="handleTradeSaved"
    />

    <!-- 观点模态框 -->
    <ViewpointModal 
      v-if="showViewpointModal"
      :strategy-id="strategyId"
      :assets="detail?.assets || []"
      :viewpoint="editingViewpoint"
      @close="closeViewpointModal"
      @saved="handleViewpointSaved"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDashboardStore } from '../stores/dashboard'
import { storeToRefs } from 'pinia'
import AssetModal from '../components/AssetModal.vue'
import TradeModal from '../components/TradeModal.vue'
import ViewpointModal from '../components/ViewpointModal.vue'
import * as tradeApi from '../api/trade'
import * as viewpointApi from '../api/viewpoint'
import * as strategyApi from '../api/strategy'

const route = useRoute()
const router = useRouter()
const dashboardStore = useDashboardStore()
const { strategyDetail: detail, loading } = storeToRefs(dashboardStore)

const strategyId = computed(() => parseInt(route.params.id))
const activeTab = ref('assets')
const showAssetModal = ref(false)
const showTradeModal = ref(false)
const showViewpointModal = ref(false)
const editingAsset = ref(null)
const editingTrade = ref(null)
const editingViewpoint = ref(null)
const hoverNoteId = ref(null)
const strategyConfig = ref({
  maConfig: {
    ma1: 51,
    ma2: 120,
    ma3: 250,
    ma4: 850
  }
})
const savingConfig = ref(false)

const tabs = [
  { key: 'assets', label: '持仓标的' },
  { key: 'trades', label: '交易记录' },
  { key: 'viewpoints', label: '大V观点' },
  { key: 'config', label: '配置' }
]

onMounted(() => {
  syncTabFromRoute()
  loadDetail()
  loadStrategyConfig()
})

function loadDetail() {
  dashboardStore.fetchStrategyDetail(strategyId.value)
}

async function loadStrategyConfig() {
  try {
    const config = await strategyApi.getStrategyConfig(strategyId.value)
    if (config.maConfig) {
      strategyConfig.value = config
    }
  } catch (error) {
    console.error('加载策略配置失败:', error)
  }
}

async function saveStrategyConfig() {
  try {
    savingConfig.value = true
    await strategyApi.updateStrategyConfig(strategyId.value, strategyConfig.value)
    alert('配置保存成功')
  } catch (error) {
    console.error('保存策略配置失败:', error)
    alert('配置保存失败: ' + (error.message || '未知错误'))
  } finally {
    savingConfig.value = false
  }
}

function goBack() {
  router.push('/strategies')
}

function syncTabFromRoute() {
  const path = route.path
  if (path.endsWith('/trade')) activeTab.value = 'trades'
  else if (path.endsWith('/viewpoints')) activeTab.value = 'viewpoints'
  else if (path.endsWith('/config')) activeTab.value = 'config'
  else activeTab.value = 'assets'
}

watch(
  () => route.fullPath,
  () => syncTabFromRoute()
)

function switchTab(key) {
  activeTab.value = key
  const base = `/strategies/${strategyId.value}`
  const suffix = key === 'trades' ? '/trade' : 
                 key === 'viewpoints' ? '/viewpoints' : 
                 key === 'config' ? '/config' : '/assets'
  router.replace(base + suffix)
}

function editAsset(asset) {
  editingAsset.value = asset
  showAssetModal.value = true
}

function showNote(assetId, note) {
  if (!note) return
  hoverNoteId.value = assetId
}

function hideNote() {
  hoverNoteId.value = null
}

function truncateNote(note) {
  if (!note) return '—'
  return note.length > 12 ? note.slice(0, 12) + '…' : note
}

function closeAssetModal() {
  showAssetModal.value = false
  editingAsset.value = null
}

function handleAssetSaved() {
  closeAssetModal()
  loadDetail()
}

function handleTradeSaved() {
  closeTradeModal()
  loadDetail()
}

function handleViewpointSaved() {
  closeViewpointModal()
  loadDetail()
}

function getAssetName(assetId) {
  const asset = detail.value?.assets.find(a => a.asset.id === assetId)
  return asset ? `${asset.asset.name}(${asset.asset.symbol})` : '-'
}

function startEditTrade(trade) {
  editingTrade.value = trade
  showTradeModal.value = true
}

async function deleteTrade(id) {
  if (confirm('确定删除该交易记录？')) {
    await tradeApi.deleteTrade(id)
    loadDetail()
  }
}

function closeTradeModal() {
  showTradeModal.value = false
  editingTrade.value = null
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

function getTradeTypeLabel(type) {
  const labels = {
    'buy': '买入',
    'sell': '卖出',
    'dividend': '分红'
  }
  return labels[type] || type
}

function getTradeTypeBadge(type) {
  const badges = {
    'buy': 'badge--success',
    'sell': 'badge--danger',
    'dividend': 'badge--info'
  }
  return badges[type] || 'badge--info'
}

function formatMoney(value) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

function formatPercent(value) {
  return `${value >= 0 ? '+' : ''}${formatMoney(value)}%`
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

function getProfitClass(value) {
  return value >= 0 ? 'profit-positive' : 'profit-negative'
}

function startEditViewpoint(viewpoint) {
  editingViewpoint.value = viewpoint
  showViewpointModal.value = true
}

async function deleteViewpoint(id) {
  if (confirm(`确定要删除观点吗?`)) {
    await viewpointApi.deleteViewpoint(id)
    loadDetail()
  }
}

function closeViewpointModal() {
  showViewpointModal.value = false
  editingViewpoint.value = null
}
</script>

<style scoped>
.strategy-header {
  margin-bottom: 1rem;
}

.strategy-header__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.strategy-header__title {
  margin: 0 1rem 0.5rem 0;
  display: inline-block;
}

.strategy-header__note {
  color: #6c757d;
  margin: 0 0 1.5rem 0;
}

.strategy-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-item__label {
  font-size: 0.875rem;
  color: #6c757d;
  margin-bottom: 0.25rem;
}

.stat-item__value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #2c3e50;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  border-bottom: 2px solid #eee;
}

.tabs__item {
  padding: 0.75rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  color: #6c757d;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tabs__item:hover {
  color: #2c3e50;
}

.tabs__item--active {
  color: #42b983;
  border-bottom-color: #42b983;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.card-header .card__title {
  margin: 0;
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

.viewpoints-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.viewpoint-item {
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 4px;
}

.viewpoint-item__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.viewpoint-item__title {
  margin: 0;
  font-size: 1.125rem;
  color: #2c3e50;
}

.viewpoint-item__summary {
  color: #6c757d;
  margin: 0 0 0.75rem 0;
}

.viewpoint-item__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid #eee;
}

.viewpoint-item__date {
  font-size: 0.875rem;
  color: #6c757d;
}

.viewpoint-item__link {
  color: #42b983;
  text-decoration: none;
  font-size: 0.875rem;
}

.viewpoint-item__link:hover {
  text-decoration: underline;
}

.viewpoint-item__actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.table-actions {
  display: flex;
  gap: 0.5rem;
}

.note-cell {
  position: relative;
}

.note-popover {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 10;
  background: #fff;
  border: 1px solid #ddd;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  padding: 0.5rem;
  width: 240px;
  max-height: 200px;
  overflow: auto;
  margin-top: 4px;
  border-radius: 4px;
}

.config-section {
  padding: 1rem 0;
}

.config-section__title {
  margin: 0 0 0.5rem 0;
  font-size: 1.125rem;
  color: #2c3e50;
}

.config-section__desc {
  margin: 0 0 1rem 0;
  color: #6c757d;
  font-size: 0.875rem;
}

.config-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-row {
  display: flex;
  flex-direction: column;
}

.form-label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #2c3e50;
}

.form-input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-input:focus {
  outline: none;
  border-color: #42b983;
}
</style>
