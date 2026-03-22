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
