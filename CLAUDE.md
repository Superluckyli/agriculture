[根目录](../CLAUDE.md) > **agri-system (后端)**

# 后端模块 - Spring Boot REST API

## 模块职责

提供智慧农业管理系统的全部 RESTful API，包括用户认证(JWT)、系统管理、任务派单流转(乐观锁并发控制)、作物品种/批次管理、IoT 传感器数据采集与预警自动触发任务、物资仓储进出(事务性库存同步)、统计报表聚合等后端服务。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.10 | Web 框架 |
| Java | 17 | 编程语言 |
| MyBatis-Plus | 3.5.5 | ORM / 单表 CRUD + 自定义 @Update SQL |
| MySQL | 8.x | 关系型数据库 |
| Hutool | 5.8.25 | 工具库 (JWT/BCrypt/通用) |
| Lombok | - | POJO 简化 |
| Spring Boot Actuator | - | 运维监控端点 |

## 入口与启动

- **主类**: `lizhuoer.agri.agri_system.AgriSystemApplication`
- **配置**: `src/main/resources/application.yml`
- **端口**: `8080`
- **数据库初始化**: 启动时自动执行 `schema.sql`(建表) + `data.sql`(种子数据)，`spring.sql.init.mode=always`
- **定时任务**: `@EnableScheduling` 由 `SensorDataSimulator` 触发

## 包结构

```
src/main/java/lizhuoer/agri/agri_system/
  AgriSystemApplication.java          # Spring Boot 启动类
  common/                             # 通用层
    config/
      MyBatisPlusConfig.java          # MyBatis-Plus 分页插件配置
      WebMvcConfig.java               # MVC 拦截器注册 (JWT)
    domain/
      R.java                          # 统一响应体 R<T> {code, msg, data}
    exception/
      GlobalExceptionHandler.java     # 全局异常处理 (@RestControllerAdvice)
    security/
      JwtTokenUtil.java               # JWT 令牌生成与验证 (Hutool JWT, 密钥 AgriSystemKey_123456)
      JwtAuthInterceptor.java         # JWT 认证拦截器 (HandlerInterceptor)
      LoginUser.java                  # 当前登录用户上下文对象 (含 hasRole/hasAnyRole)
      LoginUserContext.java           # ThreadLocal 登录用户存取
  module/
    system/                           # 系统管理模块
      domain/
        SysUser.java                  # 用户实体 (含 roleNames @TableField transient)
        SysRole.java                  # 角色实体
        SysMenu.java                  # 菜单实体
        SysUserRole.java              # 用户角色关联
        dto/LoginBody.java            # 登录请求体
      mapper/
        SysUserMapper.java            # 含自定义 SQL (角色名查询)
        SysRoleMapper.java
        SysMenuMapper.java
        SysUserRoleMapper.java
      service/
        ISysUserService.java          # 含 getRoleKeys(userId)
        ISysRoleService.java
        ISysMenuService.java
        impl/
          SysUserServiceImpl.java     # 登录/注册/角色分配/角色 key 查询
          SysRoleServiceImpl.java
          SysMenuServiceImpl.java
      controller/
        AuthController.java           # 登录/注册 (/login, /register)
        SysUserController.java        # 用户 CRUD + 角色分配
        SysRoleController.java        # 角色 CRUD
        SysMenuController.java        # 菜单 CRUD
    task/                             # 任务管理模块 (核心)
      domain/
        AgriTask.java                 # 任务实体 (含 version 乐观锁字段, 5 个 @TableField(exist=false) 虚拟字段)
        TaskExecutionLog.java         # 执行日志
        TaskFlowLog.java              # 流转日志 (assign/accept/reject + traceId)
        enums/TaskStatus.java         # 任务状态枚举 (PENDING_ASSIGN=0..REJECTED=5)
        dto/
          TaskAssignDTO.java          # 派单请求
          TaskAcceptDTO.java          # 接单请求
          TaskRejectDTO.java          # 拒单请求 (含 reason)
          TaskUpdateDTO.java          # 基础信息更新
      mapper/
        AgriTaskMapper.java           # 含 3 个自定义 @Update (assignTask/acceptTask/rejectTask, 乐观锁 CAS)
        TaskExecutionLogMapper.java
        TaskFlowLogMapper.java
      service/
        IAgriTaskService.java         # 任务服务接口 (含派单/接单/拒单/自动创建/用户名填充)
        ITaskExecutionLogService.java
        impl/
          AgriTaskServiceImpl.java    # 任务流转核心逻辑 (277 行)
          TaskExecutionLogServiceImpl.java
      controller/
        AgriTaskController.java       # 任务 REST API
        TaskExecutionLogController.java
    crop/                             # 作物管理模块
      domain/
        BaseCropVariety.java          # 品种实体
        CropBatch.java                # 种植批次
        GrowthStageLog.java           # 生长日志
      mapper/
        BaseCropVarietyMapper.java
        CropBatchMapper.java
        GrowthStageLogMapper.java
      service/
        IBaseCropVarietyService.java
        ICropBatchService.java
        IGrowthStageLogService.java
        impl/                         # 均为空实现 (MyBatis-Plus ServiceImpl 默认 CRUD)
          BaseCropVarietyServiceImpl.java
          CropBatchServiceImpl.java
          GrowthStageLogServiceImpl.java
      controller/
        BaseCropVarietyController.java
        CropBatchController.java
        GrowthStageLogController.java
    iot/                              # IoT 监测模块
      domain/
        IotSensorData.java            # 传感器数据
        AgriTaskRule.java             # 自动预警规则
      mapper/
        IotSensorDataMapper.java
        AgriTaskRuleMapper.java
      service/
        IIotSensorDataService.java
        IAgriTaskRuleService.java     # 含 checkAndTriggerTask(data)
        impl/
          IotSensorDataServiceImpl.java  # saveDataAndCheckAlert: @Transactional 入库 + 触发预警
          AgriTaskRuleServiceImpl.java   # checkAndTriggerTask: 匹配规则 -> 创建待分配任务
      controller/
        IotSensorDataController.java
        AgriTaskRuleController.java
      job/
        SensorDataSimulator.java      # @Scheduled 每 10 分钟模拟 3 地块 x 2 传感器 (TEMP/HUMIDITY)
    material/                         # 物资管理模块
      domain/
        MaterialInfo.java             # 物资信息
        MaterialInoutLog.java         # 出入库记录
      mapper/
        MaterialInfoMapper.java
        MaterialInoutLogMapper.java
      service/
        IMaterialInfoService.java
        IMaterialInoutLogService.java # 含 executeInout(log)
        impl/
          MaterialInfoServiceImpl.java     # 空实现
          MaterialInoutLogServiceImpl.java # executeInout: @Transactional 校验库存 -> 更新余量 -> 保存流水
      controller/
        MaterialInfoController.java
        MaterialInoutLogController.java
    report/                           # 报表模块
      domain/
        ChartDataVO.java              # 图表数据 VO (xAxis + series)
      mapper/
        ReportMapper.java             # 3 个 @Select 聚合查询 (作物分布/任务趋势/传感器均值)
      service/
        IReportService.java
        impl/
          ReportServiceImpl.java      # getDashboardData: 聚合 3 个查询为 Map
      controller/
        ReportController.java
```

## 对外接口 (REST API 清单)

### 认证 (AuthController, /)
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/login` | 用户登录，返回 JWT token + 用户信息 + 角色列表 | 无 |
| POST | `/register` | 用户注册，默认 WORKER 角色，BCrypt 加密密码 | 无 |

### 系统管理 (SysUserController, /system/user)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/user/list` | 分页查询用户 (含角色名填充) |
| GET | `/system/user/{userId}` | 查询单个用户 |
| POST | `/system/user` | 新增用户 |
| PUT | `/system/user` | 更新用户 |
| DELETE | `/system/user/{userIds}` | 批量删除用户 |
| GET | `/system/user/{userId}/roles` | 获取用户角色 ID 列表 |
| PUT | `/system/user/{userId}/roles` | 分配用户角色 (全量替换) |

### 角色管理 (SysRoleController, /system/role)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/role/list` | 分页查询角色 |
| GET | `/system/role/all` | 查询全部角色 (不分页) |
| POST | `/system/role` | 新增角色 |
| PUT | `/system/role` | 更新角色 |
| DELETE | `/system/role/{roleIds}` | 批量删除角色 |

### 菜单管理 (SysMenuController, /system/menu)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/menu/list` | 查询菜单列表 (不分页) |
| POST | `/system/menu` | 新增菜单 |
| PUT | `/system/menu` | 更新菜单 |
| DELETE | `/system/menu/{menuId}` | 删除菜单 |

### 任务管理 (AgriTaskController, /task)
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/task/list` | 分页查询任务 (taskName/status/executorId/assigneeId 过滤，含用户名填充) | 可选 |
| POST | `/task` | 新增任务 (初始 status=0 待分配) | 可选 |
| PUT | `/task` | 更新任务基础信息 (TaskUpdateDTO) | 可选 |
| PUT | `/task/assign` | 派单 (ADMIN/FARM_OWNER -> FARMER/WORKER)，乐观锁 CAS | 必须 |
| POST | `/task/accept` | 接单 (FARMER 确认) | 必须 |
| POST | `/task/reject` | 拒单 (FARMER 拒绝，回退到待分配) | 必须 |
| DELETE | `/task/{ids}` | 批量删除任务 | 可选 |

### 任务执行日志 (TaskExecutionLogController, /task/log)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/task/log/list` | 分页查询执行日志 |
| POST | `/task/log` | 新增执行日志 |

### 作物管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/crop/variety/list` | 分页查询品种 |
| GET | `/crop/variety/all` | 全量查询品种 |
| POST | `/crop/variety` | 新增品种 |
| PUT | `/crop/variety` | 更新品种 |
| DELETE | `/crop/variety/{ids}` | 删除品种 |
| GET | `/crop/batch/list` | 分页查询批次 |
| POST | `/crop/batch` | 新增批次 |
| PUT | `/crop/batch` | 更新批次 |
| DELETE | `/crop/batch/{ids}` | 删除批次 |
| GET | `/crop/growth-log/list/{batchId}` | 按批次查询生长日志 |
| POST | `/crop/growth-log` | 新增生长日志 |

### IoT 监测
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/iot/data/list` | 分页查询传感器数据 |
| GET | `/iot/rule/list` | 分页查询预警规则 |
| POST | `/iot/rule` | 新增规则 |
| PUT | `/iot/rule` | 更新规则 |
| DELETE | `/iot/rule/{id}` | 删除规则 |

### 物资管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/material/info/list` | 分页查询物资 |
| POST | `/material/info` | 新增物资 |
| PUT | `/material/info` | 更新物资 |
| DELETE | `/material/info/{ids}` | 删除物资 |
| GET | `/material/log/list` | 分页查询出入库日志 |
| POST | `/material/log/execute` | 执行出入库 (事务: 校验库存 -> 更新余量 -> 保存流水) |

### 报表 (ReportController, /report)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/report/dashboard` | Dashboard 聚合 (cropDistribution 饼图 + taskTrend 折线 + envMonitor 传感器均值) |

## 关键依赖与配置

- **数据库**: MySQL `agri_system`，连接配置在 `application.yml`
- **ORM**: MyBatis-Plus 自动驼峰映射 (`map-underscore-to-camel-case: true`)，ID 策略为自增，SQL 日志输出到 stdout
- **JWT**: Hutool JWT，密钥硬编码 `AgriSystemKey_123456`，无过期时间
- **密码**: BCrypt 加密 (`cn.hutool.crypto.digest.BCrypt`)
- **鉴权拦截**: `JwtAuthInterceptor` 拦截所有路径，排除 `/login`, `/register`, `/error`, `/actuator/**`
- **强制鉴权路径**: `/task/assign`, `/task/accept`, `/task/reject`, `/system/**` (无 token 返回 401)
- **其他路径**: token 可选，有则解析登录上下文
- **定时任务**: `SensorDataSimulator` 每 10 分钟 (`fixedRate=600000`) 模拟 3 地块的温度/湿度数据，20% 概率生成低湿度触发预警
- **SQL 初始化**: `spring.sql.init.mode=always`，`continue-on-error=true`，每次启动重建数据

## 核心业务逻辑

### 任务状态机

```
0(待分配) --[assign to FARMER]--> 1(待接单) --[accept]--> 2(执行中) --> 3(已完成)
                                              --[reject]--> 0(待分配, 清空指派信息)
0(待分配) --[assign to WORKER]--> 2(执行中, 跳过接单)
                                                            4(已逾期)
```

- 乐观锁: `WHERE task_id=? AND status=? AND version=?`，更新后 `version=version+1`
- 幂等性: assign/accept/reject 均做二次检查，若已达目标状态则静默返回
- 流转日志: 每次状态变更写入 `task_flow_log` (含 traceId 用于链路追踪)
- 派单角色区分: FARMER 需要接单确认(1->2)，WORKER 直接进入执行中(0->2)

### IoT 预警触发链路

```
SensorDataSimulator (每10分钟)
  -> IotSensorDataServiceImpl.saveDataAndCheckAlert(@Transactional)
    -> 1. save(data)  入库
    -> 2. AgriTaskRuleServiceImpl.checkAndTriggerTask(data)
      -> 查询该传感器类型下启用的规则
      -> 检查 value < minVal 或 value > maxVal
      -> 触发: createAlertTask -> AgriTaskServiceImpl.createAutoTask -> 创建 status=0 任务
```

注意: 当前无防抖机制，每次超标都会创建新任务。

### 物资出入库

```
MaterialInoutLogServiceImpl.executeInout(@Transactional)
  -> 1. 获取 MaterialInfo (不存在则抛异常)
  -> 2. 计算新库存 (type=1 入库加, type=2 出库减)
  -> 3. 出库校验: 库存不足则抛异常
  -> 4. 更新 material_info.stock_quantity
  -> 5. 保存 material_inout_log 流水
```

### 报表聚合

`ReportServiceImpl.getDashboardData()` 返回 3 个数据集：
1. `cropDistribution`: 按品种统计活跃批次数 (LEFT JOIN crop_batch + base_crop_variety)
2. `taskTrend`: 近 7 天每日任务创建数 (ChartDataVO: xAxis=日期, series=计数)
3. `envMonitor`: 最近 1 小时各传感器类型平均值

## 数据模型 (数据库表)

共 15 张表，定义在 `src/main/resources/schema.sql`：

### 系统模块
| 表名 | 说明 | 主键 | 关键字段 |
|------|------|------|----------|
| `sys_user` | 用户 | `user_id` | username(UNIQUE), password(BCrypt), status(1正常/0停用) |
| `sys_role` | 角色 | `role_id` | role_key(UNIQUE) |
| `sys_menu` | 菜单 | `menu_id` | parent_id, path, perms |
| `sys_user_role` | 用户-角色关联 | `(user_id, role_id)` | FK -> sys_user ON DELETE CASCADE, FK -> sys_role ON DELETE RESTRICT |
| `sys_role_menu` | 角色-菜单关联 | `(role_id, menu_id)` | - |

### 作物模块
| 表名 | 说明 | 主键 | 关键字段 |
|------|------|------|----------|
| `base_crop_variety` | 作物品种 | `variety_id` | ideal_humidity/temp_min/max |
| `crop_batch` | 种植批次 | `batch_id` | variety_id, plot_id, is_active |
| `growth_stage_log` | 生长日志 | `log_id` | batch_id, image_url, description(TEXT) |

### IoT 模块
| 表名 | 说明 | 主键 | 关键字段 |
|------|------|------|----------|
| `iot_sensor_data` | 传感器数据 | `data_id` | plot_id, sensor_type, value, INDEX(plot_id, create_time) |
| `agri_task_rule` | 自动预警规则 | `rule_id` | sensor_type, min_val, max_val, is_enable |

### 任务模块
| 表名 | 说明 | 主键 | 关键字段 |
|------|------|------|----------|
| `agri_task` | 农业任务 (核心) | `task_id` | status(0-5), executor_id(legacy), assignee_id, version(乐观锁) |
| `task_execution_log` | 执行日志 | `log_id` | task_id, photo_url, material_cost_json(JSON) |
| `task_flow_log` | 流转日志 | `log_id` | task_id, action, from/to_status, trace_id |

### 物资模块
| 表名 | 说明 | 主键 | 关键字段 |
|------|------|------|----------|
| `material_info` | 物资信息 | `material_id` | stock_quantity(DECIMAL), category |
| `material_inout_log` | 出入库记录 | `log_id` | material_id, type(1入/2出), related_task_id |

## 测试与质量

- 测试框架: JUnit 5 + Spring Boot Test
- 测试目录: `src/test/java/`
- 已有测试 (5 个类):
  - `AgriSystemApplicationTests.java` -- 启动上下文测试
  - `AgriTaskControllerAssignTest.java` -- 任务派单接口集成测试
  - `AgriTaskServiceImplAssignTaskTest.java` -- 派单 Service 逻辑单元测试
  - `AgriTaskServiceImplAcceptRejectTaskTest.java` -- 接单/拒单逻辑单元测试
  - `AgriTaskServiceImplListUserNameTest.java` -- 用户名填充单元测试
- 运行: `./mvnw test`
- 覆盖重点: 任务模块的状态流转、乐观锁、角色校验、幂等性

## 常见问题 (FAQ)

1. **Q: 为什么每次启动数据都被重置？**
   A: `application.yml` 中 `spring.sql.init.mode=always`，`data.sql` 会先 DELETE 再 INSERT。生产环境需改为 `never`。

2. **Q: JWT 为什么没有过期时间？**
   A: 当前 `JwtTokenUtil` 未设置过期，属于 V1 简化实现，生产需添加 `setExpiresAt()`。

3. **Q: 密码加密方式？**
   A: 使用 `cn.hutool.crypto.digest.BCrypt`，注册时 `BCrypt.hashpw()`，登录时 `BCrypt.checkpw()`。

4. **Q: 任务派单的并发控制？**
   A: `AgriTaskMapper` 的 assign/accept/reject 方法使用乐观锁 (`WHERE version=? ... SET version=version+1`)，更新失败后二次读取检查是否已达目标状态 (幂等性)。

5. **Q: IoT 预警为什么没有防抖？**
   A: `AgriTaskRuleServiceImpl.createAlertTask()` 有 TODO 注释说明需要防抖机制，当前每次超标都创建任务，属于演示简化。

6. **Q: executor_id 和 assignee_id 的区别？**
   A: `executor_id` 是遗留字段 (兼容旧查询)，`assignee_id` 是实际派单目标。assign 操作同时设置两者。

## 相关文件清单

| 类别 | 文件 |
|------|------|
| 入口 | `AgriSystemApplication.java` |
| 配置 | `src/main/resources/application.yml`, `pom.xml` |
| 数据库 | `src/main/resources/schema.sql`, `src/main/resources/data.sql` |
| 安全 | `common/security/JwtTokenUtil.java`, `common/security/JwtAuthInterceptor.java`, `common/security/LoginUser.java`, `common/security/LoginUserContext.java` |
| 全局 | `common/domain/R.java`, `common/exception/GlobalExceptionHandler.java`, `common/config/MyBatisPlusConfig.java`, `common/config/WebMvcConfig.java` |
| 核心业务 | `module/task/service/impl/AgriTaskServiceImpl.java`, `module/task/mapper/AgriTaskMapper.java` |
| IoT 链路 | `module/iot/job/SensorDataSimulator.java`, `module/iot/service/impl/IotSensorDataServiceImpl.java`, `module/iot/service/impl/AgriTaskRuleServiceImpl.java` |
| 物资 | `module/material/service/impl/MaterialInoutLogServiceImpl.java` |
| 报表 | `module/report/mapper/ReportMapper.java`, `module/report/service/impl/ReportServiceImpl.java` |
| 测试 | `src/test/java/` (5 个测试类) |
| 构建 | `pom.xml`, `.mvn/wrapper/maven-wrapper.properties` |

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-12T03:09:04+08:00 | 增量更新 | 深度补扫全部 Service 实现层；补全 AgriTaskServiceImpl 完整流转逻辑 (乐观锁/幂等/角色区分)、AgriTaskRuleServiceImpl 预警触发链路、MaterialInoutLogServiceImpl 出入库事务、ReportServiceImpl 聚合查询、ReportMapper 3 条 SQL、SensorDataSimulator 模拟策略、AgriTaskMapper 3 个 CAS 更新语句；新增核心业务逻辑章节 |
| 2026-03-12T02:16:28 | 初始生成 | 由架构师扫描工具首次生成 |
