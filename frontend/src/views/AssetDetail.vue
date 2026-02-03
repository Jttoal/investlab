<template>
  <div class="asset-detail" v-if="assetDetail">
    <div class="header">
      <div>
        <h1>{{ assetDetail.asset.name }} ({{ assetDetail.asset.symbol }})</h1>
        <p class="subtitle">市场: {{ assetDetail.asset.market }} | 策略: {{ assetDetail.strategy?.name || '—' }}</p>
      </div>
      <router-link class="btn btn--secondary" :to="`/strategies/${assetDetail.asset.strategyId}`">返回策略</router-link>
    </div>

    <div class="card controls">
      <div class="control-group">
        <label>周期</label>
        <select v-model="period" @change="loadBundle">
          <option value="day">日线</option>
          <option value="week">周线</option>
          <option value="month">月线</option>
          <option value="year">年线</option>
        </select>
      </div>
      <div class="control-group">
        <label>开始时间</label>
        <input type="date" v-model="start" @change="loadBundle" />
        <label>结束时间</label>
        <input type="date" v-model="end" @change="loadBundle" />
      </div>
      <div class="control-group indicators">
        <span>均线</span>
        <label><input type="checkbox" v-model="showMa1" @change="renderChart"> MA{{ effectiveMaConfig.ma1 }}</label>
        <label><input type="checkbox" v-model="showMa2" @change="renderChart"> MA{{ effectiveMaConfig.ma2 }}</label>
        <label><input type="checkbox" v-model="showMa3" @change="renderChart"> MA{{ effectiveMaConfig.ma3 }}</label>
        <label><input type="checkbox" v-model="showMa4" @change="renderChart"> MA{{ effectiveMaConfig.ma4 }}</label>
      </div>
      <div class="control-group indicators">
        <span>布林线</span>
        <label><input type="checkbox" v-model="showBoll" @change="renderChart"> 显示布林</label>
      </div>
      <div class="control-group">
        <button class="btn btn--secondary btn--sm" @click="showConfigModal = true">配置均线</button>
      </div>
    </div>

    <div class="card">
      <div class="holding-summary" v-if="bundle?.holding">
        <div>持仓：{{ bundle.holding.holding }} 股</div>
        <div>均价：{{ formatMoney(bundle.holding.avgCost) }}</div>
        <div>现价：{{ formatMoney(bundle.holding.currentPrice) }}</div>
        <div>市值：{{ formatMoney(bundle.holding.marketValue) }}</div>
        <div :class="getProfitClass(bundle.holding.profitLoss)">盈亏：{{ formatMoney(bundle.holding.profitLoss) }}</div>
        <div :class="getProfitClass(bundle.holding.profitLoss)">盈亏率：{{ formatPercent(bundle.holding.profitLossPercent) }}</div>
      </div>
      <div ref="chartRef" class="chart"></div>
    </div>

    <div class="card lists">
      <div class="lists__col">
        <h3>观点（时间范围内）</h3>
        <ul v-if="bundle?.viewpoints?.length">
          <li v-for="v in bundle.viewpoints" :key="v.id">
            <div class="vp-title" @click="openViewpoint(v)">{{ v.viewpointDate }}｜{{ v.title }}</div>
            <div class="vp-tag">{{ v.tag || '无标签' }}</div>
          </li>
        </ul>
        <div v-else class="empty-state">暂无观点</div>
      </div>
      <div class="lists__col">
        <h3>交易（时间范围内）</h3>
        <ul v-if="bundle?.trades?.length">
          <li v-for="t in bundle.trades" :key="t.id">
            {{ t.tradeDate }}｜{{ getTradeTypeLabel(t.type) }} {{ t.quantity }} @ {{ formatMoney(t.price) }} 备注：{{ t.note || '-' }}
          </li>
        </ul>
        <div v-else class="empty-state">暂无交易</div>
      </div>
    </div>

    <div v-if="showViewpointDialog" class="modal-overlay" @click.self="closeViewpointDialog">
      <div class="modal large">
        <div class="modal__header">
          <h2 class="modal__title">观点详情</h2>
          <button class="modal__close" @click="closeViewpointDialog">&times;</button>
        </div>
        <div class="timeline">
          <button 
            v-for="v in viewpoints"
            :key="v.id"
            :class="['timeline__item', v.id === activeViewpoint?.id ? 'active' : '']"
            @click="setActiveViewpoint(v)"
          >
            {{ v.viewpointDate }}
          </button>
        </div>
        <div v-if="activeViewpoint" class="vp-detail">
          <h3>{{ activeViewpoint.title }}</h3>
          <p>日期：{{ activeViewpoint.viewpointDate }} | 标签：{{ activeViewpoint.tag || '—' }}</p>
          <p>摘要：{{ activeViewpoint.summary || '—' }}</p>
          <p>备注：{{ activeViewpoint.remark || '—' }}</p>
          <p v-if="activeViewpoint.link"><a :href="activeViewpoint.link" target="_blank">原文链接</a></p>
        </div>
      </div>
    </div>

    <div v-if="showTradeDialog" class="modal-overlay" @click.self="closeTradeDialog">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">交易详情</h2>
          <button class="modal__close" @click="closeTradeDialog">&times;</button>
        </div>
        <div v-if="activeTrade" class="vp-detail">
          <p>日期：{{ activeTrade.tradeDate }}</p>
          <p>类型：{{ getTradeTypeLabel(activeTrade.type) }}</p>
          <p>价格：{{ formatMoney(activeTrade.price) }}</p>
          <p>数量：{{ activeTrade.quantity }}</p>
          <p>备注：{{ activeTrade.note || '—' }}</p>
        </div>
      </div>
    </div>
    
    <!-- 均线配置模态框 -->
    <div v-if="showConfigModal" class="modal-overlay" @click.self="closeConfigModal">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">自定义均线配置</h2>
          <button class="modal__close" @click="closeConfigModal">&times;</button>
        </div>
        <div class="modal__body">
          <p class="config-note">
            当前使用: {{ currentConfigSource }}
          </p>
          <div class="form-row">
            <label class="form-label">均线1 (天数)</label>
            <input 
              type="number" 
              v-model.number="assetConfig.maConfig.ma1" 
              class="form-input"
              min="1"
            />
          </div>
          <div class="form-row">
            <label class="form-label">均线2 (天数)</label>
            <input 
              type="number" 
              v-model.number="assetConfig.maConfig.ma2" 
              class="form-input"
              min="1"
            />
          </div>
          <div class="form-row">
            <label class="form-label">均线3 (天数)</label>
            <input 
              type="number" 
              v-model.number="assetConfig.maConfig.ma3" 
              class="form-input"
              min="1"
            />
          </div>
          <div class="form-row">
            <label class="form-label">均线4 (天数)</label>
            <input 
              type="number" 
              v-model.number="assetConfig.maConfig.ma4" 
              class="form-input"
              min="1"
            />
          </div>
          <div class="form-actions">
            <button class="btn btn--secondary" @click="clearAssetConfig">使用策略默认</button>
            <button class="btn btn--primary" @click="saveAssetConfig" :disabled="savingConfig">
              {{ savingConfig ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import { getAssetDetail, getAssetKlineBundle, getAssetConfig, updateAssetConfig } from '../api/asset'
import { getStrategyConfig } from '../api/strategy'
import { useRoute } from 'vue-router'

const route = useRoute()
const assetId = Number(route.params.id)

const assetDetail = ref(null)
const bundle = ref(null)
const viewpoints = ref([])
const period = ref('day')
const start = ref(new Date(new Date().setFullYear(new Date().getFullYear() - 2)).toISOString().slice(0, 10))
const end = ref(new Date().toISOString().slice(0, 10))

const showBoll = ref(true)
const showMa1 = ref(true)
const showMa2 = ref(true)
const showMa3 = ref(true)
const showMa4 = ref(false)

// 均线配置相关
const strategyConfig = ref(null)
const assetConfig = ref({
  maConfig: {
    ma1: 51,
    ma2: 120,
    ma3: 250,
    ma4: 850
  }
})
const hasAssetConfig = ref(false)
const showConfigModal = ref(false)
const savingConfig = ref(false)

// 计算有效配置：标的配置 > 策略配置 > 后端返回的配置
const effectiveMaConfig = computed(() => {
  if (bundle.value?.maConfig) {
    return bundle.value.maConfig
  }
  return {
    ma1: 51,
    ma2: 120,
    ma3: 250,
    ma4: 850
  }
})

const currentConfigSource = computed(() => {
  if (hasAssetConfig.value) {
    return '标的自定义配置'
  } else if (strategyConfig.value?.maConfig) {
    return '策略默认配置'
  }
  return '系统默认配置'
})


const chartRef = ref(null)
let chart

const showViewpointDialog = ref(false)
const activeViewpoint = ref(null)
const showTradeDialog = ref(false)
const activeTrade = ref(null)

onMounted(async () => {
  await loadDetail()
  await loadConfigs()
  await loadBundle()
})

watch([showBoll, showMa1, showMa2, showMa3, showMa4], renderChart)

async function loadDetail() {
  assetDetail.value = await getAssetDetail(assetId)
}

async function loadConfigs() {
  try {
    // 加载策略配置
    if (assetDetail.value?.asset?.strategyId) {
      strategyConfig.value = await getStrategyConfig(assetDetail.value.asset.strategyId)
    }
    
    // 加载标的配置
    const config = await getAssetConfig(assetId)
    if (config.maConfig) {
      assetConfig.value = config
      hasAssetConfig.value = true
    } else {
      // 如果没有标的配置，使用策略配置或默认配置
      assetConfig.value = {
        maConfig: strategyConfig.value?.maConfig || {
          ma1: 51,
          ma2: 120,
          ma3: 250,
          ma4: 850
        }
      }
      hasAssetConfig.value = false
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

async function loadBundle() {
  bundle.value = await getAssetKlineBundle(assetId, { period: period.value, start: start.value, end: end.value })
  viewpoints.value = bundle.value.viewpoints || []
  if (viewpoints.value.length) {
    activeViewpoint.value = viewpoints.value[0]
  } else {
    activeViewpoint.value = null
  }
  renderChart()
}

function renderChart() {
  if (!chartRef.value || !bundle.value?.kline?.length) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', () => chart.resize())
  }
  const dates = bundle.value.kline.map(p => p.date)
  const ohlc = bundle.value.kline.map(p => [p.open, p.close, p.low, p.high])
  const series = [
    { type: 'candlestick', name: 'K', data: ohlc }
  ]
  const addLine = (name, field, show) => {
    if (!show) return
    series.push({
      name,
      type: 'line',
      data: bundle.value.kline.map(p => p[field]),
      showSymbol: false,
      smooth: true
    })
  }
  addLine(`MA${effectiveMaConfig.value.ma1}`, 'ma1', showMa1.value)
  addLine(`MA${effectiveMaConfig.value.ma2}`, 'ma2', showMa2.value)
  addLine(`MA${effectiveMaConfig.value.ma3}`, 'ma3', showMa3.value)
  addLine(`MA${effectiveMaConfig.value.ma4}`, 'ma4', showMa4.value)

  if (showBoll.value) {
    series.push({
      name: 'BOLL U',
      type: 'line',
      data: bundle.value.kline.map(p => p.bollUpper),
      showSymbol: false,
      lineStyle: { type: 'dashed' }
    })
    series.push({
      name: 'BOLL M',
      type: 'line',
      data: bundle.value.kline.map(p => p.bollMid),
      showSymbol: false
    })
    series.push({
      name: 'BOLL L',
      type: 'line',
      data: bundle.value.kline.map(p => p.bollLower),
      showSymbol: false,
      lineStyle: { type: 'dashed' }
    })
  }

  const closeMap = new Map(bundle.value.kline.map(p => [p.date, p.close]))
  const vpPoints = (bundle.value.viewpoints || []).map(v => ({
    name: v.title,
    value: '观点',
    id: v.id,
    pointType: 'viewpoint',
    coord: [v.viewpointDate, closeMap.get(v.viewpointDate) || null],
    itemStyle: { color: '#42b983' },
    tooltip: {
      formatter: () => `
        <div style="max-width:260px">
          <b>${v.title}</b><br/>
          日期: ${v.viewpointDate}<br/>
          标签: ${v.tag || '-'}<br/>
          摘要: ${v.summary || '-'}<br/>
          备注: ${v.remark || '-'}
        </div>
      `
    }
  }))
  const tradePoints = (bundle.value.trades || []).map(t => ({
    name: getTradeTypeLabel(t.type),
    value: getTradeTypeLabel(t.type),
    id: t.id,
    pointType: 'trade',
    coord: [t.tradeDate, closeMap.get(t.tradeDate) || null],
    itemStyle: { color: t.type === 'buy' ? '#2b8efc' : '#e24d4d' },
    tooltip: {
      formatter: () => `
        <div style="max-width:260px">
          <b>${getTradeTypeLabel(t.type)}</b><br/>
          日期: ${t.tradeDate}<br/>
          价格: ${formatMoney(t.price)}<br/>
          数量: ${t.quantity}<br/>
          备注: ${t.note || '-'}
        </div>
      `
    }
  }))
  series[0].markPoint = { data: [...vpPoints, ...tradePoints] }

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    xAxis: { type: 'category', data: dates },
    yAxis: { scale: true },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series
  }, { notMerge: true, replaceMerge: ['series'] })

  chart.off('click')
  chart.on('click', params => {
    if (params.componentType === 'markPoint' && params.data?.pointType === 'viewpoint') {
      const vp = viewpoints.value.find(v => v.id === params.data.id)
      if (vp) {
        openViewpoint(vp)
      }
    }
    if (params.componentType === 'markPoint' && params.data?.pointType === 'trade') {
      const t = (bundle.value.trades || []).find(x => x.id === params.data.id)
      if (t) {
        openTrade(t)
      }
    }
  })
}

function openViewpoint(v) {
  activeViewpoint.value = v
  showViewpointDialog.value = true
}
function closeViewpointDialog() {
  showViewpointDialog.value = false
}
function setActiveViewpoint(v) {
  activeViewpoint.value = v
}

function openTrade(t) {
  activeTrade.value = t
  showTradeDialog.value = true
}
function closeTradeDialog() {
  showTradeDialog.value = false
  activeTrade.value = null
}

function closeConfigModal() {
  showConfigModal.value = false
}

async function saveAssetConfig() {
  try {
    savingConfig.value = true
    await updateAssetConfig(assetId, assetConfig.value)
    hasAssetConfig.value = true
    alert('配置保存成功')
    closeConfigModal()
    await loadBundle() // 重新加载数据以应用新配置
  } catch (error) {
    console.error('保存标的配置失败:', error)
    alert('配置保存失败: ' + (error.message || '未知错误'))
  } finally {
    savingConfig.value = false
  }
}

async function clearAssetConfig() {
  if (!confirm('确定清除标的自定义配置，使用策略默认配置？')) {
    return
  }
  try {
    savingConfig.value = true
    await updateAssetConfig(assetId, {})
    hasAssetConfig.value = false
    // 恢复为策略配置或默认配置
    assetConfig.value = {
      maConfig: strategyConfig.value?.maConfig || {
        ma1: 51,
        ma2: 120,
        ma3: 250,
        ma4: 850
      }
    }
    alert('已清除标的自定义配置')
    closeConfigModal()
    await loadBundle() // 重新加载数据以应用新配置
  } catch (error) {
    console.error('清除标的配置失败:', error)
    alert('操作失败: ' + (error.message || '未知错误'))
  } finally {
    savingConfig.value = false
  }
}

function formatMoney(value) {
  if (value === null || value === undefined) return '-'
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
}
function formatPercent(value) {
  if (value === null || value === undefined) return '-'
  return `${value >= 0 ? '+' : ''}${formatMoney(value)}%`
}
function getProfitClass(value) {
  return value >= 0 ? 'profit-positive' : 'profit-negative'
}
function getTradeTypeLabel(type) {
  const labels = { buy: '买入', sell: '卖出', dividend: '分红' }
  return labels[type] || type
}
</script>

<style scoped>
.asset-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.subtitle {
  color: #6c757d;
}
.controls {
  display: flex;
  gap: 1.5rem;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}
.control-group {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}
.indicators label {
  font-weight: 400;
}
.chart {
  width: 100%;
  height: 520px;
}
.holding-summary {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}
.lists {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}
.lists__col {
  flex: 1;
  min-width: 300px;
}
.vp-title {
  cursor: pointer;
  color: #2b8efc;
}
.vp-tag {
  color: #6c757d;
  font-size: 0.875rem;
}
.timeline {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
.timeline__item {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  background: #f8f9fa;
  cursor: pointer;
}
.timeline__item.active {
  background: #42b983;
  color: #fff;
  border-color: #42b983;
}
.vp-detail p {
  margin: 0.25rem 0;
}
.profit-positive { color: #28a745; }
.profit-negative { color: #dc3545; }
.modal .vp-detail {
  line-height: 1.5;
}
.config-note {
  padding: 0.75rem;
  background: #f8f9fa;
  border-left: 3px solid #42b983;
  margin-bottom: 1rem;
  color: #6c757d;
}
.form-row {
  margin-bottom: 1rem;
}
.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #2c3e50;
}
.form-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}
.form-input:focus {
  outline: none;
  border-color: #42b983;
}
.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
.btn--sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}
</style>
