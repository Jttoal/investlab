# InvestLab 构建和运行指南

## 快速开始

### macOS / Linux

```bash
# 一键构建并运行
./build-and-run.sh
```

### Windows

```cmd
# 双击运行或在命令行执行
build-and-run.bat
```

## 脚本功能

构建脚本会自动完成以下操作:

1. ✅ **环境检查**
   - 检查 Java 21+ 是否安装
   - 检查 Node.js 18+ 是否安装
   - 检查 npm 是否可用

2. ✅ **后端构建**
   - 使用 Gradle 构建 Spring Boot 项目
   - 生成可执行的 jar 文件
   - 复制到 `dist` 目录

3. ✅ **前端构建**
   - 安装 npm 依赖(如果需要)
   - 使用 Vite 构建生产版本
   - 复制到 `dist` 目录

4. ✅ **生成启动脚本**
   - 创建后端启动脚本
   - 创建前端启动脚本
   - 创建一键启动脚本

5. ✅ **自动启动服务**(可选)
   - 启动后端服务(端口 8080)
   - 启动前端服务(端口 8081)
   - 自动打开浏览器

## 构建产物

构建完成后,`dist` 目录包含以下文件:

```
dist/
├── investlab-backend.jar    # 后端可执行 jar 文件
├── frontend/                # 前端静态文件
│   ├── index.html
│   ├── assets/
│   └── ...
├── start-all.sh/bat         # 一键启动脚本
├── start-backend.sh/bat     # 后端启动脚本
├── start-frontend.sh/bat    # 前端启动脚本
└── README.txt               # 使用说明
```

## 手动启动服务

如果选择不立即启动,可以稍后手动启动:

### 方式一: 一键启动(推荐)

**macOS/Linux:**
```bash
cd dist
./start-all.sh
```

**Windows:**
```cmd
cd dist
start-all.bat
```

### 方式二: 分别启动

**启动后端:**
```bash
# macOS/Linux
cd dist
./start-backend.sh

# Windows
cd dist
start-backend.bat
```

**启动前端:**
```bash
# macOS/Linux
cd dist
./start-frontend.sh

# Windows
cd dist
start-frontend.bat
```

## 访问应用

启动成功后,访问以下地址:

- **前端应用**: http://localhost:8081
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/api/v1/health

## 停止服务

### 一键启动模式
- 在终端按 `Ctrl+C` 停止所有服务

### 分别启动模式
- 关闭对应的终端窗口或按 `Ctrl+C`

### Windows 后台模式
- 使用任务管理器结束 `java.exe` 和 `python.exe` 进程

## 常见问题

### Q: 构建失败怎么办?

**A:** 检查以下几点:
1. Java 版本是否 >= 21: `java -version`
2. Node.js 版本是否 >= 18: `node -v`
3. 网络连接是否正常(需要下载依赖)
4. 磁盘空间是否充足

### Q: 端口被占用怎么办?

**A:** 修改端口号:
- 后端: 编辑 `backend/src/main/resources/application.yml` 中的 `server.port`
- 前端: 编辑启动脚本中的端口号(默认 8081)

### Q: 前端无法连接后端?

**A:** 检查:
1. 后端是否正常启动: 访问 http://localhost:8080/api/v1/health
2. 前端 API 地址配置: `frontend/src/utils/http.js` 中的 `baseURL`
3. 浏览器控制台是否有 CORS 错误

### Q: 数据库文件在哪里?

**A:** SQLite 数据库文件 `investlab.db` 会在后端启动目录自动创建:
- 开发模式: `backend/investlab.db`
- 生产模式: `dist/investlab.db`

### Q: 如何重置数据库?

**A:** 删除 `investlab.db` 文件,重启后端服务即可重新创建空数据库

### Q: Python 未安装怎么办?

**A:** 前端使用 Python 的简单 HTTP 服务器,如果没有 Python:
- 安装 Python 3: https://www.python.org/downloads/
- 或使用其他 HTTP 服务器,如 `npx serve frontend`

## 生产部署建议

### 后端部署

```bash
# 使用 nohup 后台运行
nohup java -jar investlab-backend.jar > backend.log 2>&1 &

# 或使用 systemd (Linux)
sudo systemctl start investlab-backend
```

### 前端部署

推荐使用专业的 Web 服务器:
- **Nginx**: 高性能,推荐用于生产环境
- **Apache**: 功能丰富,配置灵活
- **Caddy**: 自动 HTTPS,配置简单

示例 Nginx 配置:
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    root /path/to/dist/frontend;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 性能优化

### 后端优化

```bash
# 调整 JVM 参数
java -Xms512m -Xmx1024m -jar investlab-backend.jar
```

### 前端优化

- 启用 gzip 压缩
- 配置浏览器缓存
- 使用 CDN 加速静态资源

## 日志查看

### 后端日志
```bash
# 实时查看
tail -f dist/backend.log

# 查看最近 100 行
tail -n 100 dist/backend.log
```

### 前端日志
```bash
# 实时查看
tail -f dist/frontend.log
```

## 备份数据

重要数据文件:
- `investlab.db` - SQLite 数据库文件
- `backend.log` - 后端日志
- `frontend.log` - 前端日志

建议定期备份数据库文件:
```bash
# 创建备份
cp investlab.db investlab.db.backup.$(date +%Y%m%d)

# 恢复备份
cp investlab.db.backup.20251231 investlab.db
```

## 更新应用

1. 拉取最新代码
2. 重新运行构建脚本
3. 停止旧服务
4. 启动新服务

```bash
# 完整更新流程
git pull
./build-and-run.sh
```

## 技术支持

如有问题,请:
1. 查看日志文件
2. 检查 GitHub Issues
3. 提交新的 Issue

---

**祝使用愉快!** 🎉
