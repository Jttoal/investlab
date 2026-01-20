# InvestLab - 投资策略追踪工具

## 项目简介

InvestLab 是一款面向个人长期投资者的策略维度资产管理与复盘工具。帮助投资者用简单、低门槛、结构化的方式管理投资策略、账户与交易,通过长期记录与对比,找到最适合自己的投资方式。

## 技术栈

### 后端
- **语言**: Kotlin
- **框架**: Spring Boot 3.3.0
- **数据库**: SQLite
- **迁移工具**: Flyway
- **构建工具**: Gradle

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios

## 项目结构

```
investlab/
├── backend/              # Spring Boot 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/investlab/
│   │   │   │   ├── api/           # REST API Controllers
│   │   │   │   ├── service/       # 业务逻辑层
│   │   │   │   ├── repository/    # 数据访问层
│   │   │   │   ├── model/         # 实体和DTO
│   │   │   │   ├── config/        # 配置类
│   │   │   │   └── exception/     # 异常处理
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/  # Flyway迁移脚本
│   │   └── test/                  # 单元测试
│   └── build.gradle.kts
├── frontend/             # Vue 3 前端应用
│   ├── src/
│   │   ├── api/          # API接口封装
│   │   ├── stores/       # Pinia状态管理
│   │   ├── views/        # 页面组件
│   │   ├── components/   # 可复用组件
│   │   ├── router/       # 路由配置
│   │   ├── utils/        # 工具函数
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── docs/                 # 项目文档
│   └── mvp/
│       └── mvp_prd.md    # MVP产品需求文档
└── .cursorrules/         # 开发规范
    └── project_rule.md
```

## 核心功能

### MVP 版本功能

1. **账户管理**
   - 创建和管理多个券商账户
   - 手动记录账户余额
   - 查看账户持仓汇总

2. **策略管理**
   - 支持多种策略类型(指数投资/红利低波/网格/自定义)
   - 每个策略可维护标的清单
   - 设置价格提醒区间
   - 查看策略收益汇总

3. **标的管理**
   - 添加和管理投资标的
   - 设置目标买入/卖出价格
   - 查看持仓和盈亏情况

4. **交易管理**
   - 记录买入/卖出/分红交易
   - 自动计算成本和收益
   - 按策略/账户/标的查询交易

5. **大V观点记录**
   - 记录投资观点和文章
   - 与策略关联
   - 支持标签分类和搜索

6. **仪表盘**
   - 账户和策略概览
   - 收益汇总对比
   - 价格提醒状态

## 快速开始

### 方式一: 一键构建和运行(推荐)

**macOS / Linux:**
```bash
./build-and-run.sh
```

**Windows:**
```cmd
build-and-run.bat
```

脚本会自动完成:
- ✅ 检查环境依赖
- ✅ 构建后端项目
- ✅ 构建前端项目
- ✅ 生成启动脚本
- ✅ 启动服务(可选)

详细说明请查看 [BUILD_GUIDE.md](./BUILD_GUIDE.md)

### 方式二: 开发模式启动

#### 前置要求

- JDK 21+
- Node.js 18+
- Gradle 9.0+

#### 后端启动

**普通模式:**
```bash
cd backend
./gradlew bootRun
```

**调试模式(推荐用于开发):**
```bash
# 方式1: 使用提供的脚本
./test-backend-start.sh

# 方式2: 直接使用 Gradle
./gradlew :backend:bootRun --debug-jvm
# 然后在 VS Code 中使用 F5 选择 "Attach to InvestLab (远程调试)"
```

**VS Code 调试:**
- 按 F5,选择 "InvestLab (Gradle bootRun - 推荐)"
- 或者选择 "InvestLab (Spring Boot - 直接启动)"

详细调试说明请查看 [DEBUG_GUIDE.md](./DEBUG_GUIDE.md)

后端服务将在 `http://localhost:8080` 启动

#### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

**重要**: 前端已配置 Vite 代理,会自动将 `/api` 请求转发到后端 `http://localhost:8080`。详细说明请查看 [frontend/PROXY_CONFIG.md](./frontend/PROXY_CONFIG.md)

#### 验证连接

```bash
./test-proxy.sh
```

这个脚本会自动检查前后端服务是否正常运行,以及代理配置是否正确。

## API 文档

### 基础路径
所有API的基础路径为: `/api/v1`

### 主要接口

#### 账户管理
- `GET /accounts` - 获取所有账户
- `GET /accounts/{id}` - 获取账户详情
- `POST /accounts` - 创建账户
- `PUT /accounts/{id}` - 更新账户
- `DELETE /accounts/{id}` - 删除账户

#### 策略管理
- `GET /strategies` - 获取所有策略
- `GET /strategies/{id}` - 获取策略详情
- `POST /strategies` - 创建策略
- `PUT /strategies/{id}` - 更新策略
- `DELETE /strategies/{id}` - 删除策略

#### 标的管理
- `GET /assets` - 获取所有标的
- `GET /assets/{id}` - 获取标的详情
- `POST /assets` - 创建标的
- `PUT /assets/{id}` - 更新标的
- `DELETE /assets/{id}` - 删除标的

#### 交易管理
- `GET /trades` - 获取所有交易
- `GET /trades/{id}` - 获取交易详情
- `POST /trades` - 创建交易
- `PUT /trades/{id}` - 更新交易
- `DELETE /trades/{id}` - 删除交易

#### 观点管理
- `GET /viewpoints` - 获取所有观点
- `GET /viewpoints/{id}` - 获取观点详情
- `POST /viewpoints` - 创建观点
- `PUT /viewpoints/{id}` - 更新观点
- `DELETE /viewpoints/{id}` - 删除观点

#### 仪表盘
- `GET /dashboard` - 获取仪表盘数据
- `GET /dashboard/strategies/{id}` - 获取策略详情

## 数据库

项目使用 SQLite 作为数据库,数据文件为 `investlab.db`,位于后端项目根目录。

### 数据库表结构

- `account` - 账户表
- `strategy` - 策略表
- `asset` - 标的表
- `trade` - 交易表
- `viewpoint` - 观点表

详细的表结构请参考 `backend/src/main/resources/db/migration/V1__init_schema.sql`

## 开发规范

请遵循 `.cursorrules/project_rule.md` 中定义的开发规范:

- 代码风格: Kotlin使用标准规范,JavaScript使用ES2022
- 提交规范: 使用约定式提交(Conventional Commits)
- 分支策略: feature/功能名, fix/问题名
- API设计: RESTful风格,统一错误处理
- 测试要求: 单元测试覆盖率 >= 70%

## 测试

### 后端测试

```bash
cd backend
./gradlew test
```

### 前端测试

```bash
cd frontend
npm run test
```

## 构建

### 后端构建

```bash
cd backend
./gradlew build
```

构建产物位于 `backend/build/libs/`

### 前端构建

```bash
cd frontend
npm run build
```

构建产物位于 `frontend/dist/`

## 未来规划

- [ ] 自动行情同步
- [ ] 策略回测功能
- [ ] 资产配置图表
- [ ] API导入券商交易记录
- [ ] 移动端适配
- [ ] 多用户支持

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request!
