#!/bin/bash
# RPW前端构建脚本
# Usage: ./build-frontend.sh [output_dir]

# ========== 配置 ==========
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/../.." && pwd)
FRONTEND_DIR="${PROJECT_ROOT}/frontend"
OUTPUT_DIR=${1:-/opt/rpw/web/dist}

# ========== 颜色 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# ========== 检查 Node.js ==========
echo "======================================"
echo "  RPW 前端构建"
echo "======================================"

if [[ ! -d "$FRONTEND_DIR" ]]; then
    error "前端目录不存在: ${FRONTEND_DIR}"
    exit 1
fi

if ! command -v node &> /dev/null; then
    error "未找到 Node.js，请安装 Node.js 18+"
    exit 1
fi

NODE_VERSION=$(node -v | sed 's/v//' | cut -d. -f1)
if [[ "$NODE_VERSION" -lt 18 ]]; then
    error "Node.js 版本过低: $(node -v)，需要 >= 18"
    exit 1
fi
info "Node.js: $(node -v)"
info "前端目录: ${FRONTEND_DIR}"

cd "$FRONTEND_DIR" || exit 1

# ========== 安装依赖 ==========
echo ""
info "安装依赖..."
npm install --production=false
if [[ $? -ne 0 ]]; then
    error "依赖安装失败"
    exit 1
fi

# ========== 构建 ==========
echo ""
info "开始构建..."
npm run build
if [[ $? -ne 0 ]]; then
    error "构建失败"
    exit 1
fi
info "构建成功"

# ========== 复制产物 ==========
if [[ -d "${FRONTEND_DIR}/dist" ]]; then
    mkdir -p "$OUTPUT_DIR"
    echo ""
    info "复制到 ${OUTPUT_DIR}..."
    cp -r "${FRONTEND_DIR}/dist/"* "$OUTPUT_DIR/"

    # 显示产物大小
    BUILD_SIZE=$(du -sh "$OUTPUT_DIR" 2>/dev/null | cut -f1)
    FILE_COUNT=$(find "$OUTPUT_DIR" -type f | wc -l | tr -d ' ')
    info "部署目录大小: ${BUILD_SIZE} (${FILE_COUNT} 个文件)"
else
    warn "未找到 dist/ 目录"
fi

echo ""
echo "======================================"
echo "  前端构建完成"
echo "======================================"
