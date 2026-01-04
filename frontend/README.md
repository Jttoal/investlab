# InvestLab Frontend

## 技术栈

- Vue 3.5
- Vite 7.2
- Pinia 3.0
- Vue Router 4.6
- Axios 1.13

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口封装
│   ├── stores/           # Pinia状态管理
│   ├── views/            # 页面组件
│   ├── components/       # 可复用组件
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   ├── assets/           # 静态资源
│   ├── App.vue           # 根组件
│   ├── main.js           # 入口文件
│   └── style.css         # 全局样式
├── public/               # 公共资源
├── index.html
├── package.json
└── vite.config.js
```

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 目录说明

### api/
API接口封装,每个模块一个文件:
- `account.js` - 账户相关接口
- `strategy.js` - 策略相关接口
- `asset.js` - 标的相关接口
- `trade.js` - 交易相关接口
- `viewpoint.js` - 观点相关接口
- `dashboard.js` - 仪表盘接口

### stores/
Pinia状态管理,使用 Composition API 风格:
- `account.js` - 账户状态
- `strategy.js` - 策略状态
- `dashboard.js` - 仪表盘状态

### views/
页面组件:
- `Dashboard.vue` - 仪表盘
- `AccountList.vue` - 账户列表
- `StrategyList.vue` - 策略列表
- `StrategyDetail.vue` - 策略详情

### components/
可复用组件:
- `AssetModal.vue` - 标的编辑模态框
- `TradeModal.vue` - 交易记录模态框
- `ViewpointModal.vue` - 观点记录模态框

## 开发规范

### 组件命名
- 使用 PascalCase: `MyComponent.vue`
- 页面组件放在 `views/`
- 复用组件放在 `components/`

### 变量命名
- 使用 camelCase
- 布尔值以 `is/has/should` 开头

### 样式规范
- 使用 scoped 样式
- 遵循 BEM 命名规范
- 使用 CSS 变量管理主题色

### API调用
- 统一通过 `api/` 目录的函数调用
- 不在组件中直接使用 axios
- 错误处理由 HTTP 拦截器统一处理

### 状态管理
- 只放共享的业务状态
- 纯 UI 状态放组件内部
- 通过 actions 修改状态

## 环境变量

创建 `.env.development` 和 `.env.production`:

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

## 构建优化

- 代码分割: 路由级别懒加载
- Tree Shaking: 自动移除未使用代码
- 压缩: 生产构建自动压缩
- 目标大小: < 2MB gzip

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90
