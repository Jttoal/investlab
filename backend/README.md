# InvestLab Backend

## 技术栈

- Kotlin 2.0.0
- Spring Boot 3.3.0
- Spring Data JPA
- SQLite
- Flyway
- Gradle 9.0

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/com/investlab/
│   │   │   ├── api/              # REST Controllers
│   │   │   ├── service/          # 业务逻辑
│   │   │   ├── repository/       # 数据访问
│   │   │   ├── model/            # 实体和DTO
│   │   │   ├── config/           # 配置
│   │   │   ├── exception/        # 异常处理
│   │   │   └── InvestLabApplication.kt
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/     # Flyway迁移
│   └── test/                     # 单元测试
└── build.gradle.kts
```

## 启动应用

```bash
./gradlew bootRun
```

应用将在 `http://localhost:8080` 启动

## 运行测试

```bash
./gradlew test
```

## 构建

```bash
./gradlew build
```

## API文档

所有API使用统一的错误响应格式:

```json
{
  "code": "ERROR_CODE",
  "message": "错误信息",
  "traceId": "追踪ID",
  "timestamp": "2025-12-31T10:00:00"
}
```

### 账户管理 API

**获取所有账户**
```
GET /api/v1/accounts
```

**创建账户**
```
POST /api/v1/accounts
Content-Type: application/json

{
  "name": "招商证券",
  "broker": "招商证券",
  "currency": "CNY",
  "balanceManual": 10000.00
}
```

### 策略管理 API

**获取所有策略**
```
GET /api/v1/strategies
GET /api/v1/strategies?type=index
```

**创建策略**
```
POST /api/v1/strategies
Content-Type: application/json

{
  "name": "沪深300定投",
  "type": "index",
  "goalNote": "长期定投沪深300指数"
}
```

### 更多API

详见主项目 README.md

## 数据库迁移

使用 Flyway 管理数据库版本:

- 迁移脚本位于 `src/main/resources/db/migration/`
- 命名规则: `V{版本号}__{描述}.sql`
- 首次启动时自动执行迁移

## 配置

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:sqlite:investlab.db
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

### 环境配置

- 开发环境: `application-dev.yml`
- 生产环境: `application-prod.yml`

## 开发规范

1. **包结构**: 按功能模块组织,不按技术层
2. **命名规范**: 
   - Entity: 名词单数
   - Repository: EntityRepository
   - Service: EntityService
   - Controller: EntityController
3. **异常处理**: 使用统一的 `@RestControllerAdvice`
4. **日志**: 使用 SLF4J,生产环境 INFO 级别
5. **测试**: 使用 JUnit5 + MockK,覆盖率 >= 70%
