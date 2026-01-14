#!/bin/bash

# InvestLab 构建和运行脚本
# 用途: 打包前端和后端项目,并启动服务供本地测试

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
DIST_DIR="$PROJECT_ROOT/dist"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装,请先安装 $1"
        exit 1
    fi
}

# 清理函数
cleanup() {
    log_info "正在清理进程..."
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    exit 0
}

# 捕获退出信号
trap cleanup SIGINT SIGTERM

echo "======================================"
echo "   InvestLab 构建和运行脚本"
echo "======================================"
echo ""

# 1. 检查依赖
log_info "检查系统依赖..."
check_command java
log_info "前端构建由 Gradle Node 插件自动处理，无需本地安装 Node/npm"

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    log_error "Java 版本需要 21 或更高,当前版本: $JAVA_VERSION"
    exit 1
fi
log_success "Java 版本检查通过: $JAVA_VERSION"

# 2. 创建输出目录
log_info "创建输出目录: $DIST_DIR..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# 3. 使用 Gradle 构建前后端（前端由 Node 插件执行 npm 构建）
log_info "使用 Gradle 构建前后端项目（前端资源将打包进后端静态目录）..."
cd "$PROJECT_ROOT"

if [ ! -f "gradlew" ]; then
    log_error "gradlew 文件不存在,请检查项目根目录"
    exit 1
fi

chmod +x gradlew
./gradlew :backend:clean :backend:bootJar -x test

if [ $? -eq 0 ]; then
    log_success "Gradle 构建成功（包含前端资源）"
    # 仅复制可执行的 bootJar, 避免同时匹配 plain jar
    BOOT_JAR=$(find "$BACKEND_DIR/build/libs" -maxdepth 1 -name "*-0.0.1.jar" ! -name "*plain*" -print -quit 2>/dev/null)
    if [ -n "$BOOT_JAR" ] && [ -f "$BOOT_JAR" ]; then
        cp "$BOOT_JAR" "$DIST_DIR/investlab-backend.jar"
        log_success "后端 jar 文件已复制到 dist 目录"
    else
        log_error "未找到 bootJar 产物, 请检查 backend/build/libs"
        exit 1
    fi
else
    log_error "Gradle 构建失败"
    exit 1
fi

# 4. 创建启动脚本
log_info "创建启动脚本..."

# 创建一键启动脚本
cat > "$DIST_DIR/start-all.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

echo "======================================"
echo "   InvestLab 启动脚本"
echo "======================================"
echo ""

# 启动后端（已内置前端静态资源）
echo "正在启动后端服务..."
java -jar investlab-backend.jar > backend.log 2>&1 &
BACKEND_PID=$!
echo "后端服务已启动 (PID: $BACKEND_PID)"
echo "访问地址: http://localhost:8080 (前后端一体)"
echo "日志文件: backend.log"
echo ""

# 等待后端启动
echo "等待后端服务启动..."
sleep 5

# 检查后端是否启动成功
if curl -s http://localhost:8080/api/v1/health > /dev/null; then
    echo "✓ 后端服务启动成功"
else
    echo "✗ 后端服务启动失败,请查看 backend.log"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi

echo ""
echo "======================================"
echo "   服务启动完成!"
echo "======================================"
echo ""
echo "后端/前端访问: http://localhost:8080"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo ""

# 清理函数
cleanup() {
    echo ""
    echo "正在停止服务..."
    kill $BACKEND_PID 2>/dev/null
    echo "服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

# 保持脚本运行
wait
EOF
chmod +x "$DIST_DIR/start-all.sh"

# 创建 README
cat > "$DIST_DIR/README.txt" << 'EOF'
InvestLab 运行说明
==================

目录结构:
- investlab-backend.jar    后端 jar 文件
- start-all.sh             一键启动后端(仅 macOS/Linux)

快速启动:
---------

一键启动(推荐 - macOS/Linux)
./start-all.sh


访问地址:
---------
前后端统一入口: http://localhost:8080

健康检查:
---------
访问 http://localhost:8080/api/v1/health 检查后端是否正常运行

注意事项:
---------
1. 需要安装 Java 21+
2. 确保 8080 端口未被占用
3. 数据库文件 investlab.db 会在当前目录自动创建

停止服务:
---------
- 一键启动模式: 按 Ctrl+C
- 分别启动模式: 关闭对应的终端窗口或 Ctrl+C

故障排查:
---------
1. 如果后端启动失败,检查 backend.log
2. 如果端口被占用,修改启动脚本中的端口号

更多信息请访问项目文档。
EOF

log_success "启动脚本创建完成"

# 5. 显示构建结果
echo ""
echo "======================================"
log_success "构建完成!"
echo "======================================"
echo ""
log_info "构建产物位置: $DIST_DIR"
echo ""
echo "文件列表:"
echo "  - investlab-backend.jar    (后端 jar 文件)"
echo "  - start-all.sh             (一键启动脚本，内置前端)"
echo "  - start-backend.sh/bat     (后端启动脚本，内置前端)"
echo "  - README.txt               (使用说明)"
echo ""

# 6. 询问是否立即启动
read -p "是否立即启动服务进行测试? (y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "正在启动服务..."
    cd "$DIST_DIR"
    
    # 启动后端（内置前端）
    log_info "启动后端服务..."
    java -jar $DIST_DIR/investlab-backend.jar > logs/backend.log 2>&1 &
    BACKEND_PID=$!
    log_success "后端服务已启动 (PID: $BACKEND_PID)"
    
    # 等待后端启动
    log_info "等待后端服务启动..."
    sleep 8
    
    # 检查后端健康状态
    if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
        log_success "后端服务健康检查通过"
    else
        log_warning "后端服务可能未完全启动,请稍等或查看 backend.log"
    fi
    
    echo ""
    echo "======================================"
    log_success "服务启动完成!"
    echo "======================================"
    echo ""
    echo "统一入口: ${GREEN}http://localhost:8080${NC}"
    echo ""
    echo "健康检查: ${BLUE}http://localhost:8080/api/v1/health${NC}"
    echo ""
    log_info "日志文件:"
    echo "  - $DIST_DIR/backend.log"
    echo ""
    log_warning "按 Ctrl+C 停止服务"
    echo ""
    
    # 尝试打开浏览器
    if command -v open &> /dev/null; then
        sleep 2
        open http://localhost:8080
    elif command -v xdg-open &> /dev/null; then
        sleep 2
        xdg-open http://localhost:8080
    fi
    
    # 保持脚本运行
    wait
else
    echo ""
    log_info "稍后可以进入 dist 目录运行启动脚本:"
    echo "  cd $DIST_DIR"
    echo "  ./start-all.sh"
    echo ""
fi
