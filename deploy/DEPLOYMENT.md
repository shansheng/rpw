# 资源计划预警系统 (RPW) 部署指南

## 1. 项目概述

**资源计划预警系统**（Resource Plan Warning System，简称 RPW）是一套面向企业资源计划管理的综合平台，提供资源计划管理、预警规则配置、劳动力计划变更审批以及工作流管理等功能。

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.1.6 |
| ORM 框架 | MyBatis-Plus 3.5.7 |
| 工作流引擎 | Flowable 7.0.0 |
| 前端框架 | Vue 3 + TypeScript |
| 构建工具 | Vite |
| UI 框架 | Ant Design Vue 4.x |
| CSS 框架 | Tailwind CSS |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ |
| API 文档 | Knife4j |

---

## 2. 环境要求

### 2.1 后端环境

| 软件 | 版本要求 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 17+ | OpenJDK 17 | 必须为 JDK 17 及以上版本 |
| Maven | 3.8+ | 3.9.x | 用于编译打包 |
| MySQL | 8.0+ | 8.0.x | 字符集需设置为 utf8mb4 |
| Redis | 6.0+ | 7.x | 用于缓存和会话管理 |

### 2.2 前端环境

| 软件 | 版本要求 | 推荐版本 |
|------|----------|----------|
| Node.js | 18+ | 18 LTS |
| npm | 9+ | 随 Node.js 附带 |
| pnpm | 8+（可选） | 8.x |

### 2.3 运行端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 API | 8080 | Spring Boot 内嵌 Tomcat |
| 前端（开发） | 3002 | Vite 开发服务器 |
| 前端（生产） | 80/443 | 通过 Nginx 代理 |
| MySQL | 3306 | 默认端口 |
| Redis | 6379 | 默认端口 |

### 2.4 系统资源建议

| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 50 GB SSD |

---

## 3. 数据库初始化

### 3.1 创建数据库

```sql
CREATE DATABASE rpw_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3.2 执行 SQL 脚本

按以下顺序依次执行 `sql/` 目录下的脚本：

| 顺序 | 脚本文件 | 说明 |
|------|----------|------|
| 1 | `sql/01_schema.sql` | 基础表结构，包含所有核心业务表 |
| 2 | `sql/fix-ddl-columns.sql` | 字段修复，修正表结构中的字段定义 |
| 3 | `sql/update-resource-plan-material.sql` | 物资计划更新，增加物资相关字段 |
| 4 | `sql/phase6-update.sql` | 阶段 6 更新，劳动力变更审批相关表 |
| 5 | `sql/phase7-report.sql` | 阶段 7 报表，预警统计报表相关表 |

执行示例：

```bash
mysql -u root -p rpw_db < sql/01_schema.sql
mysql -u root -p rpw_db < sql/fix-ddl-columns.sql
mysql -u root -p rpw_db < sql/update-resource-plan-material.sql
mysql -u root -p rpw_db < sql/phase6-update.sql
mysql -u root -p rpw_db < sql/phase7-report.sql
```

> **注意**：Flowable 工作流引擎的表（`ACT_*`、`FLW_*` 前缀）由 Spring Boot 自动创建（`database-schema-update: true`），无需手动执行。

### 3.3 数据库字符集验证

执行以下命令确认数据库字符集配置正确：

```sql
-- 查看数据库字符集
SHOW CREATE DATABASE rpw_db;

-- 确认关键表字符集
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'rpw_db'
LIMIT 10;
```

确保所有表的 `TABLE_COLLATION` 为 `utf8mb4_unicode_ci` 或 `utf8mb4_general_ci`。

---

## 4. 后端部署

### 4.1 编译打包

```bash
cd backend
mvn clean package -DskipTests
```

产出文件：`target/rpw-1.0.0.jar`（约 90MB）

如需执行测试：

```bash
mvn clean package
```

### 4.2 配置说明

后端配置文件位于 `backend/src/main/resources/` 目录：

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置文件，包含所有核心配置 |
| `application-dev.yml` | 开发环境配置覆盖，开启 SQL 日志和 DEBUG 级别 |

#### 关键配置项

| 配置项 | 配置文件 | 说明 | 默认值 |
|--------|----------|------|--------|
| `server.port` | application.yml | 服务端口 | `8080` |
| `spring.datasource.url` | application.yml | 数据库连接 URL | `jdbc:mysql://localhost:3306/rpw_db` |
| `spring.datasource.username` | application.yml | 数据库用户名 | `root` |
| `spring.datasource.password` | application.yml | 数据库密码 | 环境变量 `MYSQL_PASSWORD`，默认 `FundTracker2024!` |
| `spring.datasource.hikari.maximum-pool-size` | application.yml | 数据库连接池大小 | `20` |
| `spring.datasource.hikari.minimum-idle` | application.yml | 最小空闲连接数 | `5` |
| `spring.data.redis.host` | application.yml | Redis 地址 | `localhost` |
| `spring.data.redis.port` | application.yml | Redis 端口 | `6379` |
| `jwt.secret` | application.yml | JWT 密钥 | 环境变量 `JWT_SECRET` |
| `jwt.expiration` | application.yml | Token 过期时间（秒） | `86400`（24 小时） |
| `flowable.database-schema-update` | application.yml | Flowable 表自动更新 | `true` |
| `flowable.history-level` | application.yml | 流程历史记录级别 | `full` |
| `knife4j.enable` | application.yml | API 文档开关 | `true` |

### 4.3 启动服务

#### 开发模式

```bash
cd backend
mvn spring-boot:run
```

#### 生产模式

```bash
java -jar rpw-1.0.0.jar --server.port=8080
```

#### 环境变量覆盖（生产推荐）

生产环境务必通过环境变量覆盖敏感配置：

```bash
export MYSQL_PASSWORD=your_secure_password
export JWT_SECRET=your_secure_jwt_secret_at_least_64_chars_long
java -jar rpw-1.0.0.jar
```

也可以通过 Spring Profile 指定生产配置：

```bash
java -jar rpw-1.0.0.jar --spring.profiles.active=prod
```

#### 后台运行

```bash
nohup java -jar rpw-1.0.0.jar \
  --server.port=8080 \
  > /opt/rpw/app/logs/rpw.log 2>&1 &
echo $! > /opt/rpw/app/rpw.pid
```

### 4.4 健康检查

服务启动后，验证是否正常运行：

```bash
# 检查服务端口
curl -s http://localhost:8080/doc.html -o /dev/null -w "%{http_code}"

# 预期返回 200
```

---

## 5. 前端部署

### 5.1 开发模式

```bash
cd frontend2
npm install
npm run dev
```

启动后访问 http://localhost:3002，Vite 开发服务器已配置 `/api` 代理到后端 `http://localhost:8080`。

### 5.2 生产构建

```bash
cd frontend2
npm install
npm run build
```

产出目录：`dist/`，包含静态 HTML、JS、CSS 等文件。

### 5.3 Nginx 配置

以下是生产环境 Nginx 配置示例：

```nginx
server {
    listen       80;
    server_name  rpw.example.com;

    # 前端静态文件
    root /opt/rpw/web/dist;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_min_length 1024;
    gzip_types
        text/plain
        text/css
        text/javascript
        application/javascript
        application/json
        application/xml
        image/svg+xml;
    gzip_vary on;
    gzip_comp_level 6;

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
        proxy_send_timeout 30s;
    }

    # Knife4j API 文档代理（可选，生产环境建议关闭）
    location /doc.html {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    location /swagger-resources {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    location /webjars/ {
        proxy_pass http://127.0.0.1:8080;
    }

    # SPA History 模式 — 所有非文件请求回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存策略
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }

    # 请求体大小限制（支持文件上传）
    client_max_body_size 50m;
}
```

---

## 6. 生产部署方案

### 6.1 单机部署方案（推荐）

后端 JAR + 前端 dist 部署在同一台服务器，Nginx 同时托管前端静态文件和反向代理后端 API。

#### 目录结构

```
/opt/rpw/
├── app/
│   ├── rpw-1.0.0.jar          # 后端 JAR
│   ├── application-prod.yml    # 生产配置覆盖（可选）
│   └── logs/                   # 日志目录
├── web/
│   └── dist/                   # 前端构建产物
├── sql/                        # 数据库脚本
│   ├── 01_schema.sql
│   ├── fix-ddl-columns.sql
│   ├── update-resource-plan-material.sql
│   ├── phase6-update.sql
│   └── phase7-report.sql
├── deploy.sh                   # 启动脚本
├── stop.sh                     # 停止脚本
└── nginx.conf                  # Nginx 配置
```

### 6.2 启动脚本 deploy.sh

```bash
#!/bin/bash
APP_NAME="rpw"
APP_HOME="/opt/rpw/app"
JAR_FILE="${APP_HOME}/rpw-1.0.0.jar"
PID_FILE="${APP_HOME}/rpw.pid"
LOG_FILE="${APP_HOME}/logs/rpw.log"

# 环境变量（生产环境请修改）
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-FundTracker2024!}"
export JWT_SECRET="${JWT_SECRET:-change_me_in_production}"

# 检查是否已运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "${APP_NAME} is already running (PID: ${PID})"
        exit 1
    fi
fi

# 创建日志目录
mkdir -p "${APP_HOME}/logs"

# 启动服务
echo "Starting ${APP_NAME}..."
nohup java -jar "$JAR_FILE" \
    --server.port=8080 \
    > "$LOG_FILE" 2>&1 &

echo $! > "$PID_FILE"
echo "${APP_NAME} started (PID: $(cat "$PID_FILE"))"
echo "Log: tail -f ${LOG_FILE}"
```

### 6.3 停止脚本 stop.sh

```bash
#!/bin/bash
APP_NAME="rpw"
APP_HOME="/opt/rpw/app"
PID_FILE="${APP_HOME}/rpw.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "${APP_NAME} is not running (PID file not found)"
    exit 0
fi

PID=$(cat "$PID_FILE")

if kill -0 "$PID" 2>/dev/null; then
    echo "Stopping ${APP_NAME} (PID: ${PID})..."
    kill "$PID"

    # 等待进程退出
    for i in $(seq 1 30); do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "${APP_NAME} stopped"
            rm -f "$PID_FILE"
            exit 0
        fi
        sleep 1
    done

    # 强制终止
    echo "Force killing ${APP_NAME}..."
    kill -9 "$PID"
    rm -f "$PID_FILE"
else
    echo "${APP_NAME} is not running"
    rm -f "$PID_FILE"
fi
```

### 6.4 部署步骤

```bash
# 1. 创建部署目录
mkdir -p /opt/rpw/{app/logs,web/dist,sql}

# 2. 复制后端 JAR
cp backend/target/rpw-1.0.0.jar /opt/rpw/app/

# 3. 复制前端构建产物
cp -r frontend2/dist/* /opt/rpw/web/dist/

# 4. 复制数据库脚本
cp sql/*.sql /opt/rpw/sql/

# 5. 复制启动/停止脚本
cp deploy/deploy.sh /opt/rpw/
cp deploy/stop.sh /opt/rpw/
chmod +x /opt/rpw/deploy.sh /opt/rpw/stop.sh

# 6. 配置 Nginx
sudo cp deploy/nginx.conf /etc/nginx/conf.d/rpw.conf
sudo nginx -t && sudo nginx -s reload

# 7. 设置环境变量
export MYSQL_PASSWORD="your_secure_password"
export JWT_SECRET="your_secure_jwt_secret"

# 8. 启动服务
/opt/rpw/deploy.sh
```

---

## 7. API 文档

### 7.1 在线文档

Knife4j 在线 API 文档地址：http://localhost:8080/doc.html

> 生产环境建议关闭 Knife4j，在 `application-prod.yml` 中设置 `knife4j.enable: false`。

### 7.2 API 模块列表

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/v1/auth/**` | 登录、登出、Token 刷新 |
| 用户管理 | `/api/v1/users/**` | 用户 CRUD、角色分配 |
| 组织架构 | `/api/v1/organizations/**` | 组织树管理 |
| 资源计划 | `/api/v1/resource-plan/**` | 资源计划 CRUD、物资计划 |
| 预警规则 | `/api/v1/warning-rules/**` | 预警规则配置和管理 |
| 预警记录 | `/api/v1/warning-records/**` | 预警触发记录查询 |
| 预警统计 | `/api/v1/warning-statistics/**` | 预警数据统计和图表 |
| 流程管理 | `/api/v1/flowable/**` | Flowable 工作流管理 |
| 定时任务调试 | `/api/v1/scheduler/**` | 预警定时任务手动触发 |
| 导出 | `/api/v1/export/**` | 数据导出（Excel 等） |

---

## 8. 默认账号

| 项目 | 值 |
|------|----|
| 用户名 | `admin` |
| 密码 | `admin123` |

> **安全警告**：生产环境请务必在首次登录后立即修改默认密码！

---

## 9. 常见问题

### 9.1 端口被占用

**现象**：启动时报错 `Port 8080 is already in use`

**解决方案**：

```bash
# 查找占用端口的进程
lsof -i :8080

# 终止占用进程
kill -9 <PID>

# 或指定其他端口启动
java -jar rpw-1.0.0.jar --server.port=8081
```

### 9.2 数据库连接失败

**现象**：启动日志报错 `Communications link failure` 或 `Access denied`

**排查步骤**：

1. 确认 MySQL 服务正在运行：
   ```bash
   systemctl status mysql
   # 或
   brew services list | grep mysql
   ```

2. 确认数据库 `rpw_db` 存在：
   ```bash
   mysql -u root -p -e "SHOW DATABASES LIKE 'rpw_db';"
   ```

3. 确认用户名和密码正确：
   ```bash
   mysql -u root -p -e "SELECT 1;"
   ```

4. 确认 `application.yml` 中数据库连接配置正确，特别是 `useSSL=false` 和 `serverTimezone=Asia/Shanghai`

5. 检查环境变量 `MYSQL_PASSWORD` 是否已正确设置

### 9.3 Redis 连接失败

**现象**：启动日志报错 `Unable to connect to Redis` 或 `RedisConnectionException`

**排查步骤**：

1. 确认 Redis 服务正在运行：
   ```bash
   redis-cli ping
   # 预期返回 PONG
   ```

2. 确认 Redis 端口和地址配置正确（默认 `localhost:6379`）

3. 如 Redis 设置了密码，在 `application.yml` 中添加：
   ```yaml
   spring:
     data:
       redis:
         password: your_redis_password
   ```

### 9.4 Flowable 流程定义未自动部署

**现象**：工作流相关接口报错，流程定义不存在

**排查步骤**：

1. 确认 `application.yml` 中 Flowable 自动部署已开启：
   ```yaml
   flowable:
     database-schema-update: true
     process:
       deployment-enabled: true
   ```

2. 检查流程定义文件是否在正确路径：`backend/src/main/resources/processes/`

3. 查看 `ACT_RE_DEPLOYMENT` 表确认流程是否已部署：
   ```sql
   SELECT * FROM ACT_RE_DEPLOYMENT ORDER BY DEPLOY_TIME_ DESC LIMIT 10;
   ```

4. 如流程定义文件缺失，重新启动应用即可自动部署

### 9.5 前端白屏 / CORS 问题

**现象**：浏览器控制台报跨域错误或页面白屏

**排查步骤**：

1. **开发模式**：确认 `vite.config.ts` 中代理配置正确：
   ```typescript
   server: {
     proxy: {
       '/api': {
         target: 'http://localhost:8080',
         changeOrigin: true
       }
     }
   }
   ```

2. **生产模式**：确认 Nginx 反向代理配置正确，`/api/` 路径已转发到后端

3. **SPA 路由**：确认 Nginx 配置了 `try_files $uri $uri/ /index.html;`，否则刷新页面会 404

4. **浏览器缓存**：清除浏览器缓存或使用无痕模式验证

5. **控制台错误**：打开浏览器开发者工具（F12），检查 Console 和 Network 标签页的具体错误信息

---

## 10. 日志查看

### 10.1 后端日志

**开发环境**：日志直接输出到控制台。

**生产环境**：

```bash
# 查看实时日志
tail -f /opt/rpw/app/logs/rpw.log

# 搜索错误日志
grep -i "error\|exception" /opt/rpw/app/logs/rpw.log

# 查看最近 100 行
tail -n 100 /opt/rpw/app/logs/rpw.log
```

如需配置日志轮转，可在 `application.yml` 中添加 logback 配置，或使用系统日志管理工具。

### 10.2 Nginx 日志

```bash
# 访问日志
tail -f /var/log/nginx/access.log

# 错误日志
tail -f /var/log/nginx/error.log

# 筛选特定接口的请求
grep "/api/v1/" /var/log/nginx/access.log
```

---

## 附录 A：环境变量速查

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `MYSQL_PASSWORD` | 数据库密码 | `MyStr0ngP@ss` |
| `JWT_SECRET` | JWT 签名密钥（至少 64 字符） | `base64-encoded-secret...` |
| `JAVA_OPTS` | JVM 参数 | `-Xms512m -Xmx2g` |

---

## 附录 B：配置文件参考

### application-prod.yml（生产环境配置模板）

```yaml
spring:
  datasource:
    password: ${MYSQL_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

# 关闭 API 文档
knife4j:
  enable: false

# 生产环境日志级别
logging:
  level:
    com.company.rpw: INFO
    org.flowable: WARN

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

启动时指定：

```bash
java -jar rpw-1.0.0.jar --spring.profiles.active=prod
```
