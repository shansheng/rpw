#!/bin/bash
# RPW数据库初始化脚本
# Usage: ./init-db.sh [mysql_host] [mysql_port] [mysql_user] [mysql_password]

# ========== 配置 ==========
MYSQL_HOST=${1:-localhost}
MYSQL_PORT=${2:-3306}
MYSQL_USER=${3:-root}
MYSQL_PASSWORD=${4:-FundTracker2024!}
DATABASE_NAME=rpw_db

# SQL 文件列表（按顺序执行）
SQL_FILES=(
    "01_schema.sql"
    "fix-ddl-columns.sql"
    "update-resource-plan-material.sql"
    "phase6-update.sql"
    "phase7-report.sql"
)

# SQL 文件所在目录（相对于脚本位置的上级目录）
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
SQL_DIR=$(cd "${SCRIPT_DIR}/../../sql" && pwd)

# ========== 颜色 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ========== 函数 ==========
info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

mysql_exec() {
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$@" 2>&1
}

# ========== 检查 ==========
echo "======================================"
echo "  RPW 数据库初始化"
echo "======================================"
echo ""
info "连接: ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}"
info "数据库: ${DATABASE_NAME}"
echo ""

if ! command -v mysql &> /dev/null; then
    error "未找到 mysql 命令，请安装 MySQL 客户端"
    exit 1
fi

# 测试连接
info "测试数据库连接..."
if ! mysql_exec -e "SELECT 1" > /dev/null 2>&1; then
    error "无法连接数据库，请检查参数"
    exit 1
fi
info "数据库连接成功"

# 创建数据库
info "创建数据库 ${DATABASE_NAME}..."
mysql_exec -e "CREATE DATABASE IF NOT EXISTS \`${DATABASE_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if [[ $? -eq 0 ]]; then
    info "数据库 ${DATABASE_NAME} 已就绪"
else
    error "创建数据库失败"
    exit 1
fi

echo ""
echo "--------------------------------------"

# 逐个执行 SQL 文件
success_count=0
fail_count=0

for sql_file in "${SQL_FILES[@]}"; do
    sql_path="${SQL_DIR}/${sql_file}"
    echo ""
    info "执行: ${sql_file}"

    if [[ ! -f "$sql_path" ]]; then
        error "文件不存在: ${sql_path}"
        fail_count=$((fail_count + 1))
        continue
    fi

    mysql_exec "$DATABASE_NAME" < "$sql_path"
    if [[ $? -eq 0 ]]; then
        info "${sql_file} - 执行成功"
        success_count=$((success_count + 1))
    else
        error "${sql_file} - 执行失败"
        fail_count=$((fail_count + 1))
    fi
done

echo ""
echo "======================================"
echo "  执行完成"
echo "  成功: ${success_count}  失败: ${fail_count}"
echo "======================================"

if [[ $fail_count -gt 0 ]]; then
    exit 1
fi
exit 0
