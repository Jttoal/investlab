<template>
  <div class="page">
    <div class="header">
      <h2 class="page-title">
        家庭账单
        <span class="info-icon" @click="showInfoModal = true" title="查看说明">ⓘ</span>
      </h2>
      <span class="subtitle">统计分析</span>
    </div>

    <section class="card">
      <!-- 类型切换器和筛选器 -->
      <div class="top-filters">
        <div class="filters-left">
          <div class="type-switcher">
            <label>
              <input type="radio" value="ordinary" v-model="filters.category" @change="onCategoryChange" />
              <span>普通</span>
            </label>
            <label>
              <input type="radio" value="investment" v-model="filters.category" @change="onCategoryChange" />
              <span>投资</span>
            </label>
          </div>

          <div class="account-filter">
            <span class="filter-label-inline">账户筛选</span>
            <div class="account-dropdown">
              <button class="dropdown-toggle" @click="toggleAccountDropdown">
                {{ selectedAccountsDisplay }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div v-if="showAccountDropdown" class="dropdown-menu">
                <label class="dropdown-item">
                  <input type="checkbox" :checked="isAllAccountsSelected" @change="toggleAllAccounts" />
                  <span>全部</span>
                </label>
                <label v-for="name in accountNames" :key="name" class="dropdown-item">
                  <input type="checkbox" :value="name" v-model="selectedAccountNames" />
                  <span>{{ name }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div class="filters-right">
          <div class="import-dropdown">
            <button class="import-btn" @click="triggerFileInput">
              <span>📁</span> 导入账单
            </button>
            <button class="import-dropdown-toggle" @click="toggleImportMenu">▼</button>
            <div v-if="showImportMenu" class="import-menu">
              <div class="import-menu-item" @click="openRulesManager">
                <span>⚙️</span> 导入规则管理
              </div>
            </div>
          </div>
          <input 
            ref="fileInput" 
            type="file" 
            accept=".pdf" 
            style="display: none" 
            @change="handleFileSelect"
          />
        </div>
      </div>

      <!-- 标签页 -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 按月汇总 -->
      <div v-if="activeTab === 'monthly'" class="tab-content">
        <!-- 期间选择器 - 横向布局 -->
        <div class="period-selector-inline">
          <label class="inline-label">
            <span>选择月份</span>
            <input type="month" v-model="monthlyPeriod" @change="loadMonthlySummary" lang="zh-CN" />
          </label>
          <div class="quick-links">
            <a href="javascript:void(0)" @click="changeMonth(-1)">上个月</a>
            <span class="separator">|</span>
            <a href="javascript:void(0)" @click="changeMonth(1)">下个月</a>
          </div>
          <button class="view-transactions-btn" @click="openTransactionsModal('monthly')">
            查看流水
          </button>
        </div>

        <!-- 收支概览 -->
        <div v-if="!loading && monthlySummary" class="summary-container">
          <div class="overview-cards">
            <div class="overview-card">
              <div class="card-label">{{ filters.category === 'ordinary' ? '总收入' : '总赎回' }}</div>
              <div class="card-value pos">
                <span class="amount-main">{{ formatAmountCard(monthlySummary.overview.totalIncome).main }}</span>
                <span v-if="formatAmountCard(monthlySummary.overview.totalIncome).hint" class="amount-hint">{{ formatAmountCard(monthlySummary.overview.totalIncome).hint }}</span>
              </div>
            </div>
            <div class="overview-card">
              <div class="card-label">{{ filters.category === 'ordinary' ? '总支出' : '总买入' }}</div>
              <div class="card-value neg">
                <span class="amount-main">{{ formatAmountCard(monthlySummary.overview.totalExpense).main }}</span>
                <span v-if="formatAmountCard(monthlySummary.overview.totalExpense).hint" class="amount-hint">{{ formatAmountCard(monthlySummary.overview.totalExpense).hint }}</span>
              </div>
            </div>
            <div class="overview-card">
              <div class="card-label">剩余</div>
              <div class="card-value" :class="monthlySummary.overview.balance >= 0 ? 'pos' : 'neg'">
                <span class="amount-main">{{ formatAmountCard(monthlySummary.overview.balance).main }}</span>
                <span v-if="formatAmountCard(monthlySummary.overview.balance).hint" class="amount-hint">{{ formatAmountCard(monthlySummary.overview.balance).hint }}</span>
              </div>
            </div>
          </div>

          <!-- 收支明细 -->
          <h4>收支明细</h4>
          <div class="dual-table">
            <div class="table-section">
              <h5>{{ filters.category === 'ordinary' ? '支出' : '买入' }}</h5>
              <table>
                <thead>
                  <tr>
                    <th>交易摘要</th>
                    <th>金额</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in monthlySummary.expenseDetails" :key="idx">
                    <td class="clickable-summary" @click="quickSearchSummary(item.summary, 'monthly', true)" title="点击查看流水">{{ item.summary }}</td>
                    <td class="neg amount-cell">
                      <span class="amount-left">{{ formatAmountTable(item.amount).main }}</span>
                      <span v-if="formatAmountTable(item.amount).hint" class="amount-right">{{ formatAmountTable(item.amount).hint }}</span>
                    </td>
                  </tr>
                  <tr v-if="!monthlySummary.expenseDetails.length">
                    <td colspan="2" class="empty">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="table-section">
              <h5>{{ filters.category === 'ordinary' ? '收入' : '赎回' }}</h5>
              <table>
                <thead>
                  <tr>
                    <th>交易摘要</th>
                    <th>金额</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in monthlySummary.incomeDetails" :key="idx">
                    <td class="clickable-summary" @click="quickSearchSummary(item.summary, 'monthly', false)" title="点击查看流水">{{ item.summary }}</td>
                    <td class="pos amount-cell">
                      <span class="amount-left">{{ formatAmountTable(item.amount).main }}</span>
                      <span v-if="formatAmountTable(item.amount).hint" class="amount-right">{{ formatAmountTable(item.amount).hint }}</span>
                    </td>
                  </tr>
                  <tr v-if="!monthlySummary.incomeDetails.length">
                    <td colspan="2" class="empty">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <p v-if="loading" class="loading">加载中...</p>
        <p v-if="!loading && !monthlySummary" class="empty">暂无数据</p>
      </div>

      <!-- 按年汇总 -->
      <div v-if="activeTab === 'yearly'" class="tab-content">
        <!-- 期间选择器 - 横向布局 -->
        <div class="period-selector-inline">
          <label class="inline-label">
            <span>选择年份</span>
            <input type="number" v-model.number="yearlyPeriod" @change="loadYearlySummary" min="2000" max="2099" />
          </label>
          <div class="quick-links">
            <a href="javascript:void(0)" @click="changeYear(-1)">上一年</a>
            <span class="separator">|</span>
            <a href="javascript:void(0)" @click="changeYear(1)">下一年</a>
          </div>
          <button class="view-transactions-btn" @click="openTransactionsModal('yearly')">
            查看流水
          </button>
        </div>

        <!-- 收支概览 -->
        <div v-if="!loading && yearlySummary" class="summary-container">
          <div class="overview-cards">
            <div class="overview-card">
              <div class="card-label">{{ filters.category === 'ordinary' ? '总收入' : '总赎回' }}</div>
              <div class="card-value pos">
                <span class="amount-main">{{ formatAmountCard(yearlySummary.overview.totalIncome).main }}</span>
                <span v-if="formatAmountCard(yearlySummary.overview.totalIncome).hint" class="amount-hint">{{ formatAmountCard(yearlySummary.overview.totalIncome).hint }}</span>
              </div>
            </div>
            <div class="overview-card">
              <div class="card-label">{{ filters.category === 'ordinary' ? '总支出' : '总买入' }}</div>
              <div class="card-value neg">
                <span class="amount-main">{{ formatAmountCard(yearlySummary.overview.totalExpense).main }}</span>
                <span v-if="formatAmountCard(yearlySummary.overview.totalExpense).hint" class="amount-hint">{{ formatAmountCard(yearlySummary.overview.totalExpense).hint }}</span>
              </div>
            </div>
            <div class="overview-card">
              <div class="card-label">剩余</div>
              <div class="card-value" :class="yearlySummary.overview.balance >= 0 ? 'pos' : 'neg'">
                <span class="amount-main">{{ formatAmountCard(yearlySummary.overview.balance).main }}</span>
                <span v-if="formatAmountCard(yearlySummary.overview.balance).hint" class="amount-hint">{{ formatAmountCard(yearlySummary.overview.balance).hint }}</span>
              </div>
            </div>
          </div>

          <!-- 月度趋势图 -->
          <div v-if="monthlyTrend" class="chart-section">
            <h4>月度趋势</h4>
            <div class="chart-wrapper" style="position: relative;">
              <div ref="monthlyTrendChart" style="width: 100%; height: 350px;"></div>
              <div v-if="!monthlyTrend || !monthlyTrend.data || monthlyTrend.data.length === 0" 
                   style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: center; background: white;">
                <p class="empty">暂无数据</p>
              </div>
            </div>
          </div>

          <!-- 收支明细 -->
          <h4>收支明细</h4>
          <div class="dual-table">
            <div class="table-section">
              <h5>{{ filters.category === 'ordinary' ? '支出' : '买入' }}</h5>
              <table>
                <thead>
                  <tr>
                    <th>交易摘要</th>
                    <th>金额</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in yearlySummary.expenseDetails" :key="idx">
                    <td class="clickable-summary" @click="quickSearchSummary(item.summary, 'yearly', true)" title="点击查看流水">{{ item.summary }}</td>
                    <td class="neg amount-cell">
                      <span class="amount-left">{{ formatAmountTable(item.amount).main }}</span>
                      <span v-if="formatAmountTable(item.amount).hint" class="amount-right">{{ formatAmountTable(item.amount).hint }}</span>
                    </td>
                  </tr>
                  <tr v-if="!yearlySummary.expenseDetails.length">
                    <td colspan="2" class="empty">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="table-section">
              <h5>{{ filters.category === 'ordinary' ? '收入' : '赎回' }}</h5>
              <table>
                <thead>
                  <tr>
                    <th>交易摘要</th>
                    <th>金额</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in yearlySummary.incomeDetails" :key="idx">
                    <td class="clickable-summary" @click="quickSearchSummary(item.summary, 'yearly', false)" title="点击查看流水">{{ item.summary }}</td>
                    <td class="pos amount-cell">
                      <span class="amount-left">{{ formatAmountTable(item.amount).main }}</span>
                      <span v-if="formatAmountTable(item.amount).hint" class="amount-right">{{ formatAmountTable(item.amount).hint }}</span>
                    </td>
                  </tr>
                  <tr v-if="!yearlySummary.incomeDetails.length">
                    <td colspan="2" class="empty">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <p v-if="loading" class="loading">加载中...</p>
        <p v-if="!loading && !yearlySummary" class="empty">暂无数据</p>
      </div>

      <!-- 年度趋势 -->
      <div v-if="activeTab === 'yearly-trend'" class="tab-content">
        <h4>多年趋势对比</h4>
        <div class="chart-wrapper" style="position: relative;">
          <div ref="yearlyTrendChart" style="width: 100%; height: 400px;"></div>
          
          <div v-if="!yearlyTrend || !yearlyTrend.data || yearlyTrend.data.length === 0" 
               style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: center; background: white;">
            <p class="empty">暂无数据</p>
          </div>
        </div>

        <p v-if="loading" class="loading">加载中...</p>
      </div>
    </section>

    <!-- 信息提示弹窗 -->
    <div v-if="showInfoModal" class="modal-overlay" @click.self="showInfoModal = false">
      <div class="modal-content info-modal">
        <div class="modal-header">
          <h3>家庭账单使用说明</h3>
          <button class="modal-close" @click="showInfoModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="info-section">
            <h4>💡 核心概念</h4>
            <p>家庭账单帮助您在<strong>同一个银行账户</strong>中，通过逻辑分类管理不同用途的资金。</p>
          </div>

          <div class="info-diagram">
            <div class="bank-account">
              <div class="account-title">🏦 银行实体账户</div>
              <div class="account-subtitle">总余额 = 现金账户 + 投资账户</div>
              
              <div class="logic-zones">
                <div class="logic-zone ordinary">
                  <div class="zone-header">
                    <span class="zone-icon">💰</span>
                    <span class="zone-title">逻辑分区1：现金账户</span>
                  </div>
                  <div class="zone-tag">日常消费资金池</div>
                  <ul class="zone-items">
                    <li>记录：工资、红包、日常消费</li>
                    <li>作用：现金流管理、预算控制</li>
                  </ul>
                  <div class="zone-examples">
                    <span class="example-tag">工资</span>
                    <span class="example-tag">消费</span>
                    <span class="example-tag">缴费</span>
                  </div>
                </div>

                <div class="logic-zone investment">
                  <div class="zone-header">
                    <span class="zone-icon">📈</span>
                    <span class="zone-title">逻辑分区2：投资账户</span>
                  </div>
                  <div class="zone-tag">投资运作资金池</div>
                  <ul class="zone-items">
                    <li>记录：转入本金、投资收益、赎回操作</li>
                    <li>作用：投资追踪、收益分析</li>
                  </ul>
                  <div class="zone-examples">
                    <span class="example-tag">月月宝</span>
                    <span class="example-tag">日日宝</span>
                    <span class="example-tag">基金</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="info-section">
            <h4>📝 使用流程</h4>
            <ol>
              <li><strong>导入账单</strong>：点击右上角"导入账单"按钮，上传银行PDF流水文件</li>
              <li><strong>自动分类</strong>：系统根据交易摘要自动分为"普通"和"投资"两类</li>
              <li><strong>查看统计</strong>：按月/按年查看收支汇总、趋势图表</li>
              <li><strong>编辑摘要</strong>：点击"查看流水"可手动调整交易摘要，优化统计分组</li>
            </ol>
          </div>

          <div class="info-section">
            <h4>🎯 关键特性</h4>
            <ul class="feature-list">
              <li><strong>智能去重</strong>：自动识别重复导入的交易记录</li>
              <li><strong>灵活分类</strong>：支持手动编辑交易摘要，按需调整分类</li>
              <li><strong>多维统计</strong>：按账户、类型、时间多维度分析</li>
              <li><strong>趋势可视化</strong>：直观展示收支变化趋势</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传结果提示弹窗 -->
    <div v-if="showUploadResult" class="modal-overlay" @click.self="closeUploadResult">
      <div class="modal-content upload-result-modal">
        <div class="modal-header">
          <h3>{{ uploadResult.status === 'success' ? '✅ 导入成功' : '❌ 导入失败' }}</h3>
          <button class="modal-close" @click="closeUploadResult">×</button>
        </div>
        <div class="modal-body">
          <div v-if="uploadResult.status === 'success'" class="result-success">
            <p><strong>文件名：</strong>{{ uploadResult.fileName }}</p>
            <p><strong>账户：</strong>{{ uploadResult.accountName }}</p>
            <p><strong>总记录数：</strong>{{ uploadResult.totalRows }}</p>
            <p><strong>新增记录：</strong>{{ uploadResult.insertedRows }}</p>
            <p><strong>重复记录：</strong>{{ uploadResult.dedupRows }}</p>
            <p v-if="uploadResult.failedRows > 0" class="warning">
              <strong>失败记录：</strong>{{ uploadResult.failedRows }}
            </p>
          </div>
          <div v-else class="result-error">
            <p>{{ uploadResult.errorMsg || '上传失败，请重试' }}</p>
          </div>
          <div class="countdown">
            {{ countdown }} 秒后自动关闭
          </div>
        </div>
      </div>
    </div>

    <!-- 流水查询弹窗 -->
    <div v-if="showTransactionsModal" class="modal-overlay" @click.self="closeTransactionsModal">
      <div class="modal-content transactions-modal">
        <div class="modal-body">
          <!-- 查询条件 -->
          <div class="query-filters">
            <!-- 第一行：日期、类型、方向、账户 -->
            <div class="filter-row">
              <label class="filter-item">
                <span class="filter-label">开始日期</span>
                <input type="date" v-model="transactionQuery.startDate" class="filter-input" />
              </label>
              <label class="filter-item">
                <span class="filter-label">结束日期</span>
                <input type="date" v-model="transactionQuery.endDate" class="filter-input" />
              </label>
              <label class="filter-item">
                <span class="filter-label">类型</span>
                <select v-model="transactionQuery.category" class="filter-select">
                  <option value="">全部</option>
                  <option value="ordinary">普通</option>
                  <option value="investment">投资</option>
                </select>
              </label>
              <label class="filter-item">
                <span class="filter-label">钱款方向</span>
                <select v-model="transactionQuery.amountDirection" class="filter-select">
                  <option value="">全部</option>
                  <option value="positive">收入 (+)</option>
                  <option value="negative">支出 (-)</option>
                </select>
              </label>
              <label class="filter-item">
                <span class="filter-label">账户</span>
                <select v-model="transactionQuery.accountName" class="filter-select">
                  <option value="">全部账户</option>
                  <option v-for="name in accountNames" :key="name" :value="name">{{ name }}</option>
                </select>
              </label>
            </div>
            
            <!-- 第二行：交易摘要、交易对手、查询按钮、关闭按钮 -->
            <div class="filter-row">
              <label class="filter-item filter-item-wide">
                <span class="filter-label">交易摘要</span>
                <div class="input-with-clear">
                  <input type="text" v-model="transactionQuery.keyword" placeholder="输入关键词" class="filter-input-wide" />
                  <span v-if="transactionQuery.keyword" class="clear-icon" @click="clearKeyword" title="清除">✕</span>
                </div>
              </label>
              <label class="filter-item filter-item-wide">
                <span class="filter-label">交易对手</span>
                <div class="input-with-clear">
                  <input type="text" v-model="transactionQuery.counterparty" placeholder="输入关键词" class="filter-input-wide" />
                  <span v-if="transactionQuery.counterparty" class="clear-icon" @click="clearCounterparty" title="清除">✕</span>
                </div>
              </label>
              <button class="query-btn" @click="queryTransactions">查询</button>
              <button class="query-btn close-btn-inline" @click="closeTransactionsModal">关闭</button>
            </div>
          </div>

          <!-- 批量操作工具栏 -->
          <div v-if="selectedTransactions.length > 0" class="batch-toolbar">
            <span class="batch-info">已选择 {{ selectedTransactions.length }} 条记录</span>
            <input 
              type="text" 
              v-model="batchEditSummary" 
              placeholder="输入新的交易摘要" 
              class="batch-input"
            />
            <button @click="batchUpdateSummary" class="batch-save-btn">批量修改</button>
            <button @click="clearSelection" class="batch-cancel-btn">取消选择</button>
          </div>

          <!-- 流水列表 -->
          <div class="transactions-table-wrapper">
            <table class="transactions-table">
              <thead>
                <tr>
                  <th style="width: 40px;">
                    <input 
                      type="checkbox" 
                      :checked="isAllSelected" 
                      @change="toggleSelectAll"
                      class="checkbox"
                    />
                  </th>
                  <th style="width: 100px;">日期</th>
                  <th style="width: 240px;">
                    交易摘要<span class="hint-text">（双击可修改）</span>
                  </th>
                  <th style="width: 150px;">对手信息</th>
                  <th style="width: 120px;">金额</th>
                  <th style="width: 120px;">余额</th>
                  <th style="width: 100px;">账户</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="txn in transactions" :key="txn.id">
                  <td>
                    <input 
                      type="checkbox" 
                      :checked="selectedTransactions.includes(txn.id)"
                      @change="toggleSelect(txn.id)"
                      class="checkbox"
                    />
                  </td>
                  <td>{{ txn.txnDate }}</td>
                  <td class="summary-cell">
                    <div v-if="editingTxnId === txn.id" class="summary-edit-mode">
                      <input 
                        v-model="editingSummary" 
                        @keyup.enter="saveSummary(txn.id)"
                        @keyup.esc="cancelEdit"
                        class="summary-input"
                        ref="summaryInput"
                      />
                      <div class="edit-actions">
                        <button @click="saveSummary(txn.id)" class="save-btn" title="保存">✓</button>
                        <button @click="cancelEdit" class="cancel-btn" title="取消">✕</button>
                      </div>
                    </div>
                    <div v-else 
                         class="summary-display" 
                         @dblclick="startEdit(txn)"
                         @mouseenter="showQuickFilter(txn)" 
                         @mouseleave="hideQuickFilter">
                      <div class="summary-text">
                        <span v-if="txn.txnTypeRaw !== txn.txnTypeRawOriginal" class="original-summary">
                          {{ txn.txnTypeRawOriginal }}
                        </span>
                        <span class="current-summary">{{ txn.txnTypeRaw }}</span>
                      </div>
                      <div v-if="hoveredTxnId === txn.id" 
                           class="quick-filter-tooltip"
                           @mouseenter="showQuickFilter(txn)"
                           @mouseleave="hideQuickFilter">
                        <button @click.stop="applyQuickFilter(txn.txnTypeRaw)" class="quick-filter-btn">
                          🔍 快速过滤
                        </button>
                      </div>
                    </div>
                  </td>
                  <td>{{ txn.counterparty }}</td>
                  <td :class="txn.amount >= 0 ? 'pos' : 'neg'">{{ formatAmount(txn.amount) }}</td>
                  <td>{{ formatAmount(txn.balance) }}</td>
                  <td>{{ txn.accountName }}</td>
                </tr>
                <tr v-if="!transactions.length">
                  <td colspan="7" class="empty">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 导入规则管理弹窗 -->
    <div v-if="showRulesManager" class="modal-overlay" @click.self="showRulesManager = false">
      <div class="modal-content rules-manager-modal">
        <div class="modal-header">
          <h3>导入规则管理</h3>
          <button class="modal-close" @click="showRulesManager = false">×</button>
        </div>
        <div class="modal-body">
          <!-- 分类规则 -->
          <div class="rules-section">
            <h4>投资分类规则</h4>
            <p class="rules-desc">匹配以下关键词的交易将被分类为"投资"类别</p>
            
            <div class="rule-group">
              <label class="rule-label">交易摘要关键词</label>
              <div class="keywords-list">
                <div v-for="(keyword, idx) in rules.summaryKeywords" :key="idx" class="keyword-tag">
                  <span>{{ keyword }}</span>
                  <button @click="removeSummaryKeyword(idx)" class="remove-btn">×</button>
                </div>
                <input 
                  v-model="newSummaryKeyword" 
                  @keyup.enter="addSummaryKeyword"
                  placeholder="输入关键词后按回车添加"
                  class="keyword-input"
                />
              </div>
            </div>

            <div class="rule-group">
              <label class="rule-label">对手信息关键词</label>
              <div class="keywords-list">
                <div v-for="(keyword, idx) in rules.counterpartyKeywords" :key="idx" class="keyword-tag">
                  <span>{{ keyword }}</span>
                  <button @click="removeCounterpartyKeyword(idx)" class="remove-btn">×</button>
                </div>
                <input 
                  v-model="newCounterpartyKeyword" 
                  @keyup.enter="addCounterpartyKeyword"
                  placeholder="输入关键词后按回车添加"
                  class="keyword-input"
                />
              </div>
            </div>
          </div>

          <!-- 摘要替换规则 -->
          <div class="rules-section">
            <h4>交易摘要替换规则</h4>
            <p class="rules-desc">根据交易摘要或对手信息自动替换为指定的新摘要</p>
            
            <div class="replace-rules-list">
              <div v-for="(rule, idx) in rules.replaceRules" :key="idx" class="replace-rule-item">
                <select v-model="rule.matchType" class="match-type-select">
                  <option value="summary">匹配交易摘要</option>
                  <option value="counterparty">匹配对手信息</option>
                  <option value="both">同时匹配</option>
                </select>
                <input 
                  v-model="rule.pattern" 
                  :placeholder="rule.matchType === 'counterparty' ? '对手关键字' : '摘要关键字'"
                  class="rule-input"
                />
                <input 
                  v-if="rule.matchType === 'both'"
                  v-model="rule.counterpartyPattern" 
                  placeholder="对手关键字"
                  class="rule-input"
                />
                <span class="arrow">→</span>
                <input 
                  v-model="rule.replacement" 
                  placeholder="新摘要"
                  class="rule-input"
                />
                <button @click="removeReplaceRule(idx)" class="remove-btn-large">删除</button>
              </div>
              <button @click="addReplaceRule" class="add-rule-btn">+ 添加规则</button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="rules-actions">
            <button @click="saveRulesData" class="save-rules-btn">保存规则</button>
            <button @click="previewRerunData" class="rerun-btn">预览重跑</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 规则重跑预览弹窗 -->
    <div v-if="showRerunPreview" class="modal-overlay" @click.self="showRerunPreview = false">
      <div class="modal-content rerun-preview-modal">
        <div class="modal-header">
          <h3>规则重跑预览</h3>
          <button class="modal-close" @click="showRerunPreview = false">×</button>
        </div>
        <div class="modal-body">
          <p class="preview-info">
            将根据当前规则重新分类和修改交易摘要，共找到 <strong>{{ rerunChanges.length }}</strong> 条需要修改的记录
          </p>
          
          <div class="changes-table-wrapper">
            <table class="changes-table">
              <thead>
                <tr>
                  <th style="width: 40px;">
                    <input 
                      type="checkbox" 
                      :checked="isAllChangesSelected" 
                      @change="toggleSelectAllChanges"
                      class="checkbox"
                    />
                  </th>
                  <th>日期</th>
                  <th>金额</th>
                  <th>交易对手</th>
                  <th>原始摘要</th>
                  <th>当前摘要</th>
                  <th>变更内容</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="change in rerunChanges" :key="change.id">
                  <td>
                    <input 
                      type="checkbox" 
                      :checked="selectedChanges.includes(change.id)"
                      @change="toggleSelectChange(change.id)"
                      class="checkbox"
                    />
                  </td>
                  <td>{{ change.txnDate }}</td>
                  <td :class="parseFloat(change.amount) < 0 ? 'neg' : 'pos'">
                    {{ parseFloat(change.amount).toFixed(2) }}
                  </td>
                  <td>{{ change.counterparty || '-' }}</td>
                  <td>{{ change.originalSummary }}</td>
                  <td>{{ change.currentSummary }}</td>
                  <td>
                    <div class="change-details">
                      <div v-if="change.categoryChange" class="change-item">
                        <span class="change-label">分类:</span>
                        <span class="old-value">{{ change.oldCategory }}</span>
                        <span class="arrow">→</span>
                        <span class="new-value">{{ change.newCategory }}</span>
                      </div>
                      <div v-if="change.summaryChange" class="change-item">
                        <span class="change-label">摘要:</span>
                        <span class="old-value">{{ change.oldSummary }}</span>
                        <span class="arrow">→</span>
                        <span class="new-value">{{ change.newSummary }}</span>
                      </div>
                    </div>
                  </td>
                </tr>
                <tr v-if="!rerunChanges.length">
                  <td colspan="7" class="empty">没有需要修改的记录</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="preview-actions">
            <button @click="confirmRerun" class="confirm-rerun-btn" :disabled="selectedChanges.length === 0">
              确认修改 ({{ selectedChanges.length }})
            </button>
            <button @click="showRerunPreview = false" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  getMonthlySummary,
  getYearlySummary,
  getMonthlyTrend,
  getYearlyTrend,
  listTransactions,
  uploadBill,
  getUploadStatus,
  updateTransactionSummary,
  getAccountNames,
  getRules,
  saveRules,
  previewRerun,
  executeRerun
} from '../api/householdBills'

const router = useRouter()

const filters = ref({
  category: 'ordinary'
})

// 弹窗状态
const showInfoModal = ref(false)
const showUploadResult = ref(false)
const showTransactionsModal = ref(false)
const showImportMenu = ref(false)
const showRulesManager = ref(false)
const showRerunPreview = ref(false)

// 上传相关
const fileInput = ref(null)
const uploadResult = ref({})
const countdown = ref(10)
let countdownTimer = null

// 流水查询相关
const transactionQuery = ref({
  startDate: '',
  endDate: '',
  category: '',
  accountName: '',
  keyword: '',
  counterparty: '',
  amountDirection: '' // 新增：钱款方向，''=全部, 'positive'=收入(+), 'negative'=支出(-)
})
const transactions = ref([])
const editingTxnId = ref(null)
const editingSummary = ref('')
const hoveredTxnId = ref(null)
const summaryInput = ref(null)

// 批量操作相关
const selectedTransactions = ref([])
const batchEditSummary = ref('')

// 规则管理相关
const rules = ref({
  summaryKeywords: [],
  counterpartyKeywords: [],
  replaceRules: []
})
const newSummaryKeyword = ref('')
const newCounterpartyKeyword = ref('')

// 规则重跑相关
const rerunChanges = ref([])
const selectedChanges = ref([])

const selectedAccountNames = ref([])
const accountNames = ref([])
const showAccountDropdown = ref(false)

// 计算属性：显示选中的账户
const selectedAccountsDisplay = computed(() => {
  if (selectedAccountNames.value.length === 0) {
    return '全部账户'
  }
  if (selectedAccountNames.value.length === accountNames.value.length) {
    return '全部账户'
  }
  if (selectedAccountNames.value.length === 1) {
    return selectedAccountNames.value[0]
  }
  return `已选 ${selectedAccountNames.value.length} 个账户`
})

const isAllAccountsSelected = computed(() => {
  return selectedAccountNames.value.length === 0 || 
         selectedAccountNames.value.length === accountNames.value.length
})

// 是否全选
const isAllSelected = computed(() => {
  return transactions.value.length > 0 && 
         selectedTransactions.value.length === transactions.value.length
})

// 是否全选变更
const isAllChangesSelected = computed(() => {
  return rerunChanges.value.length > 0 && 
         selectedChanges.value.length === rerunChanges.value.length
})

// 切换下拉框显示
function toggleAccountDropdown() {
  showAccountDropdown.value = !showAccountDropdown.value
}

// 切换全选
function toggleAllAccounts() {
  if (isAllAccountsSelected.value) {
    selectedAccountNames.value = []
  } else {
    selectedAccountNames.value = [...accountNames.value]
  }
}

// 点击外部关闭下拉框
function handleClickOutside(event) {
  const dropdown = document.querySelector('.account-dropdown')
  if (dropdown && !dropdown.contains(event.target)) {
    showAccountDropdown.value = false
  }
}

const tabs = [
  { id: 'monthly', label: '按月汇总' },
  { id: 'yearly', label: '按年汇总' },
  { id: 'yearly-trend', label: '多年趋势' }
]

const activeTab = ref('monthly')
const loading = ref(false)

// 期间选择器 - 默认上个月和今年
const today = new Date()
const currentYear = today.getFullYear()
const lastMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1)
const lastMonthStr = `${lastMonth.getFullYear()}-${String(lastMonth.getMonth() + 1).padStart(2, '0')}`

const monthlyPeriod = ref(lastMonthStr)
const yearlyPeriod = ref(currentYear)
// 如果是1月份，默认显示去年的趋势（因为今年可能还没数据）
const trendYear = ref(today.getMonth() === 0 ? currentYear - 1 : currentYear)

// 月份快捷操作
function changeMonth(delta) {
  const [year, month] = monthlyPeriod.value.split('-').map(Number)
  const date = new Date(year, month - 1 + delta, 1)
  monthlyPeriod.value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  loadMonthlySummary()
}

// 年份快捷操作
function changeYear(delta) {
  yearlyPeriod.value += delta
  loadYearlySummary()
  loadMonthlyTrend()  // 同时重新加载月度趋势
}

// 趋势年份快捷操作
function changeTrendYear(delta) {
  trendYear.value += delta
  loadMonthlyTrend()
}

// 数据
const monthlySummary = ref(null)
const yearlySummary = ref(null)
const monthlyTrend = ref(null)
const yearlyTrend = ref(null)

// 图表实例
const monthlyTrendChart = ref(null)
const yearlyTrendChart = ref(null)
let monthlyChartInstance = null
let yearlyChartInstance = null

// 加载账户名列表
async function loadAccountNames() {
  try {
    const data = await getAccountNames()
    accountNames.value = data
  } catch (e) {
    console.error('加载账户名失败:', e)
  }
}

// 文件上传相关方法
function triggerFileInput() {
  fileInput.value.click()
}

async function handleFileSelect(event) {
  const file = event.target.files[0]
  if (!file) return

  try {
    const result = await uploadBill(file)
    // 轮询查询上传状态
    await pollUploadStatus(result.id)
  } catch (e) {
    uploadResult.value = {
      status: 'failed',
      errorMsg: e.response?.data?.message || '上传失败'
    }
    showUploadResult.value = true
    startCountdown()
  } finally {
    // 清空文件输入
    event.target.value = ''
  }
}

async function pollUploadStatus(uploadId) {
  const maxAttempts = 60 // 最多轮询60次（60秒）
  let attempts = 0

  const poll = async () => {
    try {
      const status = await getUploadStatus(uploadId)
      
      if (status.status === 'success' || status.status === 'failed') {
        uploadResult.value = status
        showUploadResult.value = true
        startCountdown()
        
        // 刷新账户列表和当前页面数据
        if (status.status === 'success') {
          await loadAccountNames()
          if (activeTab.value === 'monthly') {
            loadMonthlySummary()
          } else if (activeTab.value === 'yearly') {
            loadYearlySummary()
            loadMonthlyTrend()
          }
        }
        return
      }

      attempts++
      if (attempts < maxAttempts) {
        setTimeout(poll, 1000) // 1秒后再次轮询
      } else {
        uploadResult.value = {
          status: 'failed',
          errorMsg: '上传超时，请稍后查看'
        }
        showUploadResult.value = true
        startCountdown()
      }
    } catch (e) {
      console.error('查询上传状态失败:', e)
    }
  }

  poll()
}

function startCountdown() {
  countdown.value = 10
  if (countdownTimer) clearInterval(countdownTimer)
  
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      closeUploadResult()
    }
  }, 1000)
}

function closeUploadResult() {
  showUploadResult.value = false
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 流水弹窗相关方法
function openTransactionsModal(type) {
  // 根据当前tab自动填充查询条件
  if (type === 'monthly') {
    const [year, month] = monthlyPeriod.value.split('-')
    const startDate = `${year}-${month}-01`
    const lastDay = new Date(year, month, 0).getDate()
    const endDate = `${year}-${month}-${String(lastDay).padStart(2, '0')}`
    
    transactionQuery.value = {
      startDate,
      endDate,
      category: filters.value.category,
      accountName: selectedAccountNames.value.length === 1 ? selectedAccountNames.value[0] : '',
      keyword: ''
    }
  } else if (type === 'yearly') {
    transactionQuery.value = {
      startDate: `${yearlyPeriod.value}-01-01`,
      endDate: `${yearlyPeriod.value}-12-31`,
      category: filters.value.category,
      accountName: selectedAccountNames.value.length === 1 ? selectedAccountNames.value[0] : '',
      keyword: ''
    }
  }
  
  showTransactionsModal.value = true
  queryTransactions()
}

function closeTransactionsModal() {
  showTransactionsModal.value = false
  transactions.value = []
  editingTxnId.value = null
}

async function queryTransactions() {
  try {
    const params = {}
    if (transactionQuery.value.startDate) params.startDate = transactionQuery.value.startDate
    if (transactionQuery.value.endDate) params.endDate = transactionQuery.value.endDate
    if (transactionQuery.value.category) params.category = transactionQuery.value.category
    if (transactionQuery.value.accountName) params.accountName = transactionQuery.value.accountName
    if (transactionQuery.value.keyword) params.keyword = transactionQuery.value.keyword
    if (transactionQuery.value.counterparty) params.counterparty = transactionQuery.value.counterparty
    if (transactionQuery.value.amountDirection) params.amountDirection = transactionQuery.value.amountDirection
    
    const data = await listTransactions(params)
    transactions.value = data
  } catch (e) {
    console.error('查询流水失败:', e)
  }
}

// 交易摘要编辑相关方法
function startEdit(txn) {
  editingTxnId.value = txn.id
  editingSummary.value = txn.txnTypeRaw
  nextTick(() => {
    if (summaryInput.value && summaryInput.value[0]) {
      summaryInput.value[0].focus()
    }
  })
}

function cancelEdit() {
  editingTxnId.value = null
  editingSummary.value = ''
}

async function saveSummary(txnId) {
  try {
    await updateTransactionSummary(txnId, editingSummary.value)
    // 更新本地数据
    const txn = transactions.value.find(t => t.id === txnId)
    if (txn) {
      txn.txnTypeRaw = editingSummary.value
    }
    editingTxnId.value = null
    editingSummary.value = ''
    
    // 刷新统计数据
    if (activeTab.value === 'monthly') {
      loadMonthlySummary()
    } else if (activeTab.value === 'yearly') {
      loadYearlySummary()
      loadMonthlyTrend()
    }
  } catch (e) {
    console.error('保存失败:', e)
    alert('保存失败，请重试')
  }
}

let hideQuickFilterTimer = null

function showQuickFilter(txn) {
  // 清除之前的定时器
  if (hideQuickFilterTimer) {
    clearTimeout(hideQuickFilterTimer)
    hideQuickFilterTimer = null
  }
  hoveredTxnId.value = txn.id
}

function hideQuickFilter() {
  // 延迟300ms隐藏，给用户时间移动鼠标到按钮上
  hideQuickFilterTimer = setTimeout(() => {
    hoveredTxnId.value = null
  }, 300)
}

function applyQuickFilter(summary) {
  // 立即隐藏tooltip
  if (hideQuickFilterTimer) {
    clearTimeout(hideQuickFilterTimer)
  }
  hoveredTxnId.value = null
  
  // 应用过滤
  transactionQuery.value.keyword = summary
  queryTransactions()
}

// 清除关键词并刷新
function clearKeyword() {
  transactionQuery.value.keyword = ''
  queryTransactions()
}

// 清除对手信息并刷新
function clearCounterparty() {
  transactionQuery.value.counterparty = ''
  queryTransactions()
}

// 切换单个选择
function toggleSelect(txnId) {
  const index = selectedTransactions.value.indexOf(txnId)
  if (index > -1) {
    selectedTransactions.value.splice(index, 1)
  } else {
    selectedTransactions.value.push(txnId)
  }
}

// 切换全选
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedTransactions.value = []
  } else {
    selectedTransactions.value = transactions.value.map(t => t.id)
  }
}

// 清除选择
function clearSelection() {
  selectedTransactions.value = []
  batchEditSummary.value = ''
}

// 批量更新交易摘要
async function batchUpdateSummary() {
  if (!batchEditSummary.value.trim()) {
    alert('请输入新的交易摘要')
    return
  }
  
  if (selectedTransactions.value.length === 0) {
    alert('请至少选择一条记录')
    return
  }
  
  // 保存选中数量，因为后面会清除选择
  const count = selectedTransactions.value.length
  
  try {
    // 批量更新
    await Promise.all(
      selectedTransactions.value.map(txnId => 
        updateTransactionSummary(txnId, batchEditSummary.value.trim())
      )
    )
    
    // 更新本地数据
    transactions.value.forEach(txn => {
      if (selectedTransactions.value.includes(txn.id)) {
        txn.txnTypeRaw = batchEditSummary.value.trim()
      }
    })
    
    // 刷新统计数据
    if (activeTab.value === 'monthly') {
      loadMonthlySummary()
    } else if (activeTab.value === 'yearly') {
      loadYearlySummary()
      loadMonthlyTrend()
    }
    
    // 清除选择
    clearSelection()
    
    // 显示成功提示（使用之前保存的数量）
    alert(`成功修改 ${count} 条记录`)
  } catch (e) {
    console.error('批量修改失败:', e)
    alert('批量修改失败，请重试')
  }
}

// 导入菜单相关
function toggleImportMenu() {
  showImportMenu.value = !showImportMenu.value
}

function openRulesManager() {
  showImportMenu.value = false
  loadRules()
  showRulesManager.value = true
}

// 加载规则
async function loadRules() {
  try {
    const data = await getRules()
    rules.value = {
      summaryKeywords: data.summaryKeywords || [],
      counterpartyKeywords: data.counterpartyKeywords || [],
      replaceRules: (data.replaceRules || []).map(rule => ({
        pattern: rule.pattern || '',
        replacement: rule.replacement || '',
        matchType: rule.matchType || 'summary',
        counterpartyPattern: rule.counterpartyPattern || ''
      }))
    }
  } catch (e) {
    console.error('加载规则失败:', e)
    rules.value = {
      summaryKeywords: [],
      counterpartyKeywords: [],
      replaceRules: []
    }
  }
}

// 添加摘要关键词
function addSummaryKeyword() {
  const keyword = newSummaryKeyword.value.trim()
  if (keyword && !rules.value.summaryKeywords.includes(keyword)) {
    rules.value.summaryKeywords.push(keyword)
    newSummaryKeyword.value = ''
  }
}

// 删除摘要关键词
function removeSummaryKeyword(index) {
  rules.value.summaryKeywords.splice(index, 1)
}

// 添加对手关键词
function addCounterpartyKeyword() {
  const keyword = newCounterpartyKeyword.value.trim()
  if (keyword && !rules.value.counterpartyKeywords.includes(keyword)) {
    rules.value.counterpartyKeywords.push(keyword)
    newCounterpartyKeyword.value = ''
  }
}

// 删除对手关键词
function removeCounterpartyKeyword(index) {
  rules.value.counterpartyKeywords.splice(index, 1)
}

// 添加替换规则
function addReplaceRule() {
  rules.value.replaceRules.push({
    pattern: '',
    replacement: '',
    matchType: 'summary',
    counterpartyPattern: ''
  })
}

// 删除替换规则
function removeReplaceRule(index) {
  rules.value.replaceRules.splice(index, 1)
}

// 保存规则
async function saveRulesData() {
  try {
    // 过滤掉空的替换规则
    const validReplaceRules = rules.value.replaceRules.filter(
      rule => rule.pattern.trim() && rule.replacement.trim()
    )
    
    await saveRules({
      summaryKeywords: rules.value.summaryKeywords,
      counterpartyKeywords: rules.value.counterpartyKeywords,
      replaceRules: validReplaceRules
    })
    
    alert('规则保存成功，可以继续预览重跑')
    // 不关闭面板，方便用户继续操作重跑
  } catch (e) {
    console.error('保存规则失败:', e)
    alert('保存规则失败，请重试')
  }
}

// 预览重跑
async function previewRerunData() {
  try {
    const data = await previewRerun()
    rerunChanges.value = data
    selectedChanges.value = data.map(c => c.id)
    showRerunPreview.value = true
  } catch (e) {
    console.error('预览失败:', e)
    alert('预览失败，请重试')
  }
}

// 切换选择变更
function toggleSelectChange(changeId) {
  const index = selectedChanges.value.indexOf(changeId)
  if (index > -1) {
    selectedChanges.value.splice(index, 1)
  } else {
    selectedChanges.value.push(changeId)
  }
}

// 切换全选变更
function toggleSelectAllChanges() {
  if (isAllChangesSelected.value) {
    selectedChanges.value = []
  } else {
    selectedChanges.value = rerunChanges.value.map(c => c.id)
  }
}

// 确认重跑
async function confirmRerun() {
  if (selectedChanges.value.length === 0) {
    alert('请至少选择一条记录')
    return
  }
  
  const count = selectedChanges.value.length
  
  try {
    const result = await executeRerun(selectedChanges.value)
    alert(`成功修改 ${result.count} 条记录`)
    showRerunPreview.value = false
    showRulesManager.value = false
    
    // 刷新数据
    if (activeTab.value === 'monthly') {
      loadMonthlySummary()
    } else if (activeTab.value === 'yearly') {
      loadYearlySummary()
      loadMonthlyTrend()
    }
  } catch (e) {
    console.error('执行重跑失败:', e)
    alert('执行失败，请重试')
  }
}

// 快速搜索交易摘要
function quickSearchSummary(summary, periodType, isExpense = null) {
  // 根据期间类型设置日期范围
  if (periodType === 'monthly') {
    const [year, month] = monthlyPeriod.value.split('-')
    const startDate = `${year}-${month}-01`
    const lastDay = new Date(year, month, 0).getDate()
    const endDate = `${year}-${month}-${String(lastDay).padStart(2, '0')}`
    
    transactionQuery.value = {
      startDate,
      endDate,
      category: filters.value.category,
      accountName: selectedAccountNames.value.length === 1 ? selectedAccountNames.value[0] : '',
      keyword: summary,
      counterparty: '',
      amountDirection: isExpense === true ? 'negative' : (isExpense === false ? 'positive' : '')
    }
  } else if (periodType === 'yearly') {
    const year = yearlyPeriod.value
    const startDate = `${year}-01-01`
    const endDate = `${year}-12-31`
    
    transactionQuery.value = {
      startDate,
      endDate,
      category: filters.value.category,
      accountName: selectedAccountNames.value.length === 1 ? selectedAccountNames.value[0] : '',
      keyword: summary,
      counterparty: '',
      amountDirection: isExpense === true ? 'negative' : (isExpense === false ? 'positive' : '')
    }
  }
  
  // 打开流水弹窗并查询
  showTransactionsModal.value = true
  queryTransactions()
}

// 加载按月汇总
async function loadMonthlySummary() {
  loading.value = true
  try {
    const params = {
      month: monthlyPeriod.value,
      category: filters.value.category
    }
    if (selectedAccountNames.value.length > 0 && selectedAccountNames.value[0] !== '') {
      params.accountNames = selectedAccountNames.value
    }
    const data = await getMonthlySummary(params)
    monthlySummary.value = data
  } catch (e) {
    console.error('加载按月汇总失败:', e)
    monthlySummary.value = null
  } finally {
    loading.value = false
  }
}

// 加载按年汇总
async function loadYearlySummary() {
  loading.value = true
  try {
    const params = {
      year: String(yearlyPeriod.value),
      category: filters.value.category
    }
    if (selectedAccountNames.value.length > 0 && selectedAccountNames.value[0] !== '') {
      params.accountNames = selectedAccountNames.value
    }
    const data = await getYearlySummary(params)
    yearlySummary.value = data
  } catch (e) {
    console.error('加载按年汇总失败:', e)
    yearlySummary.value = null
  } finally {
    loading.value = false
  }
}

// 加载月度趋势
async function loadMonthlyTrend() {
  loading.value = true
  // 使用yearlyPeriod作为年份（因为现在在按年汇总tab中）
  const year = String(yearlyPeriod.value)
  console.log('[月度趋势] 开始加载, 年份:', year, '类型:', filters.value.category)
  try {
    const params = {
      year: year,
      category: filters.value.category
    }
    if (selectedAccountNames.value.length > 0 && selectedAccountNames.value[0] !== '') {
      params.accountNames = selectedAccountNames.value
    }
    console.log('[月度趋势] 请求参数:', params)
    const data = await getMonthlyTrend(params)
    console.log('[月度趋势] 返回数据:', data)
    monthlyTrend.value = data
    // 不在这里渲染，让watch来处理
  } catch (e) {
    console.error('[月度趋势] 加载失败:', e)
    monthlyTrend.value = null
  } finally {
    loading.value = false
    console.log('[月度趋势] 加载完成')
  }
}

// 加载年度趋势
async function loadYearlyTrend() {
  loading.value = true
  console.log('[年度趋势] 开始加载, 类型:', filters.value.category)
  try {
    const params = {
      category: filters.value.category
    }
    if (selectedAccountNames.value.length > 0 && selectedAccountNames.value[0] !== '') {
      params.accountNames = selectedAccountNames.value
    }
    console.log('[年度趋势] 请求参数:', params)
    const data = await getYearlyTrend(params)
    console.log('[年度趋势] 返回数据:', data)
    yearlyTrend.value = data
    // 不在这里渲染，让watch来处理
  } catch (e) {
    console.error('[年度趋势] 加载失败:', e)
    yearlyTrend.value = null
  } finally {
    loading.value = false
    console.log('[年度趋势] 加载完成')
  }
}

// 渲染月度趋势图表
function renderMonthlyTrendChart() {
  console.log('[渲染月度趋势] 开始渲染, monthlyTrendChart.value:', monthlyTrendChart.value, 'monthlyTrend.value:', monthlyTrend.value)
  
  if (!monthlyTrendChart.value) {
    console.error('[渲染月度趋势] DOM元素不存在')
    return
  }
  
  if (!monthlyTrend.value || !monthlyTrend.value.data || monthlyTrend.value.data.length === 0) {
    console.error('[渲染月度趋势] 数据不存在或为空')
    return
  }

  try {
    if (monthlyChartInstance) {
      console.log('[渲染月度趋势] 销毁旧图表实例')
      monthlyChartInstance.dispose()
    }

    console.log('[渲染月度趋势] 初始化ECharts')
    monthlyChartInstance = echarts.init(monthlyTrendChart.value)

    const months = monthlyTrend.value.data.map(d => d.period)
    const incomeData = monthlyTrend.value.data.map(d => Number(d.income))
    const expenseData = monthlyTrend.value.data.map(d => Number(d.expense))
    const balanceData = monthlyTrend.value.data.map(d => Number(d.balance))

    console.log('[渲染月度趋势] 数据点:', { months, incomeData, expenseData, balanceData })

    const isOrdinary = filters.value.category === 'ordinary'

    const option = {
      title: {
        text: `${yearlyPeriod.value}年${isOrdinary ? '普通' : '投资'}月度收支趋势`,
        left: 'center',
        textStyle: {
          fontSize: 14,
          fontWeight: 'normal'
        }
      },
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: [isOrdinary ? '收入' : '赎回', isOrdinary ? '支出' : '买入', '剩余'],
        top: 30
      },
      xAxis: {
        type: 'category',
        data: months
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: isOrdinary ? '收入' : '赎回',
          type: 'bar',
          data: incomeData,
          itemStyle: { color: '#16a34a' }
        },
        {
          name: isOrdinary ? '支出' : '买入',
          type: 'bar',
          data: expenseData,
          itemStyle: { color: '#dc2626' }
        },
        {
          name: '剩余',
          type: 'line',
          data: balanceData,
          itemStyle: { color: '#2563eb' }
        }
      ]
    }

    console.log('[渲染月度趋势] 设置图表配置')
    monthlyChartInstance.setOption(option)
    console.log('[渲染月度趋势] 渲染完成')
  } catch (error) {
    console.error('[渲染月度趋势] 渲染出错:', error)
  }
}

// 渲染年度趋势图表
function renderYearlyTrendChart() {
  console.log('[渲染年度趋势] 开始渲染, yearlyTrendChart.value:', yearlyTrendChart.value, 'yearlyTrend.value:', yearlyTrend.value)
  
  if (!yearlyTrendChart.value) {
    console.error('[渲染年度趋势] DOM元素不存在')
    return
  }
  
  if (!yearlyTrend.value || !yearlyTrend.value.data || yearlyTrend.value.data.length === 0) {
    console.error('[渲染年度趋势] 数据不存在或为空')
    return
  }

  try {
    if (yearlyChartInstance) {
      console.log('[渲染年度趋势] 销毁旧图表实例')
      yearlyChartInstance.dispose()
    }

    console.log('[渲染年度趋势] 初始化ECharts')
    yearlyChartInstance = echarts.init(yearlyTrendChart.value)

    const years = yearlyTrend.value.data.map(d => d.period)
    const incomeData = yearlyTrend.value.data.map(d => Number(d.income))
    const expenseData = yearlyTrend.value.data.map(d => Number(d.expense))
    const balanceData = yearlyTrend.value.data.map(d => Number(d.balance))

    console.log('[渲染年度趋势] 数据点:', { years, incomeData, expenseData, balanceData })

    const isOrdinary = filters.value.category === 'ordinary'

    const option = {
      title: {
        text: `${isOrdinary ? '普通' : '投资'}年度趋势`,
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: [isOrdinary ? '收入' : '赎回', isOrdinary ? '支出' : '买入', '剩余'],
        top: 30
      },
      xAxis: {
        type: 'category',
        data: years
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: isOrdinary ? '收入' : '赎回',
          type: 'bar',
          data: incomeData,
          itemStyle: { color: '#16a34a' }
        },
        {
          name: isOrdinary ? '支出' : '买入',
          type: 'bar',
          data: expenseData,
          itemStyle: { color: '#dc2626' }
        },
        {
          name: '剩余',
          type: 'bar',
          data: balanceData,
          itemStyle: { color: '#2563eb' }
        }
      ]
    }

    console.log('[渲染年度趋势] 设置图表配置')
    yearlyChartInstance.setOption(option)
    console.log('[渲染年度趋势] 渲染完成')
  } catch (error) {
    console.error('[渲染年度趋势] 渲染出错:', error)
  }
}

// 类型切换
function onCategoryChange() {
  // 重新加载当前tab的数据
  if (activeTab.value === 'monthly') {
    loadMonthlySummary()
  } else if (activeTab.value === 'yearly') {
    loadYearlySummary()
    loadMonthlyTrend()
  } else if (activeTab.value === 'yearly-trend') {
    loadYearlyTrend()
  }
}

// 监听账户筛选变化
watch(selectedAccountNames, () => {
  onCategoryChange()
})

// 监听tab切换
watch(activeTab, async (newTab) => {
  if (newTab === 'monthly') {
    if (!monthlySummary.value) {
      loadMonthlySummary()
    }
  } else if (newTab === 'yearly') {
    // 加载年度汇总和月度趋势
    if (!yearlySummary.value) {
      loadYearlySummary()
    }
    await nextTick()
    loadMonthlyTrend()
  } else if (newTab === 'yearly-trend') {
    await nextTick()
    loadYearlyTrend()
  }
})

// 监听月度趋势数据变化，渲染图表（嵌入在按年汇总tab中）
watch([monthlyTrend, activeTab], async ([newData, newTab]) => {
  if (newTab !== 'yearly') return
  if (!newData || !newData.data || newData.data.length === 0) return
  
  console.log('[watch月度趋势] 数据已更新，准备渲染')
  await nextTick()
  
  // 等待DOM准备好，最多尝试5次
  for (let i = 0; i < 5; i++) {
    console.log(`[watch月度趋势] 尝试${i + 1}/5, monthlyTrendChart.value:`, monthlyTrendChart.value)
    if (monthlyTrendChart.value) {
      renderMonthlyTrendChart()
      return
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  console.error('[watch月度趋势] 5次尝试后仍无法获取DOM元素')
})

// 监听年度趋势数据变化，渲染图表
watch([yearlyTrend, activeTab], async ([newData, newTab]) => {
  if (newTab !== 'yearly-trend') return
  if (!newData || !newData.data || newData.data.length === 0) return
  
  console.log('[watch年度趋势] 数据已更新，准备渲染')
  await nextTick()
  
  // 等待DOM准备好，最多尝试5次
  for (let i = 0; i < 5; i++) {
    console.log(`[watch年度趋势] 尝试${i + 1}/5, yearlyTrendChart.value:`, yearlyTrendChart.value)
    if (yearlyTrendChart.value) {
      renderYearlyTrendChart()
      return
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  console.error('[watch年度趋势] 5次尝试后仍无法获取DOM元素')
})

function formatAmount(v) {
  if (v === null || v === undefined) return '0.00'
  const num = Number(v)
  if (Number.isNaN(num)) return v
  
  const absNum = Math.abs(num)
  const formattedNum = num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  
  // 如果金额大于等于1万，在原始数字后面加上"(x.xw+)"标注
  if (absNum >= 10000) {
    // 向上取整到0.1万，确保显示的是超出的值
    const wanValue = (Math.ceil(absNum / 1000) / 10).toFixed(1)
    return `${formattedNum} (${wanValue}w+)`
  }
  
  // 小于1万的正常显示
  return formattedNum
}

// 用于概览卡片的格式化函数，返回HTML结构
function formatAmountCard(v) {
  if (v === null || v === undefined) return { main: '0.00', hint: '' }
  const num = Number(v)
  if (Number.isNaN(num)) return { main: v, hint: '' }
  
  const absNum = Math.abs(num)
  const formattedNum = num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  
  // 如果金额大于等于1万，返回分离的主数字和提示
  if (absNum >= 10000) {
    // 向上取整到0.1万
    const wanValue = (Math.ceil(absNum / 1000) / 10).toFixed(1)
    return { main: formattedNum, hint: `(${wanValue}w+)` }
  }
  
  return { main: formattedNum, hint: '' }
}

// 用于表格的格式化函数，返回分离的主数字和提示
function formatAmountTable(v) {
  if (v === null || v === undefined) return { main: '0.00', hint: '' }
  const num = Number(v)
  if (Number.isNaN(num)) return { main: v, hint: '' }
  
  const absNum = Math.abs(num)
  const formattedNum = num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  
  // 如果金额大于等于1万，返回分离的主数字和提示
  if (absNum >= 10000) {
    // 向上取整到0.1万
    const wanValue = (Math.ceil(absNum / 1000) / 10).toFixed(1)
    return { main: formattedNum, hint: `(${wanValue}w+)` }
  }
  
  return { main: formattedNum, hint: '' }
}

onMounted(() => {
  loadAccountNames()
  loadMonthlySummary()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
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

.page-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.info-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3b82f6;
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.info-icon:hover {
  background: #2563eb;
  transform: scale(1.1);
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

.top-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.filters-left {
  display: flex;
  gap: 2rem;
  align-items: center;
  flex-wrap: wrap;
}

.filters-right {
  margin-left: auto;
}

.import-btn {
  padding: 0.5rem 1rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
  font-weight: 500;
}

.import-btn:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.3);
}

.type-switcher {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.type-switcher label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  font-weight: 500;
}

.type-switcher input[type="radio"] {
  cursor: pointer;
}

.type-switcher span {
  padding: 0.25rem 0.5rem;
}

.account-filter {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.filter-label {
  font-size: 0.9rem;
  color: #374151;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.filter-label-inline {
  font-size: 0.9rem;
  color: #374151;
  font-weight: 500;
  white-space: nowrap;
}

.account-dropdown {
  position: relative;
  min-width: 200px;
}

.dropdown-toggle {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  color: #374151;
  transition: border-color 0.2s;
}

.dropdown-toggle:hover {
  border-color: #2563eb;
}

.dropdown-arrow {
  font-size: 0.7rem;
  color: #6b7280;
  transition: transform 0.2s;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
  z-index: 1000;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item input[type="checkbox"] {
  cursor: pointer;
  width: 16px;
  height: 16px;
}

.dropdown-item span {
  font-size: 0.9rem;
  color: #374151;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.tabs button {
  padding: 0.6rem 1rem;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 0.95rem;
  color: #6b7280;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tabs button.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
  font-weight: 600;
}

.tabs button:hover {
  color: #2563eb;
}

.tab-content {
  min-height: 300px;
}

/* 横向表单布局 */
.period-selector-inline {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
  padding: 0.75rem 1rem;
  background: #f9fafb;
  border-radius: 6px;
}

.inline-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #374151;
}

.inline-label span {
  white-space: nowrap;
}

.inline-label input {
  padding: 0.4rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  min-width: 150px;
}

.quick-links {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.quick-links a {
  color: #2563eb;
  text-decoration: none;
  font-size: 0.85rem;
  padding: 0.25rem 0.5rem;
  border-radius: 3px;
  transition: all 0.2s;
}

.quick-links a:hover {
  background: #eff6ff;
  text-decoration: underline;
}

.quick-links .separator {
  color: #d1d5db;
  font-size: 0.85rem;
}

.view-transactions-btn {
  padding: 0.4rem 0.8rem;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
  margin-left: auto;
}

.view-transactions-btn:hover {
  background: #059669;
  transform: translateY(-1px);
}

/* 图表区域 */
.chart-section {
  margin-bottom: 2rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 6px;
}

.chart-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  color: #374151;
}

/* 保留旧样式以兼容年度趋势tab */
.period-selector {
  margin-bottom: 1.5rem;
}

.period-controls {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.period-selector label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.9rem;
}

.period-selector input {
  padding: 0.45rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  min-width: 150px;
}

.period-buttons {
  display: flex;
  gap: 0.5rem;
}

.period-btn {
  padding: 0.45rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.summary-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.amount-main {
  line-height: 1.2;
}

.amount-hint {
  font-size: 0.85rem;
  color: #6b7280;
  font-weight: 400;
}

h4 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  color: #374151;
}

h5 {
  margin: 0 0 0.5rem 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #4b5563;
}

.dual-table {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.table-section {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
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
  background: #fff;
  font-size: 0.9rem;
  font-weight: 600;
}

td {
  font-size: 0.9rem;
}

/* 金额单元格 - 左右对齐 */
.amount-cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.amount-left {
  text-align: left;
  flex: 1;
}

.amount-right {
  text-align: right;
  font-size: 0.75rem;
  color: #6b7280;
  margin-left: 0.5rem;
  white-space: nowrap;
}

.empty {
  color: #9ca3af;
  text-align: center;
  padding: 2rem;
}

.loading {
  color: #6b7280;
  text-align: center;
  padding: 2rem;
}

.neg {
  color: #dc2626;
}

.pos {
  color: #16a34a;
}

.chart-container {
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .dual-table {
    grid-template-columns: 1fr;
  }
  
  .overview-cards {
    grid-template-columns: 1fr 1fr;
  }
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 90vw;
  max-height: 90vh;
  overflow: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #111827;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #6b7280;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #111827;
}

.modal-body {
  padding: 1.5rem;
}

/* 信息提示弹窗 */
.info-modal {
  max-width: 800px;
}

.info-section {
  margin-bottom: 1.5rem;
}

.info-section h4 {
  margin: 0 0 0.75rem 0;
  font-size: 1.1rem;
  color: #111827;
}

.info-section p {
  margin: 0.5rem 0;
  line-height: 1.6;
  color: #4b5563;
}

.info-section ul, .info-section ol {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
  color: #4b5563;
}

.info-section li {
  margin: 0.5rem 0;
  line-height: 1.6;
}

.feature-list {
  list-style: none;
  padding: 0;
}

.feature-list li {
  padding-left: 1.5rem;
  position: relative;
}

.feature-list li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #10b981;
  font-weight: bold;
}

/* 信息图样式 */
.info-diagram {
  margin: 1.5rem 0;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 8px;
}

.bank-account {
  border: 2px solid #3b82f6;
  border-radius: 12px;
  padding: 1.5rem;
  background: white;
}

.account-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.account-subtitle {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.logic-zones {
  display: grid;
  gap: 1rem;
}

.logic-zone {
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  background: #fafafa;
}

.logic-zone.ordinary {
  border-color: #10b981;
  background: #f0fdf4;
}

.logic-zone.investment {
  border-color: #3b82f6;
  background: #eff6ff;
}

.zone-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.zone-icon {
  font-size: 1.5rem;
}

.zone-title {
  font-weight: 600;
  color: #111827;
}

.zone-tag {
  font-size: 0.85rem;
  color: #6b7280;
  margin-bottom: 0.75rem;
}

.zone-items {
  list-style: none;
  padding: 0;
  margin: 0.75rem 0;
  font-size: 0.9rem;
  color: #4b5563;
}

.zone-items li {
  margin: 0.25rem 0;
}

.zone-examples {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
}

.example-tag {
  padding: 0.25rem 0.75rem;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  font-size: 0.8rem;
  color: #4b5563;
}

/* 上传结果弹窗 */
.upload-result-modal {
  max-width: 500px;
}

.result-success p, .result-error p {
  margin: 0.5rem 0;
  line-height: 1.6;
}

.result-error {
  color: #dc2626;
}

.warning {
  color: #f59e0b;
}

.countdown {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
  text-align: center;
  color: #6b7280;
  font-size: 0.9rem;
}

/* 流水查询弹窗 */
.transactions-modal {
  max-width: 1400px;
  width: 85vw;
  max-height: 90vh;
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.transactions-modal .modal-body {
  flex: 1;
  overflow: hidden;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.query-filters {
  margin-bottom: 1rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 6px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.filter-row {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-item-wide {
  flex: 1;
  min-width: 0;
}

.filter-input-wide {
  flex: 1;
  min-width: 200px;
  padding: 0.4rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
}

.filter-label {
  font-size: 0.9rem;
  color: #374151;
  white-space: nowrap;
}

.filter-input {
  padding: 0.4rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  width: 140px;
}

.filter-select {
  padding: 0.4rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  width: 110px;
  background: white;
  cursor: pointer;
}

.input-with-clear {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.input-with-clear .filter-input {
  padding-right: 2rem;
}

.clear-icon {
  position: absolute;
  right: 0.5rem;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #6b7280;
  border-radius: 50%;
  cursor: pointer;
  font-size: 0.7rem;
  transition: all 0.2s;
  user-select: none;
}

.clear-icon:hover {
  background: #d1d5db;
  color: #374151;
}

.query-btn {
  padding: 0.4rem 1.5rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.query-btn:hover {
  background: #2563eb;
}

.close-btn-inline {
  background: #6b7280;
}

.close-btn-inline:hover {
  background: #4b5563;
}

.transactions-table-wrapper {
  flex: 1;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  position: relative;
}

.transactions-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  table-layout: fixed;
}

.transactions-table th,
.transactions-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.transactions-table td:not(.summary-cell) {
  overflow: hidden;
  text-overflow: ellipsis;
}

.transactions-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
  z-index: 10;
  font-size: 0.85rem;
  overflow: visible;
}

.transactions-table tbody tr:hover {
  background: #f9fafb;
}

.hint-text {
  color: #9ca3af;
  font-weight: 400;
  font-size: 0.8rem;
  margin-left: 0.25rem;
}

.clickable-summary {
  cursor: pointer;
  color: #3b82f6;
  transition: all 0.2s;
}

.clickable-summary:hover {
  color: #2563eb;
  text-decoration: underline;
}

/* 批量操作工具栏 */
.batch-toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: #eff6ff;
  border: 1px solid #3b82f6;
  border-radius: 6px;
  margin-bottom: 1rem;
  flex-shrink: 0;
}

.batch-info {
  font-size: 0.9rem;
  color: #1e40af;
  font-weight: 500;
}

.batch-input {
  flex: 1;
  padding: 0.4rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  max-width: 300px;
}

.batch-save-btn {
  padding: 0.4rem 1rem;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.batch-save-btn:hover {
  background: #059669;
}

.batch-cancel-btn {
  padding: 0.4rem 1rem;
  background: #6b7280;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.batch-cancel-btn:hover {
  background: #4b5563;
}

.checkbox {
  cursor: pointer;
  width: 16px;
  height: 16px;
}

/* 导入下拉菜单 */
.import-dropdown {
  position: relative;
  display: flex;
  gap: 0;
}

.import-dropdown-toggle {
  padding: 0.5rem 0.75rem;
  background: #2563eb;
  color: white;
  border: none;
  border-left: 1px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  border-radius: 0 6px 6px 0;
}

.import-btn {
  border-radius: 6px 0 0 6px;
}

.import-dropdown-toggle:hover {
  background: #1d4ed8;
}

.import-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0.25rem;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 180px;
}

.import-menu-item {
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.import-menu-item:hover {
  background: #f3f4f6;
}

/* 规则管理弹窗 */
.rules-manager-modal {
  max-width: 800px;
  width: 90vw;
}

.rules-section {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e5e7eb;
}

.rules-section:last-of-type {
  border-bottom: none;
}

.rules-section h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  color: #111827;
}

.rules-desc {
  margin: 0 0 1rem 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.rule-group {
  margin-bottom: 1.5rem;
}

.rule-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.keyword-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.75rem;
  background: #eff6ff;
  border: 1px solid #3b82f6;
  border-radius: 16px;
  font-size: 0.85rem;
  color: #1e40af;
}

.keyword-tag .remove-btn {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.keyword-tag .remove-btn:hover {
  color: #1e40af;
}

.keyword-input {
  flex: 1;
  min-width: 200px;
  padding: 0.4rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
}

.replace-rules-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.replace-rule-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.match-type-select {
  padding: 0.4rem 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  background-color: white;
  cursor: pointer;
  min-width: 130px;
}

.match-type-select:hover {
  border-color: #9ca3af;
}

.match-type-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.rule-input {
  flex: 1;
  padding: 0.4rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
}

.arrow {
  color: #6b7280;
  font-size: 1.2rem;
}

.remove-btn-large {
  padding: 0.4rem 0.75rem;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.remove-btn-large:hover {
  background: #dc2626;
}

.add-rule-btn {
  padding: 0.5rem 1rem;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.add-rule-btn:hover {
  background: #059669;
}

.rules-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.save-rules-btn {
  padding: 0.5rem 1.5rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.save-rules-btn:hover {
  background: #2563eb;
}

.rerun-btn {
  padding: 0.5rem 1.5rem;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.rerun-btn:hover {
  background: #d97706;
}

/* 重跑预览弹窗 */
.rerun-preview-modal {
  max-width: 1000px;
  width: 90vw;
}

.preview-info {
  margin: 0 0 1rem 0;
  padding: 0.75rem 1rem;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 4px;
  color: #92400e;
  font-size: 0.9rem;
}

.preview-info strong {
  color: #78350f;
  font-weight: 600;
}

.changes-table-wrapper {
  max-height: 500px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 1rem;
}

.changes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.changes-table th,
.changes-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.changes-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
  z-index: 10;
}

.changes-table tbody tr:hover {
  background: #f9fafb;
}

.change-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.change-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.change-label {
  font-weight: 500;
  color: #6b7280;
  min-width: 40px;
}

.old-value {
  color: #dc2626;
  text-decoration: line-through;
}

.new-value {
  color: #10b981;
  font-weight: 500;
}

.preview-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.confirm-rerun-btn {
  padding: 0.5rem 1.5rem;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.confirm-rerun-btn:hover:not(:disabled) {
  background: #059669;
}

.confirm-rerun-btn:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.summary-cell {
  position: relative;
  overflow: visible;
}

.summary-edit-mode {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.summary-input {
  flex: 1;
  padding: 0.3rem 0.5rem;
  border: 1px solid #3b82f6;
  border-radius: 4px;
  font-size: 0.85rem;
  min-width: 0;
}

.edit-actions {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.summary-display {
  position: relative;
  cursor: pointer;
  padding: 0.2rem 0.3rem;
  margin: -0.2rem -0.3rem;
  z-index: 1;
  border-radius: 4px;
  transition: background 0.2s;
}

.summary-display:hover {
  background: #f3f4f6;
  z-index: 100;
}

.summary-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.original-summary {
  text-decoration: line-through;
  color: #9ca3af;
  font-size: 0.75rem;
}

.current-summary {
  color: #111827;
  font-size: 0.85rem;
}

.quick-filter-tooltip {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 0.25rem;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  white-space: nowrap;
}

.quick-filter-btn {
  padding: 0.5rem 0.75rem;
  background: white;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  width: 100%;
  text-align: left;
}

.quick-filter-btn:hover {
  background: #eff6ff;
  color: #2563eb;
}

.save-btn, .cancel-btn {
  padding: 0.3rem 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
  min-width: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.save-btn {
  background: #10b981;
  color: white;
}

.save-btn:hover {
  background: #059669;
}

.cancel-btn {
  background: #6b7280;
  color: white;
}

.cancel-btn:hover {
  background: #4b5563;
}
</style>
