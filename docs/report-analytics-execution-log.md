# 统计报表执行记录

## Step 1 - 启动前后端并完成基础联调启动（2026-03-23）

### 后端
- 目录：`agriculture`
- 启动命令：`mvn -Dmaven.repo.local=/tmp/report-analytics-m2 -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.arguments='--spring.config.additional-location=file:src/test/resources/' spring-boot:run`
- Java：`/home/lze/projects/agri-system/.tooling/jdk17/usr/lib/jvm/java-17-openjdk-amd64`
- 结果：Spring Boot 成功启动在 `http://127.0.0.1:8080`

### 联调说明
- 因本机 MySQL 未启动，当前使用 test classpath + H2 数据集完成联调启动
- 后续 Step 2 将直接基于当前运行中的后端，检查 analytics 接口真实返回结构与数据

## Step 2 - 检查统计报表页实际接口返回（2026-03-23）

### 后端联调结果
- 登录接口：`POST /login` 返回 200，拿到 token
- `GET /report/analytics/overview?startDate=2026-03-01&endDate=2026-03-31` 返回 200
  - 示例：`taskCompletionRate = 22.2`
  - 示例：`activeBatchCount = 3`
- 前端代理访问 overview 也返回 200

### 发现的问题
- `task / production / cost` 三个 analytics 接口在 **本地 H2 联调环境** 下返回 500
- 当前定位结果：运行时 SQL 中的 `DATE_FORMAT(...)` 在 H2 下不可用
- 已补充 H2 兼容初始化尝试（`src/test/resources/application.yml` + `src/test/resources/h2-functions.sql`），但仍未完成运行时兼容

### 结果结论
- overview 接口和前端代理链路已验证成功
- 本地 H2 启动链路可用，但除 overview 外的 analytics 接口仍存在运行时兼容问题
- 后续若继续做人工联调，需要优先处理 H2 / SQL 方言兼容，或切换到真实 MySQL 环境
