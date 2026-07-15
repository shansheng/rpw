#!/bin/bash
# RPW后端启动脚本
# Usage: ./deploy.sh [start|stop|restart|status]

# ========== 配置 ==========
APP_HOME=${APP_HOME:-/opt/rpw/app}
JAR_NAME=rpw-1.0.0.jar
JAR_PATH=${APP_HOME}/${JAR_NAME}
PID_FILE=${APP_HOME}/rpw.pid
LOG_DIR=${APP_HOME}/logs
LOG_FILE=${LOG_DIR}/rpw.log

# JVM 参数
JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Spring Boot 配置
SPRING_OPTS="--spring.profiles.active=prod"

# ========== 颜色 ==========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ========== 函数 ==========
info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

detect_java() {
    if command -v java_home &> /dev/null; then
        export JAVA_HOME=$(java_home -v 17 2>/dev/null)
        if [[ -n "$JAVA_HOME" && -x "${JAVA_HOME}/bin/java" ]]; then
            info "使用 JAVA_HOME: ${JAVA_HOME}"
            return 0
        fi
    fi
    # 回退到系统 java
    if command -v java &> /dev/null; then
        export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java) 2>/dev/null || which java)))
        info "使用系统 Java: ${JAVA_HOME}"
        return 0
    fi
    error "未找到 Java，请安装 Java 17+"
    return 1
}

get_pid() {
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return 0
        fi
    fi
    return 1
}

do_start() {
    local pid=$(get_pid)
    if [[ -n "$pid" ]]; then
        warn "RPW 已在运行中 (PID: ${pid})"
        return 1
    fi

    if [[ ! -f "$JAR_PATH" ]]; then
        error "JAR 文件不存在: ${JAR_PATH}"
        return 1
    fi

    detect_java || return 1

    # 创建日志目录
    mkdir -p "$LOG_DIR"

    info "启动 RPW 后端..."
    nohup "${JAVA_HOME}/bin/java" \
        ${JVM_OPTS} \
        ${SPRING_OPTS} \
        -jar "$JAR_PATH" \
        > "$LOG_FILE" 2>&1 &

    local new_pid=$!
    echo "$new_pid" > "$PID_FILE"

    # 等待检查启动状态
    sleep 2
    if kill -0 "$new_pid" 2>/dev/null; then
        info "RPW 启动成功 (PID: ${new_pid})"
        info "日志文件: ${LOG_FILE}"
        return 0
    else
        error "RPW 启动失败，请查看日志: ${LOG_FILE}"
        rm -f "$PID_FILE"
        return 1
    fi
}

do_stop() {
    local pid=$(get_pid)
    if [[ -z "$pid" ]]; then
        warn "RPW 未在运行"
        rm -f "$PID_FILE"
        return 0
    fi

    info "停止 RPW (PID: ${pid})..."

    # 先发送 SIGTERM
    kill "$pid" 2>/dev/null

    # 等待最多 10 秒
    local count=0
    while kill -0 "$pid" 2>/dev/null && [[ $count -lt 10 ]]; do
        sleep 1
        count=$((count + 1))
    done

    if kill -0 "$pid" 2>/dev/null; then
        warn "SIGTERM 超时，发送 SIGKILL..."
        kill -9 "$pid" 2>/dev/null
    fi

    rm -f "$PID_FILE"
    info "RPW 已停止"
    return 0
}

do_restart() {
    do_stop
    sleep 1
    do_start
}

do_status() {
    local pid=$(get_pid)
    if [[ -n "$pid" ]]; then
        info "RPW 正在运行 (PID: ${pid})"
        if command -v ps &> /dev/null; then
            ps -p "$pid" -o pid,ppid,user,%cpu,%mem,etime,command --no-headers 2>/dev/null || \
            ps -p "$pid" 2>/dev/null
        fi
    else
        warn "RPW 未在运行"
    fi
}

# ========== 主逻辑 ==========
case "${1:-}" in
    start)
        do_start
        ;;
    stop)
        do_stop
        ;;
    restart)
        do_restart
        ;;
    status)
        do_status
        ;;
    *)
        echo "Usage: $0 [start|stop|restart|status]"
        exit 1
        ;;
esac
