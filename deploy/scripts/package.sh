#!/bin/bash
# RPW打包发布脚本 - 将项目打包为发布包
# Usage: ./package.sh

# ========== 配置 ==========
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/../.." && pwd)
VERSION="v1.0.0"
DATE=$(date +%Y%m%d)
PACKAGE_NAME="rpw-${VERSION}-${DATE}"
RELEASE_DIR="${PROJECT_ROOT}/release"
PACKAGE_PATH="${RELEASE_DIR}/${PACKAGE_NAME}.tar.gz"

# ========== 颜色 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# ========== 开始 ==========
echo "======================================"
echo "  RPW 打包发布"
echo "  版本: ${VERSION}  日期: ${DATE}"
echo "======================================"

STAGING_DIR=$(mktemp -d)
info "临时目录: ${STAGING_DIR}"

# 收集文件
mkdir -p "${STAGING_DIR}/sql"
mkdir -p "${STAGING_DIR}/deploy/scripts"
mkdir -p "${STAGING_DIR}/backend"
mkdir -p "${STAGING_DIR}/deploy"

# 1. JAR 文件
if [[ -f "${PROJECT_ROOT}/backend/target/rpw-1.0.0.jar" ]]; then
    cp "${PROJECT_ROOT}/backend/target/rpw-1.0.0.jar" "${STAGING_DIR}/backend/"
    info "已包含: rpw-1.0.0.jar"
else
    warn "未找到 rpw-1.0.0.jar（需先构建后端）"
fi

# 2. SQL 文件
if [[ -d "${PROJECT_ROOT}/sql" ]]; then
    cp "${PROJECT_ROOT}/sql/"*.sql "${STAGING_DIR}/sql/" 2>/dev/null
    info "已包含: SQL 文件 ($(ls ${STAGING_DIR}/sql/*.sql 2>/dev/null | wc -l | tr -d ' ') 个)"
fi

# 3. 部署脚本
cp "${PROJECT_ROOT}/deploy/scripts/"* "${STAGING_DIR}/deploy/scripts/" 2>/dev/null
info "已包含: 部署脚本"

# 4. BPMN 文件
if [[ -d "${PROJECT_ROOT}/backend/src/main/resources/processes" ]]; then
    mkdir -p "${STAGING_DIR}/backend/processes"
    cp "${PROJECT_ROOT}/backend/src/main/resources/processes/"*.bpmn20.xml "${STAGING_DIR}/backend/processes/" 2>/dev/null
    info "已包含: BPMN 流程文件"
fi

# 5. 部署文档
if [[ -f "${PROJECT_ROOT}/deploy/DEPLOYMENT.md" ]]; then
    cp "${PROJECT_ROOT}/deploy/DEPLOYMENT.md" "${STAGING_DIR}/deploy/"
    info "已包含: DEPLOYMENT.md"
else
    warn "未找到 DEPLOYMENT.md"
fi

# ========== 打包 ==========
echo ""
info "正在打包..."
mkdir -p "$RELEASE_DIR"
tar -czf "$PACKAGE_PATH" -C "$STAGING_DIR" .
PACKAGE_SIZE=$(ls -lh "$PACKAGE_PATH" | awk '{print $5}')

# 清理临时目录
rm -rf "$STAGING_DIR"

echo ""
echo "======================================"
info "打包完成: ${PACKAGE_NAME}.tar.gz (${PACKAGE_SIZE})"
echo "  位置: ${PACKAGE_PATH}"
echo "======================================"
