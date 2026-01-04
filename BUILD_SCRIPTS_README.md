# InvestLab 构建脚本说明

## 📦 脚本文件

| 文件名 | 平台 | 用途 |
|--------|------|------|
| `build-and-run.sh` | macOS/Linux | 一键构建和运行脚本 |
| `build-and-run.bat` | Windows | 一键构建和运行脚本 |
| `test-build.sh` | macOS/Linux | 环境检查脚本 |
| `BUILD_GUIDE.md` | 通用 | 详细使用指南 |

## 🚀 快速使用

### 第一步: 环境检查(可选)

**macOS/Linux:**
```bash
./test-build.sh
```

这会检查:
- ✅ Java 21+ 是否安装
- ✅ Node.js 18+ 是否安装
- ✅ Python 3 是否安装
- ✅ 项目结构是否完整

### 第二步: 构建和运行

**macOS/Linux:**
```bash
./build-and-run.sh
```

**Windows:**
```cmd
build-and-run.bat
```

### 第三步: 访问应用

构建完成并启动后:
- **前端应用**: http://localhost:8081
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/api/v1/health

## 📋 脚本功能详解

### build-and-run.sh / build-and-run.bat

这是主构建脚本,会执行以下步骤:

#### 1. 环境检查 ✓
- 检查 Java 版本(需要 21+)
- 检查 Node.js 版本(需要 18+)
- 验证必要的命令是否可用

#### 2. 后端构建 ✓
```bash
cd backend
./gradlew clean build -x test
```
- 清理旧的构建文件
- 编译 Kotlin 代码
- 打包成可执行 jar 文件
- 跳过测试以加快构建速度

#### 3. 前端构建 ✓
```bash
cd frontend
npm install  # 如果需要
npm run build
```
- 安装依赖(首次或依赖变更时)
- 使用 Vite 构建生产版本
- 生成优化后的静态文件

#### 4. 生成启动脚本 ✓

在 `dist` 目录生成:
- `start-all.sh/bat` - 一键启动所有服务
- `start-backend.sh/bat` - 单独启动后端
- `start-frontend.sh/bat` - 单独启动前端
- `README.txt` - 使用说明

#### 5. 可选启动 ✓

脚本会询问是否立即启动服务:
- 选择 `y`: 自动启动后端和前端,并打开浏览器
- 选择 `n`: 仅构建,稍后手动启动

## 📁 构建产物

构建完成后,`dist` 目录结构:

```
dist/
├── investlab-backend.jar      # 后端可执行文件 (~50MB)
├── frontend/                  # 前端静态文件 (~2MB)
│   ├── index.html
│   ├── assets/
│   │   ├── index-xxx.js
│   │   ├── index-xxx.css
│   │   └── ...
│   └── vite.svg
├── start-all.sh               # 一键启动脚本(Linux/Mac)
├── start-all.bat              # 一键启动脚本(Windows)
├── start-backend.sh           # 后端启动脚本(Linux/Mac)
├── start-backend.bat          # 后端启动脚本(Windows)
├── start-frontend.sh          # 前端启动脚本(Linux/Mac)
├── start-frontend.bat         # 前端启动脚本(Windows)
├── README.txt                 # 使用说明
├── backend.log                # 后端日志(启动后生成)
├── frontend.log               # 前端日志(启动后生成)
└── investlab.db               # SQLite数据库(启动后生成)
```

## 🎯 启动脚本说明

### start-all.sh/bat (一键启动)

**功能:**
- 启动后端服务(端口 8080)
- 等待后端就绪
- 启动前端服务(端口 8081)
- 自动打开浏览器

**使用:**
```bash
cd dist
./start-all.sh    # macOS/Linux
start-all.bat     # Windows
```

**停止:**
- macOS/Linux: 按 `Ctrl+C`
- Windows: 关闭窗口或任务管理器结束进程

### start-backend.sh/bat (后端启动)

**功能:**
- 启动 Spring Boot 应用
- 监听 8080 端口
- 自动创建数据库

**使用:**
```bash
cd dist
./start-backend.sh    # macOS/Linux
start-backend.bat     # Windows
```

**验证:**
```bash
curl http://localhost:8080/api/v1/health
```

### start-frontend.sh/bat (前端启动)

**功能:**
- 使用 Python HTTP 服务器
- 监听 8081 端口
- 提供静态文件服务

**使用:**
```bash
cd dist
./start-frontend.sh    # macOS/Linux
start-frontend.bat     # Windows
```

**访问:**
打开浏览器访问 http://localhost:8081

## 🔧 自定义配置

### 修改端口

**后端端口:**
编辑 `backend/src/main/resources/application.yml`:
```yaml
server:
  port: 8080  # 改为其他端口
```

**前端端口:**
编辑启动脚本中的端口号:
```bash
# 将 8081 改为其他端口
python3 -m http.server 8081
```

### 修改 API 地址

如果后端端口改变,需要更新前端配置:

编辑 `frontend/src/utils/http.js`:
```javascript
const http = axios.create({
  baseURL: 'http://localhost:8080',  // 修改为新端口
  // ...
})
```

然后重新构建前端。

### JVM 参数调整

编辑 `dist/start-backend.sh`:
```bash
java -Xms512m -Xmx1024m -jar investlab-backend.jar
```

## 🐛 故障排查

### 问题 1: 构建失败

**症状:** 脚本报错退出

**排查步骤:**
1. 运行 `./test-build.sh` 检查环境
2. 检查 Java 版本: `java -version`
3. 检查 Node.js 版本: `node -v`
4. 检查网络连接(需要下载依赖)
5. 查看详细错误信息

### 问题 2: 后端启动失败

**症状:** 访问 http://localhost:8080 无响应

**排查步骤:**
1. 查看 `dist/backend.log` 日志
2. 检查端口是否被占用: `lsof -i :8080`
3. 检查 Java 版本是否正确
4. 检查数据库文件权限

### 问题 3: 前端启动失败

**症状:** 访问 http://localhost:8081 无响应

**排查步骤:**
1. 查看 `dist/frontend.log` 日志
2. 检查 Python 是否安装: `python3 -V`
3. 检查端口是否被占用: `lsof -i :8081`
4. 尝试手动启动: `cd dist/frontend && python3 -m http.server 8081`

### 问题 4: 前端无法连接后端

**症状:** 前端页面加载但 API 调用失败

**排查步骤:**
1. 确认后端正常运行: `curl http://localhost:8080/api/v1/health`
2. 检查浏览器控制台错误
3. 检查 CORS 配置
4. 确认 API 地址配置正确

### 问题 5: 数据库错误

**症状:** 后端日志显示数据库错误

**解决方案:**
```bash
# 删除数据库文件重新创建
cd dist
rm investlab.db
./start-backend.sh
```

## 📊 性能优化

### 构建速度优化

**跳过测试:**
```bash
# 已默认跳过测试
./gradlew build -x test
```

**并行构建:**
```bash
# 在 gradle.properties 中添加
org.gradle.parallel=true
org.gradle.caching=true
```

### 运行时优化

**后端 JVM 参数:**
```bash
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -jar investlab-backend.jar
```

**前端服务器:**
使用更高性能的服务器替代 Python:
```bash
# 使用 serve
npx serve -s frontend -p 8081

# 使用 http-server
npx http-server frontend -p 8081
```

## 📝 最佳实践

### 开发环境

使用开发模式获得更好的开发体验:
```bash
# 后端 - 支持热重载
cd backend
./gradlew bootRun

# 前端 - 支持热更新
cd frontend
npm run dev
```

### 生产环境

使用构建脚本生成优化的生产版本:
```bash
./build-and-run.sh
```

### CI/CD 集成

在 CI/CD 管道中使用:
```yaml
# GitHub Actions 示例
- name: Build InvestLab
  run: |
    chmod +x build-and-run.sh
    ./build-and-run.sh
    # 选择 n 不启动服务
```

## 🎓 进阶使用

### Docker 部署

创建 Dockerfile:
```dockerfile
FROM openjdk:21-slim
COPY dist/investlab-backend.jar /app/
WORKDIR /app
EXPOSE 8080
CMD ["java", "-jar", "investlab-backend.jar"]
```

### Nginx 反向代理

```nginx
server {
    listen 80;
    
    location / {
        root /path/to/dist/frontend;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
    }
}
```

## 📚 相关文档

- [BUILD_GUIDE.md](./BUILD_GUIDE.md) - 详细构建指南
- [README.md](./README.md) - 项目主文档
- [QUICKSTART.md](./QUICKSTART.md) - 快速开始指南
- [docs/api-examples.md](./docs/api-examples.md) - API 使用示例

## 🆘 获取帮助

如有问题:
1. 查看日志文件
2. 阅读相关文档
3. 提交 GitHub Issue

---

**祝构建顺利!** 🚀
