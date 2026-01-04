#!/bin/bash

# InvestLab 构建测试脚本
# 用途: 快速验证构建脚本是否正常工作

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "======================================"
echo "   InvestLab 构建测试"
echo "======================================"
echo ""

# 测试环境检查
echo -e "${BLUE}[TEST]${NC} 检查 Java..."
if java -version 2>&1 | grep -q "version"; then
    echo -e "${GREEN}[PASS]${NC} Java 已安装"
else
    echo -e "${RED}[FAIL]${NC} Java 未安装"
    exit 1
fi

echo -e "${BLUE}[TEST]${NC} 检查 Node.js..."
if node -v &> /dev/null; then
    echo -e "${GREEN}[PASS]${NC} Node.js 已安装"
else
    echo -e "${RED}[FAIL]${NC} Node.js 未安装"
    exit 1
fi

echo -e "${BLUE}[TEST]${NC} 检查 Python..."
if python3 -V &> /dev/null || python -V &> /dev/null; then
    echo -e "${GREEN}[PASS]${NC} Python 已安装"
else
    echo -e "${RED}[FAIL]${NC} Python 未安装(前端服务需要)"
fi

# 测试项目结构
echo ""
echo -e "${BLUE}[TEST]${NC} 检查项目结构..."
if [ -d "backend" ] && [ -d "frontend" ]; then
    echo -e "${GREEN}[PASS]${NC} 项目结构正确"
else
    echo -e "${RED}[FAIL]${NC} 项目结构不完整"
    exit 1
fi

# 测试 Gradle
echo -e "${BLUE}[TEST]${NC} 检查 Gradle..."
if [ -f "backend/gradlew" ]; then
    echo -e "${GREEN}[PASS]${NC} Gradle wrapper 存在"
else
    echo -e "${RED}[FAIL]${NC} Gradle wrapper 不存在"
    exit 1
fi

# 测试前端依赖
echo -e "${BLUE}[TEST]${NC} 检查前端配置..."
if [ -f "frontend/package.json" ]; then
    echo -e "${GREEN}[PASS]${NC} package.json 存在"
else
    echo -e "${RED}[FAIL]${NC} package.json 不存在"
    exit 1
fi

# 测试构建脚本
echo -e "${BLUE}[TEST]${NC} 检查构建脚本..."
if [ -f "build-and-run.sh" ] && [ -x "build-and-run.sh" ]; then
    echo -e "${GREEN}[PASS]${NC} 构建脚本存在且可执行"
else
    echo -e "${RED}[FAIL]${NC} 构建脚本不存在或不可执行"
    exit 1
fi

echo ""
echo "======================================"
echo -e "${GREEN}[SUCCESS]${NC} 所有测试通过!"
echo "======================================"
echo ""
echo "可以运行以下命令开始构建:"
echo "  ./build-and-run.sh"
echo ""
