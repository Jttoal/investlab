# 家庭账单统计分析模块 PRD

## 1. 背景与目标

基于家庭账单流水数据，提供多维度统计分析能力，帮助用户：
- 快速了解收支趋势和结余情况
- 识别主要支出/收入项目
- 通过图表直观查看月度/年度财务健康状况
- 支持按账户名过滤，实现多账户独立分析

## 2. 用户故事

### US1: 按月汇总查看
**作为**家庭财务管理者  
**我想要**按月查看汇总信息（总收入、总支出、剩余、各交易摘要汇总）  
**以便于**了解每月财务状况和主要开销项

**验收标准：**
- 可选择查看某个月份的数据
- 可选择类型（普通/投资），默认显示普通类
- 显示该月总收入、总支出、剩余（收入-支出）
- 显示收支明细表，左右分栏布局：
  - 左侧：支出明细（按交易摘要分组汇总）
  - 右侧：收入明细（按交易摘要分组汇总）

### US2: 按年汇总查看
**作为**家庭财务管理者  
**我想要**按年查看汇总信息  
**以便于**总结年度收支情况

**验收标准：**
- 可选择查看某一年的数据
- 可选择类型（普通/投资），默认显示普通类
- 显示该年总收入、总支出、剩余
- 同样采用左右分栏布局显示收支明细

### US3: 月度趋势图
**作为**家庭财务管理者  
**我想要**查看一年内每月的收入/支出/剩余趋势图  
**以便于**发现季节性规律和异常波动

**验收标准：**
- 默认显示过去12个月的数据
- 折线图/柱状图展示每月总收入、总支出、剩余
- 可切换查看不同年份

### US4: 年度趋势图
**作为**家庭财务管理者  
**我想要**查看多年的收入/支出/剩余年度趋势  
**以便于**评估长期财务状况变化

**验收标准：**
- 显示所有有数据年份的趋势
- 柱状图展示每年总收入、总支出、剩余

### US5: 按类型过滤
**作为**家庭财务管理者  
**我想要**在普通和投资两种类型之间切换查看  
**以便于**分别分析日常开销和投资情况

**验收标准：**
- 统计页面提供类型切换器（单选：普通/投资）
- 默认显示"普通"类型
- 切换类型后，所有汇总和图表数据仅计算该类型的交易
  - 普通：expense/income
  - 投资：buy/redeem

### US6: 按账户名过滤
**作为**拥有多个银行账户的用户  
**我想要**按账户名过滤统计数据  
**以便于**单独分析某个账户的收支情况

**验收标准：**
- 统计页面顶部提供账户名筛选器（支持多选）
- 筛选后，所有汇总和图表数据仅计算选中账户的交易
- 默认显示所有账户

## 3. 功能需求

### 3.1 汇总维度

#### 3.1.1 按月汇总
- **输入参数**：月份（YYYY-MM）、类型（ordinary/investment，默认 ordinary）、账户名（可选，多选）
- **输出内容**：
  - 总览卡片：总收入、总支出、剩余
  - 收支明细表（左右分栏）：
    - **左侧：支出明细**
      - 列：交易摘要、金额
      - 行：按交易摘要分组，只显示支出方向（`direction=expense` 或 `direction=buy`）
      - 金额显示为正数（取绝对值）
      - 排序：按金额降序
    - **右侧：收入明细**
      - 列：交易摘要、金额
      - 行：按交易摘要分组，只显示收入方向（`direction=income` 或 `direction=redeem`）
      - 排序：按金额降序

#### 3.1.2 按年汇总
- **输入参数**：年份（YYYY）、类型（ordinary/investment，默认 ordinary）、账户名（可选，多选）
- **输出内容**：与按月汇总相同结构，但统计整年数据

### 3.2 趋势图表

#### 3.2.1 月度趋势图
- **输入参数**：年份（YYYY）、类型（ordinary/investment，默认 ordinary）、账户名（可选，多选）
- **图表类型**：折线图 + 柱状图组合
- **数据系列**：
  - 总收入（绿色柱）
  - 总支出（红色柱）
  - 剩余（蓝色折线）
- **X轴**：月份（1-12月）
- **Y轴**：金额
- **交互**：悬停显示详细数值

#### 3.2.2 年度趋势图
- **输入参数**：类型（ordinary/investment，默认 ordinary）、账户名（可选，多选）
- **图表类型**：柱状图
- **数据系列**：
  - 总收入（绿色）
  - 总支出（红色）
  - 剩余（蓝色）
- **X轴**：年份（自动识别数据中所有年份）
- **Y轴**：金额

### 3.3 筛选器

#### 3.3.1 类型切换器
- **位置**：统计页面顶部
- **控件类型**：单选按钮组（Radio）
- **选项**：
  - 普通（ordinary）- 默认选中
  - 投资（investment）
- **交互逻辑**：点击切换后，所有汇总表和图表刷新

#### 3.3.2 账户名过滤器

- **位置**：统计页面顶部
- **控件类型**：下拉多选框
- **数据来源**：从 `statement_transaction` 表中去重获取所有 `account_name`
- **交互逻辑**：
  - 默认选中"全部账户"
  - 用户可勾选/取消单个或多个账户
  - 点击"应用"后，所有汇总表和图表刷新

## 4. 数据模型

### 4.1 现有表结构（复用）
```sql
statement_transaction (
  id, user_id, file_id, txn_date, currency, amount, balance,
  txn_type_raw, counterparty, account_name,
  category (ordinary/investment),
  direction (expense/income/buy/redeem),
  dedup_key, hash_raw, created_at
)
```

### 4.2 汇总查询逻辑

#### 按月汇总 - 支出明细
```sql
SELECT 
  txn_type_raw AS summary,
  SUM(ABS(amount)) AS total
FROM statement_transaction
WHERE category = :category  -- 'ordinary' 或 'investment'
  AND direction IN ('expense', 'buy')  -- 根据 category 选择对应的支出方向
  AND txn_date >= :startDate AND txn_date < :endDate
  AND (:accountNames IS NULL OR account_name IN (:accountNames))
GROUP BY txn_type_raw
ORDER BY total DESC
```

#### 按月汇总 - 收入明细
```sql
SELECT 
  txn_type_raw AS summary,
  SUM(amount) AS total
FROM statement_transaction
WHERE category = :category
  AND direction IN ('income', 'redeem')  -- 根据 category 选择对应的收入方向
  AND txn_date >= :startDate AND txn_date < :endDate
  AND (:accountNames IS NULL OR account_name IN (:accountNames))
GROUP BY txn_type_raw
ORDER BY total DESC
```

#### 月度趋势
```sql
SELECT 
  strftime('%Y-%m', txn_date) AS month,
  SUM(CASE WHEN direction IN ('income', 'redeem') THEN amount ELSE 0 END) AS income,
  SUM(CASE WHEN direction IN ('expense', 'buy') THEN ABS(amount) ELSE 0 END) AS expense,
  SUM(amount) AS balance
FROM statement_transaction
WHERE category = :category
  AND txn_date >= :startDate AND txn_date < :endDate
  AND (:accountNames IS NULL OR account_name IN (:accountNames))
GROUP BY month
ORDER BY month ASC
```

## 5. API 设计

### 5.1 按月汇总
```
GET /api/v1/household-bills/statistics/monthly?month=2025-01&category=ordinary&accountNames=蒋涛,陈钦伟

Response:
{
  "month": "2025-01",
  "category": "ordinary",
  "overview": {
    "totalIncome": 15236.49,
    "totalExpense": 28017.14,
    "balance": -12780.65
  },
  "expenseDetails": [
    {"summary": "快捷支付", "amount": 9984.94},
    {"summary": "转账汇款", "amount": 12300.00},
    {"summary": "基金定期定额申购", "amount": 1600.00},
    ...
  ],
  "incomeDetails": [
    {"summary": "代发工资", "amount": 15236.49},
    {"summary": "行内转账转入", "amount": 6150.00},
    {"summary": "活动现金红包", "amount": 2.88},
    ...
  ]
}
```

### 5.2 按年汇总
```
GET /api/v1/household-bills/statistics/yearly?year=2021&category=ordinary&accountNames=蒋涛

Response: (结构同按月汇总)
```

### 5.3 月度趋势
```
GET /api/v1/household-bills/statistics/monthly-trend?year=2021&category=ordinary&accountNames=蒋涛

Response:
{
  "year": 2021,
  "category": "ordinary",
  "data": [
    {"month": "2021-01", "income": 15236.49, "expense": 28017.14, "balance": -12780.65},
    {"month": "2021-02", "income": ..., "expense": ..., "balance": ...},
    ...
  ]
}
```

### 5.4 年度趋势
```
GET /api/v1/household-bills/statistics/yearly-trend?category=ordinary&accountNames=蒋涛

Response:
{
  "category": "ordinary",
  "data": [
    {"year": "2021", "income": 182345.67, "expense": 156789.12, "balance": 25556.55},
    {"year": "2022", "income": ..., "expense": ..., "balance": ...},
    ...
  ]
}
```

### 5.5 获取账户名列表
```
GET /api/v1/household-bills/account-names

Response:
{
  "accountNames": ["蒋涛", "陈钦伟", "王佳楠"]
}
```

## 6. 前端 UX 设计

### 6.1 页面布局

```
┌─────────────────────────────────────────────────┐
│ 家庭账单 > 统计分析                               │
├─────────────────────────────────────────────────┤
│ 类型: ● 普通  ○ 投资                             │
│ 账户筛选: [全部 ▼] [蒋涛 ✓] [陈钦伟 ✓]           │
│                                                 │
│ [按月汇总] [按年汇总] [月度趋势] [年度趋势]      │
├─────────────────────────────────────────────────┤
│ 当前选择: 2025年1月  [普通]                      │
│ ┌──────────┬──────────┬──────────┐              │
│ │ 总收入    │ 总支出    │ 剩余      │              │
│ │ 15,236.49│ 28,017.14│ -12,780.65│              │
│ └──────────┴──────────┴──────────┘              │
│                                                 │
│ 【收支明细】                                      │
│ ┌─────────────────┬─────────────────┐           │
│ │   支出           │    收入          │           │
│ ├─────────────────┼─────────────────┤           │
│ │ 快捷支付  9,984.94│ 代发工资 15,236.49│         │
│ │ 转账汇款 12,300.00│ 汇入     6,150.00│         │
│ │ 信用卡还款 2,297.21│ 红包        2.88│         │
│ │ ...              │ ...              │           │
│ └─────────────────┴─────────────────┘           │
└─────────────────────────────────────────────────┘
```

### 6.2 交互说明

1. **类型切换**：点击"普通"或"投资"单选按钮，所有数据刷新
   - 普通类型：显示 expense/income 数据，支出/收入标签
   - 投资类型：显示 buy/redeem 数据，买入/赎回标签
2. **标签切换**：点击顶部标签切换不同视图（按月/按年/月度趋势/年度趋势）
3. **账户筛选**：点击账户下拉框，勾选/取消账户，点击"应用"后刷新所有数据
4. **月份/年份选择**：
   - 按月汇总：显示月份选择器（YYYY-MM）
   - 按年汇总：显示年份选择器（YYYY）
   - 月度趋势：显示年份选择器
   - 年度趋势：无需选择（自动显示所有年份）
5. **图表交互**：悬停显示详细数值，可缩放、拖拽

### 6.3 样式要求

- 类型切换器：单选按钮，选中项加粗显示
- 总览卡片：大字号显示金额，正数绿色、负数红色
- 收支明细表：左右分栏，等宽布局，支出列红色、收入列绿色
- 图表：使用 ECharts，配色与总览卡片一致
- 响应式：移动端表格横向滚动或上下堆叠

## 7. 性能要求

- 汇总查询 P95 < 300ms
- 趋势图数据量：月度最多12个数据点，年度预计 < 10个数据点
- 前端图表渲染 < 500ms

## 8. 验收标准

1. 类型切换器默认选中"普通"，切换后数据正确刷新
2. 按月汇总显示正确的总收入、总支出、剩余
3. 收支明细表采用左右分栏布局，支出/收入分别显示在左右两侧
4. 切换到投资类型后，显示买入/赎回数据，标签也相应改变
5. 月度趋势图正确显示12个月的收支趋势
6. 年度趋势图正确显示多年数据
7. 账户名过滤后，所有统计数据仅计算选中账户
8. 切换月份/年份后，数据实时更新

## 9. 后续优化

- 支持导出统计报表（Excel/PDF）
- 增加预算对比功能（设定月度预算，显示超支/结余）
- 支持自定义日期范围汇总
- 增加支出/收入分类饼图
- 移动端优化（手势切换月份）
