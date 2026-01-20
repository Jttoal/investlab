# 家庭账单模块 PRD

## 1. 背景与目标
- 支持用户在网页上传银行 PDF 账单，系统自动解析流水并入库，形成家庭收支/投资视图。
- 新增 Kotlin PDF 解析与导入链路（不再依赖 `backend/extra_pdf.py`），补齐去重、分类、入库与查询能力。
- 目标：解析准确、去重可靠、分类可见，提供可检索的流水列表与汇总。

## 2. 范围
- In scope：PDF 上传、解析入库、重复检测、分类标注、查询/汇总、结果反馈。
- Out of scope：手动改账、OCR（仅处理文本型 PDF）、多银行适配（先支持现有格式）。

## 3. 角色
- 登录用户（家庭成员/空间），数据按用户/家庭隔离。

## 4. 业务需求
1) 上传与解析  
   - 前端上传单个 PDF（最大 50MB，校验扩展名/大小）。  
   - 后端异步解析：记录文件元信息，使用 Kotlin PDF 解析器（建议 pdfbox/itext）生成流水行。  
   - 解析进度与结果可查询（总行数、成功/去重/失败）。

2) 去重规则  
   - 维度：`日期 + 币种 + 金额(带正负) + 序号`。  
   - 序号：同一文件内，同一天同一签名金额的出现顺序（1-based）；示例：`20251112_-100_1`、`20251112_-100_2`。  
   - 跨文件去重：若同一用户已存在相同 `dedup_key` 视为重复，跳过写入并计入去重数。  
   - 数据库需对 `user_id + dedup_key` 建唯一索引。

3) 分类规则  
   - 类型：普通 | 投资。  
   - 方向：普通 → 支出/收入；投资 → 买入/赎回（后端按金额符号区分：金额 < 0 视作支出/买入，金额 > 0 视作收入/赎回，若金额=0 归为收入-其他）。  
   - 判定优先级：  
     1. 若交易摘要包含投资关键词 → 投资类。  
     2. 若对手信息包含投资对手关键词或含“基金销售” → 投资类。  
     3. 否则 → 普通类。  
   - 投资类交易摘要关键词（当前版）：`受托理财申购`、`受托理财赎回`、`基金定期定额申购`、`基金申购`、`申购`、`基金赎回`、`朝朝宝转入`、`朝朝宝自动转入`、`朝朝宝转出`、`基金认购`、`银证转账(第三方存管)`、`受托理财分红`。  
   - 投资类对手信息关键词（当前版）：`盈米基金`、`蚂蚁基金`、`广发基金`、`景顺长城基金`、`基金销售`。

4) 数据查询与呈现  
   - 列表筛选：日期范围、方向、类型、金额区间、关键词（摘要/对手信息）。  
   - 汇总：按月/分类汇总收入、支出、买入、赎回。  
   - 上传记录页：展示文件名、状态、总行数、成功/去重/失败计数、开始/结束时间、下载 CSV（解析结果）。
  
5) 投资类关键字规则可配置
    - 投资类交易摘要关键词 和  投资类对手信息关键词 可以在系统中配置中维护，key为 household_bills.invest_category value 为json {"billing_summary_keyword":["xxx"],"counter_party_keyword":["xxx"]}
  
  
## 5. 业务流程（简化）
1. 前端上传 PDF → `POST /api/v1/household-bills/uploads`（表单+文件）。  
2. 后端创建文件记录，校验 MD5（可选同文件去重），存储。  
3. 进入异步任务：调用 Kotlin 解析器解析 PDF，产出行数据。  
4. 为每行生成 `dedup_key`，检查唯一约束；新行入库，重复计入去重数。  
5. 按分类规则标注类型/方向并写入交易表。  
6. 更新文件记录状态，前端轮询/查询展示。  
7. 用户在列表/汇总页面查看、筛选。

## 6. 数据模型（建议）
- 表：`statement_file`  
  - `id`, `user_id`, `file_name`, `file_md5`, `period_start`, `period_end`, `status`(pending/processing/success/failed), `total_rows`, `inserted_rows`, `dedup_rows`, `failed_rows`, `error_msg`, `created_at`, `finished_at`.
- 表：`statement_transaction`  
  - `id`, `user_id`, `file_id`, `txn_date`, `currency`, `amount`, `balance`, `txn_type_raw`, `counterparty`,  
    `category`(ordinary/investment), `direction`(expense/income/buy/redeem),  
    `dedup_key`, `hash_raw`(可选保存原行哈希), `created_at`.  
  - 索引：`(user_id, dedup_key)` 唯一；`txn_date` 索引；`category, direction` 组合索引（汇总查询）。
- 可选：`classification_rules`（可配置关键词），字段 `keyword`, `category`, `direction`, `priority`, `enabled`.

## 7. API 草案（REST，Kotlin/Spring）
- `POST /api/v1/household-bills/uploads`：表单上传 PDF，返回 `file_id`。  
- `GET /api/v1/household-bills/uploads/{fileId}`：获取状态与计数。  
- `GET /api/v1/household-bills/transactions`：列表查询（分页，含筛选条件）。  
- `GET /api/v1/household-bills/summary`：汇总统计（按月/分类）。  
- `POST /api/v1/household-bills/uploads/{fileId}/reparse`（可选）：重新解析。  
- 认证：复用现有鉴权；所有接口需登录。

## 8. 前端 UX 要点
- 上传页：文件选择、大小/格式校验、提交后显示“处理中”并轮询状态。  
- 结果页：显示成功/去重/失败计数，下载 CSV（解析结果），失败原因列表。  
- 流水列表：可筛选、排序，展示分类与方向。  
- 汇总视图：按月收入/支出/买入/赎回的图表。

## 9. 去重与序号生成实现建议
- 解析时按文件内行顺序遍历，维护哈希：`(date, currency, signed_amount) -> count`；每出现一条递增得到序号。  
- `signed_amount = amount`（保留正负，去除千分位，保留两位小数）。  
- `dedup_key = "{yyyymmdd}_{signed_amount}_{count}"`。  
- 入库前检查唯一约束，冲突即计为去重。

## 10. 失败与告警
- 失败行：记录原因（解析失败/字段缺失/格式错误），计入 `failed_rows`。  
- 文件级失败：超时、Kotlin 解析异常、格式不匹配。  
- 关键错误写 ERROR 日志，不含敏感信息。

## 11. 非功能
- 性能：列表接口 P95 < 300ms；解析任务单文件目标 < 60s（50页基准）。  
- 并发：单用户建议同时最多 2 个解析任务。  
- 安全：仅登录可用；文件存储需访问控制；清理临时文件。  
- 可观察性：解析耗时、行数、去重率、失败率指标。

## 12. 验收标准
- 上传并解析样例 PDF，成功入库且去重计数正确（准备两份时间重叠样例）。  
- 分类规则覆盖关键词用例，对手信息含“基金销售”判为投资；金额符号 fallback 正确。  
- 列表/汇总接口字段正确，前端联调通过。  
- 日志可追踪，唯一约束生效。

## 13. 后续迭代（非必需）
- 规则管理后台（关键词可配）。  
- 导出 Excel/CSV。  
- OCR/PDF 图片流支持。  
- 金额归因至预算科目（映射表）。
