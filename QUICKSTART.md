# InvestLab 快速启动指南

## 前置条件

确保你的系统已安装:
- **JDK 21** 或更高版本
- **Node.js 18** 或更高版本
- **npm** 或 **yarn**

## 第一步: 启动后端服务

### 1. 进入后端目录
```bash
cd backend
```

### 2. 启动Spring Boot应用
```bash
# macOS/Linux
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

### 3. 验证后端启动
打开浏览器访问: http://localhost:8080/api/v1/accounts

如果看到 `[]` (空数组),说明后端启动成功!

> 首次启动时,Flyway会自动创建数据库表结构

## 第二步: 启动前端应用

### 1. 打开新的终端窗口,进入前端目录
```bash
cd frontend
```

### 2. 安装依赖(仅首次需要)
```bash
npm install
```

### 3. 启动开发服务器
```bash
npm run dev
```

### 4. 访问应用
打开浏览器访问: http://localhost:5173

## 开始使用

### 1. 创建账户
1. 点击顶部导航栏的"账户管理"
2. 点击"新建账户"按钮
3. 填写账户信息并保存

### 2. 创建策略
1. 点击顶部导航栏的"策略管理"
2. 点击"新建策略"按钮
3. 选择策略类型(指数投资/红利低波/网格/自定义)
4. 填写策略信息并保存

### 3. 添加标的
1. 进入某个策略的详情页
2. 在"持仓标的"标签页,点击"添加标的"
3. 填写股票代码、名称等信息
4. 可选:设置目标低价和高价用于提醒

### 4. 记录交易
1. 在策略详情页的"交易记录"标签页
2. 点击"记录交易"
3. 选择标的、账户、交易类型
4. 填写价格、数量等信息并保存

### 5. 查看收益
1. 返回首页仪表盘
2. 查看各策略的收益汇总
3. 点击"查看详情"进入策略详情页查看更多信息

## 常见问题

### Q: 后端启动失败
A: 检查是否安装了JDK 21+,运行 `java -version` 确认版本

### Q: 前端启动失败
A: 
1. 确认Node.js版本: `node -version` (需要18+)
2. 删除 `node_modules` 文件夹,重新运行 `npm install`

### Q: 前端无法连接后端
A: 
1. 确认后端已启动并运行在 8080 端口
2. 检查浏览器控制台是否有CORS错误
3. 确认 `frontend/src/utils/http.js` 中的 baseURL 配置正确

### Q: 数据库在哪里?
A: SQLite数据库文件 `investlab.db` 位于 `backend/` 目录下,首次启动时自动创建

### Q: 如何重置数据库?
A: 删除 `backend/investlab.db` 文件,重启后端服务即可重新创建空数据库

## 下一步

- 阅读完整的 [README.md](./README.md)
- 查看 [开发规范](./.cursorrules/project_rule.md)
- 查看 [MVP PRD](./docs/mvp/mvp_prd.md)

## 技术支持

如有问题,请提交 Issue 或查看项目文档。
