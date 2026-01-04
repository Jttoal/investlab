# API 使用示例

本文档提供了InvestLab API的使用示例,可以使用curl、Postman或其他HTTP客户端进行测试。

## 基础信息

- **Base URL**: `http://localhost:8080/api/v1`
- **Content-Type**: `application/json`

## 账户管理 API

### 创建账户

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "招商证券主账户",
    "broker": "招商证券",
    "currency": "CNY",
    "balanceManual": 50000.00
  }'
```

### 获取所有账户

```bash
curl http://localhost:8080/api/v1/accounts
```

### 更新账户

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "招商证券主账户",
    "broker": "招商证券",
    "currency": "CNY",
    "balanceManual": 60000.00
  }'
```

### 删除账户

```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/1
```

## 策略管理 API

### 创建策略

```bash
curl -X POST http://localhost:8080/api/v1/strategies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "沪深300定投",
    "type": "index",
    "goalNote": "每月定投沪深300指数基金,长期持有"
  }'
```

### 获取所有策略

```bash
curl http://localhost:8080/api/v1/strategies
```

### 按类型筛选策略

```bash
curl "http://localhost:8080/api/v1/strategies?type=index"
```

### 获取策略详情

```bash
curl http://localhost:8080/api/v1/strategies/1
```

## 标的管理 API

### 添加标的

```bash
curl -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "510300",
    "name": "沪深300ETF",
    "market": "CN",
    "strategyId": 1,
    "targetLow": 3.50,
    "targetHigh": 4.50,
    "note": "跟踪沪深300指数"
  }'
```

### 获取策略的所有标的

```bash
curl "http://localhost:8080/api/v1/assets?strategyId=1"
```

## 交易管理 API

### 记录买入交易

```bash
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "accountId": 1,
    "assetId": 1,
    "type": "buy",
    "price": 3.85,
    "quantity": 1000,
    "fee": 5.00,
    "tradeDate": "2025-12-31",
    "note": "定投第一笔"
  }'
```

### 记录卖出交易

```bash
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "accountId": 1,
    "assetId": 1,
    "type": "sell",
    "price": 4.20,
    "quantity": 500,
    "fee": 5.00,
    "tradeDate": "2025-12-31",
    "note": "部分止盈"
  }'
```

### 记录分红

```bash
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "accountId": 1,
    "assetId": 1,
    "type": "dividend",
    "price": 0.15,
    "quantity": 1000,
    "fee": 0,
    "tradeDate": "2025-12-31",
    "note": "年度分红"
  }'
```

### 获取策略的交易记录

```bash
curl "http://localhost:8080/api/v1/trades?strategyId=1"
```

### 按日期范围查询交易

```bash
curl "http://localhost:8080/api/v1/trades?startDate=2025-01-01&endDate=2025-12-31"
```

## 观点管理 API

### 添加观点

```bash
curl -X POST http://localhost:8080/api/v1/viewpoints \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "title": "2025年指数投资展望",
    "summary": "预计市场将继续震荡上行,适合继续定投",
    "link": "https://example.com/article",
    "tag": "宏观",
    "viewpointDate": "2025-12-31"
  }'
```

### 获取策略的观点

```bash
curl "http://localhost:8080/api/v1/viewpoints?strategyId=1"
```

### 按标签筛选观点

```bash
curl "http://localhost:8080/api/v1/viewpoints?tag=宏观"
```

## 仪表盘 API

### 获取仪表盘数据

```bash
curl http://localhost:8080/api/v1/dashboard
```

### 获取策略详情(含持仓和收益)

```bash
curl http://localhost:8080/api/v1/dashboard/strategies/1
```

## 完整示例流程

以下是一个完整的使用流程示例:

```bash
# 1. 创建账户
ACCOUNT=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"测试账户","broker":"测试券商","currency":"CNY","balanceManual":100000}')
ACCOUNT_ID=$(echo $ACCOUNT | jq -r '.id')

# 2. 创建策略
STRATEGY=$(curl -s -X POST http://localhost:8080/api/v1/strategies \
  -H "Content-Type: application/json" \
  -d '{"name":"测试策略","type":"index","goalNote":"测试用"}')
STRATEGY_ID=$(echo $STRATEGY | jq -r '.id')

# 3. 添加标的
ASSET=$(curl -s -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d "{\"symbol\":\"510300\",\"name\":\"沪深300ETF\",\"market\":\"CN\",\"strategyId\":$STRATEGY_ID,\"targetLow\":3.5,\"targetHigh\":4.5}")
ASSET_ID=$(echo $ASSET | jq -r '.id')

# 4. 记录交易
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d "{\"strategyId\":$STRATEGY_ID,\"accountId\":$ACCOUNT_ID,\"assetId\":$ASSET_ID,\"type\":\"buy\",\"price\":3.85,\"quantity\":1000,\"fee\":5,\"tradeDate\":\"2025-12-31\"}"

# 5. 查看仪表盘
curl http://localhost:8080/api/v1/dashboard

# 6. 查看策略详情
curl http://localhost:8080/api/v1/dashboard/strategies/$STRATEGY_ID
```

## 错误响应示例

所有错误都会返回统一格式:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "账户不存在: 999",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2025-12-31T10:00:00"
}
```

常见错误码:
- `RESOURCE_NOT_FOUND` - 资源不存在(404)
- `BUSINESS_ERROR` - 业务逻辑错误(400)
- `VALIDATION_ERROR` - 数据验证失败(400)
- `INTERNAL_ERROR` - 系统内部错误(500)
