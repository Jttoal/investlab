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
LOG_PATH="$PROJECT_ROOT/.cursor/debug.log"

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

#region agent log
dbg_log() {
    local hypothesis_id="$1"
    local message="$2"
    local data="$3" # JSON string or empty
    local ts
    ts=$(date +%s%3N)
    local payload
    if [ -z "$data" ]; then
        payload="{\"sessionId\":\"debug-session\",\"runId\":\"attach\",\"hypothesisId\":\"${hypothesis_id}\",\"location\":\"build-and-run.sh\",\"message\":\"${message}\",\"data\":{},\"timestamp\":${ts}}"
    else
        payload="{\"sessionId\":\"debug-session\",\"runId\":\"attach\",\"hypothesisId\":\"${hypothesis_id}\",\"location\":\"build-and-run.sh\",\"message\":\"${message}\",\"data\":${data},\"timestamp\":${ts}}"
    fi
    echo "$payload" >> "$LOG_PATH" 2>/dev/null || true
}
#endregion

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

MODE="${1:-run}"

usage() {
    echo "用法: $0 [run|debug]"
    echo "  run   : 打包前后端并产出 dist，启动集成包"
    echo "  debug : 启动前端 dev server + 后端 JDWP (5005)，日志输出控制台，可在 VSCode 断点调试"
}

echo "======================================"
echo "   InvestLab 构建和运行脚本"
echo "======================================"
echo ""

check_command java

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    log_error "Java 版本需要 21 或更高,当前版本: $JAVA_VERSION"
    exit 1
fi
log_success "Java 版本检查通过: $JAVA_VERSION"

if [ "$MODE" = "run" ]; then
    log_info "模式: run（构建发布包并内置前端）"
    log_info "创建输出目录: $DIST_DIR..."
    rm -rf "$DIST_DIR"
    mkdir -p "$DIST_DIR"

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

    log_info "创建启动脚本..."
    cat > "$DIST_DIR/start-all.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

echo "======================================"
echo "   InvestLab 启动脚本"
echo "======================================"
echo ""

echo "正在启动后端服务..."
java -jar investlab-backend.jar > backend.log 2>&1 &
BACKEND_PID=$!
echo "后端服务已启动 (PID: $BACKEND_PID)"
echo "访问地址: http://localhost:8080 (前后端一体)"
echo "日志文件: backend.log"
echo ""

echo "等待后端服务启动..."
sleep 5

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

cleanup() {
    echo ""
    echo "正在停止服务..."
    kill $BACKEND_PID 2>/dev/null
    echo "服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

wait
EOF
    chmod +x "$DIST_DIR/start-all.sh"

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
    echo "  - README.txt               (使用说明)"
    echo ""

elif [ "$MODE" = "debug" ]; then
    log_info "模式: debug（前端 dev server + 后端 JDWP，日志输出控制台）"
    check_command npm

    find_free_port() {
        python3 - "$1" "$2" <<'PY'
import sys, socket
start = int(sys.argv[1]); end = int(sys.argv[2])
for p in range(start, end + 1):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind(("0.0.0.0", p))
        except OSError:
            continue
        print(p)
        sys.exit(0)
sys.exit(1)
PY
    }

    DEBUG_PORT=$(find_free_port 5005 5015)
    if [ -z "$DEBUG_PORT" ]; then
        log_error "未找到可用的 JDWP 端口(5005-5015)，请释放端口后重试"
        exit 1
    fi
    dbg_log "H_attach_port" "jdwp port selected" "{\"port\":\"$DEBUG_PORT\"}"
    log_info "JDWP 端口: $DEBUG_PORT (已检测可绑定)"
    log_info "JDWP Attach -> host: 127.0.0.1  port: $DEBUG_PORT"

    cd "$PROJECT_ROOT/frontend"
    if [ ! -d "node_modules" ]; then
        log_info "检测到前端缺少 node_modules，正在 npm install ..."
        npm install
    fi

    FRONT_PORT=5173
    log_info "启动前端 dev server (npm run dev -- --host --port $FRONT_PORT)..."
    npm run dev -- --host --port "$FRONT_PORT" &
    FRONTEND_PID=$!
    log_success "前端 dev server 已启动 (PID: $FRONTEND_PID)，访问: http://localhost:$FRONT_PORT"

    cd "$BACKEND_DIR"
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        log_error "gradlew 文件不存在,请检查项目根目录"
        exit 1
    fi
    chmod +x "$PROJECT_ROOT/gradlew"

    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
    log_info "启动后端（JDWP $DEBUG_PORT，可在 VSCode 远程调试），日志输出控制台..."
    dbg_log "H_attach_boot" "bootRun start" "{\"port\":\"$DEBUG_PORT\"}"
    SPRING_ARGS="--spring.profiles.active=dev"

    wait_for_port() {
        local port="$1"
        python3 - "$port" <<'PY'
import sys, socket
p = int(sys.argv[1])
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(1.0)
try:
    s.connect(("127.0.0.1", p))
    sys.exit(0)
except OSError:
    sys.exit(1)
finally:
    s.close()
PY
    }

    # 后端后台启动，监听端口后再提示 VSCode 附加
    JAVA_TOOL_OPTIONS="" "$PROJECT_ROOT/gradlew" :backend:bootRun -x test --no-daemon --args="$SPRING_ARGS" -Dspring-boot.run.jvmArguments="$DEBUG_OPTS" &
    BACKEND_PID=$!

    log_info "等待后端 JDWP 监听在端口 $DEBUG_PORT ..."
    JDWP_READY=0
    for i in {1..30}; do
        if wait_for_port "$DEBUG_PORT"; then
            JDWP_READY=1
            log_success "JDWP 已监听: $DEBUG_PORT，可在 VSCode 附加"
            dbg_log "H_attach_port" "jdwp listen ready" "{\"port\":\"$DEBUG_PORT\"}"
            break
        fi
        sleep 1
    done

    if [ $JDWP_READY -eq 0 ]; then
        log_warning "30 秒内未检测到 JDWP 监听，请检查后端日志"
        dbg_log "H_attach_port" "jdwp listen timeout" "{\"port\":\"$DEBUG_PORT\"}"
    fi

    # 如果后端进程退出，清理前端
    cleanup() {
        echo ""
        echo "正在停止服务..."
        if [ ! -z "$FRONTEND_PID" ]; then
            kill $FRONTEND_PID 2>/dev/null
        fi
        if [ ! -z "$BACKEND_PID" ]; then
            kill $BACKEND_PID 2>/dev/null
        fi
        echo "服务已停止"
        exit 0
    }
    trap cleanup SIGINT SIGTERM
    wait $BACKEND_PID
else
    usage
    exit 1
fi
