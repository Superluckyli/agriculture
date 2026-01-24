-- ==========================================
-- 1. 系统管理模块 (System Module)
-- ==========================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    real_name VARCHAR(50) COMMENT '真名',
    phone VARCHAR(20) COMMENT '联系方式',
    dept_name VARCHAR(50) COMMENT '所属部门',
    status INT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色权限字符 (admin, farmer...)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='角色表';

-- 菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path VARCHAR(200) COMMENT '路由地址',
    perms VARCHAR(100) COMMENT '权限标识',
    type INT COMMENT '类型 0目录 1菜单 2按钮',
    order_num INT DEFAULT 0 COMMENT '显示顺序'
) COMMENT='菜单权限表';

-- 用户角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关联表';

-- 角色菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) COMMENT='角色菜单关联表';


-- ==========================================
-- 2. 作物与地块模块 (Crop Module)
-- ==========================================

-- 作物品种库
CREATE TABLE IF NOT EXISTS base_crop_variety (
    variety_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '品种ID',
    crop_name VARCHAR(100) NOT NULL COMMENT '作物名称',
    growth_cycle_days INT COMMENT '生长周期(天)',
    ideal_humidity_min DECIMAL(5,2) COMMENT '适宜湿度下限',
    ideal_humidity_max DECIMAL(5,2) COMMENT '适宜湿度上限',
    ideal_temp_min DECIMAL(5,2) COMMENT '适宜温度下限',
    ideal_temp_max DECIMAL(5,2) COMMENT '适宜温度上限',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='作物品种库';

-- 种植批次 (核心表)
CREATE TABLE IF NOT EXISTS crop_batch (
    batch_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '批次ID',
    variety_id BIGINT NOT NULL COMMENT '品种ID',
    plot_id VARCHAR(50) NOT NULL COMMENT '地块ID (关联地块信息，此处简化为ID)',
    sowing_date DATE COMMENT '播种日期',
    expected_harvest_date DATE COMMENT '预计收获日期',
    current_stage VARCHAR(50) COMMENT '当前生长阶段 (播种期, 生长期, 成熟期)',
    is_active INT DEFAULT 1 COMMENT '是否活跃 1是 0否'
) COMMENT='种植批次表';

-- 生长阶段记录
CREATE TABLE IF NOT EXISTS growth_stage_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    stage_name VARCHAR(50) COMMENT '阶段名称',
    log_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    image_url VARCHAR(500) COMMENT '照片地址',
    description TEXT COMMENT '备注'
) COMMENT='生长阶段记录';


-- ==========================================
-- 3. 农情监测模块 (IoT Module)
-- ==========================================

-- 传感器数据 (时序数据)
CREATE TABLE IF NOT EXISTS iot_sensor_data (
    data_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plot_id VARCHAR(50) COMMENT '地块ID',
    sensor_type VARCHAR(20) COMMENT '类型: TEMP(温度), HUMIDITY(湿度), PH, LIGHT',
    value DECIMAL(10, 2) COMMENT '数值',
    unit VARCHAR(10) COMMENT '单位',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plot_time (plot_id, create_time)
) COMMENT='传感器数据表';

-- 智能预警规则
CREATE TABLE IF NOT EXISTS agri_task_rule (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(50),
    sensor_type VARCHAR(20) COMMENT '监测指标',
    min_val DECIMAL(10, 2) COMMENT '最小值阈值 (低于此值报警)',
    max_val DECIMAL(10, 2) COMMENT '最大值阈值 (高于此值报警)',
    auto_task_type VARCHAR(50) COMMENT '自动触发的任务类型 (e.g. 灌溉)',
    priority INT DEFAULT 1 COMMENT '生成任务的优先级',
    is_enable INT DEFAULT 1
) COMMENT='智能预警规则';


-- ==========================================
-- 4. 农事任务模块 (Task Module)
-- ==========================================

-- 农事任务
CREATE TABLE IF NOT EXISTS agri_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT COMMENT '关联种植批次',
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) COMMENT '任务类型: 播种/灌溉/施肥/除草/采摘',
    priority INT DEFAULT 0 COMMENT '优先级 2紧急 1重要 0普通',
    plan_time DATETIME COMMENT '计划执行时间',
    status INT DEFAULT 0 COMMENT '0待分配 1待执行 2进行中 3已完成 4已逾期',
    executor_id BIGINT COMMENT '执行人ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='农事任务表';

-- 任务执行记录
CREATE TABLE IF NOT EXISTS task_execution_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    actual_start_time DATETIME,
    actual_end_time DATETIME,
    status_snapshot INT COMMENT '执行后的状态',
    photo_url VARCHAR(500) COMMENT '执行凭证',
    material_cost_json JSON COMMENT '物资消耗JSON记录',
    problem_desc TEXT COMMENT '遇到的问题',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='任务执行记录';


-- ==========================================
-- 5. 物资与仓储模块 (Material Module)
-- ==========================================

-- 物资档案与库存
CREATE TABLE IF NOT EXISTS material_info (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) COMMENT '分类 (种子, 化肥, 农药, 工具)',
    price DECIMAL(10,2) COMMENT '单价',
    stock_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '当前库存',
    unit VARCHAR(20) COMMENT '单位',
    update_time DATETIME
) COMMENT='物资档案与库存';

-- 物资出入库流水
CREATE TABLE IF NOT EXISTS material_inout_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    type INT COMMENT '1入库 2出库',
    quantity DECIMAL(10,2) COMMENT '数量',
    related_task_id BIGINT COMMENT '关联任务ID (如果是任务消耗)',
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='物资出入库流水';
