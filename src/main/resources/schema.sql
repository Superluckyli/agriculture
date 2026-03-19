-- ==========================================
-- [DEPRECATED] 已迁移到 Flyway: db/migration/V1__baseline.sql
-- 此文件保留仅供参考，不再由 Spring SQL Init 执行
-- ==========================================

-- ------------------------------------------
-- 0. 清理已废弃的旧表 (Phase 1 兼容清理)
-- ------------------------------------------
DROP TABLE IF EXISTS material_inout_log;
DROP TABLE IF EXISTS task_execution_log;
DROP TABLE IF EXISTS task_flow_log;
DROP TABLE IF EXISTS growth_stage_log;
DROP TABLE IF EXISTS crop_batch;

-- 清理将被重建的业务表 (反向依赖顺序)
DROP TABLE IF EXISTS payment_record;
DROP TABLE IF EXISTS purchase_order_item;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS material_stock_log;
DROP TABLE IF EXISTS agri_task_log;
DROP TABLE IF EXISTS agri_task_material;
DROP TABLE IF EXISTS agri_task;
DROP TABLE IF EXISTS agri_crop_batch;
DROP TABLE IF EXISTS material_info;
DROP TABLE IF EXISTS supplier_info;
DROP TABLE IF EXISTS agri_farmland;
DROP TABLE IF EXISTS iot_task_dispatch_record;
DROP TABLE IF EXISTS iot_warning_event;
DROP TABLE IF EXISTS iot_simulation_profile;
DROP TABLE IF EXISTS iot_device_binding;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_sensor_data;
DROP TABLE IF EXISTS agri_task_rule;

-- ==========================================
-- 1. System Module (保留不变)
-- ==========================================

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    dept_name VARCHAR(50),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_key VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    menu_name VARCHAR(50) NOT NULL,
    path VARCHAR(200),
    perms VARCHAR(100),
    type INT,
    order_num INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_conversation_pair UNIQUE (user_a_id, user_b_id),
    INDEX idx_chat_conversation_user_a (user_a_id, last_message_at),
    INDEX idx_chat_conversation_user_b (user_b_id, last_message_at)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    message_type VARCHAR(16) NOT NULL DEFAULT 'text',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_conversation (conversation_id, id),
    INDEX idx_chat_message_receiver (receiver_id, id)
);

CREATE TABLE IF NOT EXISTS chat_read_state (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT,
    last_read_at DATETIME,
    PRIMARY KEY (conversation_id, user_id),
    INDEX idx_chat_read_state_user (user_id, last_read_at)
);

-- ==========================================
-- 2. Crop Module — 品种字典 (保留不变)
-- ==========================================

CREATE TABLE IF NOT EXISTS base_crop_variety (
    variety_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_name VARCHAR(100) NOT NULL,
    growth_cycle_days INT,
    ideal_humidity_min DECIMAL(5, 2),
    ideal_humidity_max DECIMAL(5, 2),
    ideal_temp_min DECIMAL(5, 2),
    ideal_temp_max DECIMAL(5, 2),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. Crop Module — 农田 (V1 新增)
-- ==========================================

CREATE TABLE agri_farmland (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64),
    location VARCHAR(255),
    area DECIMAL(10, 2),
    manager_user_id BIGINT,
    crop_adapt_note VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_farmland_tenant_name (tenant_id, name),
    INDEX idx_farmland_manager (manager_user_id)
);

-- ==========================================
-- 4. Supplier Module (V1 新增)
-- ==========================================

CREATE TABLE supplier_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(64) NOT NULL,
    contact_name VARCHAR(64),
    phone VARCHAR(32),
    address VARCHAR(255),
    remark VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. Crop Module — 种植批次 (V1 重建，替代旧 crop_batch)
-- ==========================================

CREATE TABLE agri_crop_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    farmland_id BIGINT NOT NULL,
    variety_id BIGINT,
    crop_variety VARCHAR(64),
    planting_date DATE,
    estimated_harvest_date DATE,
    actual_harvest_date DATE,
    stage VARCHAR(32) COMMENT '当前生长阶段',
    status VARCHAR(32) NOT NULL DEFAULT 'not_started' COMMENT 'not_started/in_progress/paused/harvested/abandoned/archived',
    owner_user_id BIGINT,
    target_output DECIMAL(12, 2),
    actual_output DECIMAL(12, 2),
    abandon_reason VARCHAR(255),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch_farmland_status (farmland_id, status),
    INDEX idx_batch_owner (owner_user_id)
);

-- ==========================================
-- 6. Material Module — 物资信息 (V1 重建)
-- ==========================================

CREATE TABLE material_info (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    specification VARCHAR(64),
    unit VARCHAR(20),
    current_stock DECIMAL(12, 3) NOT NULL DEFAULT 0,
    safe_threshold DECIMAL(12, 3) NOT NULL DEFAULT 0,
    suggest_purchase_qty DECIMAL(12, 3),
    supplier_id BIGINT,
    unit_price DECIMAL(12, 2),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=停用',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_tenant_name (tenant_id, name),
    INDEX idx_material_supplier (supplier_id),
    INDEX idx_material_stock (current_stock, safe_threshold)
);

-- ==========================================
-- 7. Task Module — 农事任务 (V1 重建)
-- ==========================================

CREATE TABLE agri_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    batch_id BIGINT NOT NULL,
    task_no VARCHAR(64),
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50),
    task_source VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT 'manual/rule/ai',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH',
    need_review TINYINT NOT NULL DEFAULT 0,
    priority INT DEFAULT 0,
    plan_time DATETIME,
    deadline_at DATETIME,

    -- 任务状态 (VARCHAR, 唯一状态字段)
    status_v2 VARCHAR(32) NOT NULL DEFAULT 'pending_accept' COMMENT 'pending_review/pending_accept/in_progress/completed/rejected_reassign/rejected_review/suspended/overdue/cancelled',

    -- 指派相关
    assignee_id BIGINT,
    assign_time DATETIME,
    assign_by BIGINT,
    assign_remark VARCHAR(255),
    reviewer_user_id BIGINT,

    -- 接单/完成
    accept_time DATETIME,
    accept_by BIGINT,
    completed_at DATETIME,

    -- 拒绝
    reject_time DATETIME,
    reject_by BIGINT,
    reject_reason VARCHAR(255),
    reject_reason_type VARCHAR(32) COMMENT 'personnel/resource/environment/task_problem',

    -- 挂起/取消
    suspend_reason VARCHAR(255),
    cancel_reason VARCHAR(255),

    -- IoT 防抖追溯
    source_rule_id BIGINT COMMENT 'IoT 规则 ID（防抖键）',
    source_farmland_id BIGINT COMMENT 'IoT 触发地块 ID（防抖键）',

    -- 建议与注意事项
    suggest_action TEXT,
    precaution_note TEXT,

    -- 审计
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time DATETIME,
    version INT NOT NULL DEFAULT 0,

    INDEX idx_task_batch_status (batch_id, status_v2),
    INDEX idx_task_assignee (assignee_id, status_v2),
    INDEX idx_task_deadline (deadline_at),
    INDEX idx_task_review (need_review, status_v2),
    INDEX idx_task_no (task_no),
    INDEX idx_task_source_dedup (source_rule_id, source_farmland_id, create_time)
);

-- ==========================================
-- 8. Task Module — 任务耗材明细 (V1 新增)
-- ==========================================

CREATE TABLE agri_task_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    suggested_qty DECIMAL(12, 3),
    actual_qty DECIMAL(12, 3),
    unit_price DECIMAL(12, 2),
    deviation_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_material_task (task_id),
    INDEX idx_task_material_material (material_id)
);

-- ==========================================
-- 9. Task Module — 统一任务日志 (V1 新增)
--    替代旧 task_execution_log + task_flow_log + growth_stage_log
-- ==========================================

CREATE TABLE agri_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    batch_id BIGINT,
    action VARCHAR(32) NOT NULL COMMENT 'create/assign/review/accept/reject/start/complete/suspend/resume/cancel/reassign/execute_log',
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    operator_id BIGINT NOT NULL,
    target_user_id BIGINT,
    growth_note TEXT,
    image_urls TEXT,
    abnormal_note VARCHAR(255),
    remark VARCHAR(255),
    trace_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_log_task (task_id, created_at),
    INDEX idx_task_log_batch (batch_id, created_at)
);

-- ==========================================
-- 10. Material Module — 库存流水 (V1 新增，替代旧 material_inout_log)
-- ==========================================

CREATE TABLE material_stock_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL COMMENT 'OUT/IN/ADJUST/DAMAGE/RETURN',
    qty DECIMAL(12, 3) NOT NULL,
    before_stock DECIMAL(12, 3) NOT NULL,
    after_stock DECIMAL(12, 3) NOT NULL,
    related_type VARCHAR(32) COMMENT 'task/purchase/manual',
    related_id BIGINT,
    operator_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_log_material (material_id, created_at),
    INDEX idx_stock_log_related (related_type, related_id)
);

-- ==========================================
-- 11. Purchase Module — 采购单 (V1 新增)
-- ==========================================

CREATE TABLE purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/confirmed/paid/receiving/partial_received/completed/cancelled',
    supplier_id BIGINT,
    total_amount DECIMAL(14, 2),
    pay_method VARCHAR(32),
    remark VARCHAR(255),
    created_by BIGINT,
    confirmed_by BIGINT,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_purchase_status (status),
    INDEX idx_purchase_supplier (supplier_id)
);

-- ==========================================
-- 12. Purchase Module — 采购明细 (V1 新增)
-- ==========================================

CREATE TABLE purchase_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    purchase_qty DECIMAL(12, 3),
    receive_qty DECIMAL(12, 3) DEFAULT 0,
    unit_price DECIMAL(12, 2),
    line_amount DECIMAL(14, 2),
    remark VARCHAR(255),
    INDEX idx_poi_order (purchase_order_id),
    INDEX idx_poi_material (material_id)
);

-- ==========================================
-- 13. Purchase Module — 支付记录 (V1 新增)
-- ==========================================

CREATE TABLE payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    pay_method VARCHAR(32),
    pay_amount DECIMAL(14, 2),
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/failed',
    pay_time DATETIME,
    operator_id BIGINT,
    remark VARCHAR(255),
    INDEX idx_payment_order (purchase_order_id)
);

-- ==========================================
-- 14. IoT Module (保留不变，V1.5 使用)
-- ==========================================

CREATE TABLE iot_device (
    device_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(20) NOT NULL COMMENT 'PHYSICAL/SIMULATED',
    device_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/OFFLINE/DISABLED',
    last_reported_at DATETIME,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_device_source_status (source_type, device_status)
);

CREATE TABLE iot_device_binding (
    binding_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    farmland_id BIGINT NOT NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_binding_device (device_id, is_active),
    INDEX idx_iot_binding_farmland (farmland_id, is_active)
);

CREATE TABLE iot_simulation_profile (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    sensor_type VARCHAR(30) NOT NULL,
    base_value DECIMAL(10, 2) NOT NULL,
    fluctuation_range DECIMAL(10, 2) NOT NULL,
    warning_value DECIMAL(10, 2) NOT NULL,
    warning_probability DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    interval_seconds INT NOT NULL DEFAULT 600,
    is_enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_iot_sim_profile_device_sensor (device_id, sensor_type),
    INDEX idx_iot_sim_profile_enabled (is_enabled)
);

CREATE TABLE iot_sensor_data (
    data_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    farmland_id BIGINT NOT NULL,
    plot_id VARCHAR(50) COMMENT '地块编码快照/兼容展示字段',
    sensor_type VARCHAR(30) NOT NULL,
    sensor_value DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20),
    source_type VARCHAR(20) NOT NULL COMMENT 'PHYSICAL/SIMULATED',
    quality_status VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID/INVALID',
    reported_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_iot_sensor_plot_time (plot_id, reported_at),
    INDEX idx_iot_sensor_farmland_type_time (farmland_id, sensor_type, reported_at),
    INDEX idx_iot_sensor_device_time (device_id, reported_at),
    INDEX idx_iot_sensor_source (source_type, quality_status)
);

CREATE TABLE agri_task_rule (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    sensor_type VARCHAR(30) NOT NULL,
    trigger_condition VARCHAR(20) NOT NULL DEFAULT 'OUTSIDE_RANGE' COMMENT 'LT/GT/OUTSIDE_RANGE',
    min_value DECIMAL(10, 2),
    max_value DECIMAL(10, 2),
    create_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO' COMMENT 'MANUAL/AUTO/AUTO_AI',
    task_type VARCHAR(50),
    task_priority INT DEFAULT 2,
    dispatch_cooldown_minutes INT NOT NULL DEFAULT 60,
    is_enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_rule_sensor_enabled (sensor_type, is_enabled)
);

CREATE TABLE iot_warning_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    sensor_data_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    farmland_id BIGINT NOT NULL,
    batch_id BIGINT,
    sensor_type VARCHAR(30) NOT NULL,
    trigger_value DECIMAL(10, 2) NOT NULL,
    triggered_at DATETIME NOT NULL,
    handle_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/TASK_CREATED/TASK_LINKED/FAILED',
    dispatch_mode VARCHAR(20) COMMENT 'MANUAL/AUTO/AUTO_AI',
    task_id BIGINT,
    failure_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_warning_rule_time (rule_id, triggered_at),
    INDEX idx_iot_warning_farmland_time (farmland_id, triggered_at),
    INDEX idx_iot_warning_handle_status (handle_status, triggered_at),
    INDEX idx_iot_warning_batch (batch_id)
);

CREATE TABLE iot_task_dispatch_record (
    dispatch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    task_id BIGINT,
    dispatch_mode VARCHAR(20) NOT NULL COMMENT 'MANUAL/AUTO/AUTO_AI',
    dispatch_status VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/LINKED_EXISTING',
    operator_id BIGINT,
    ai_summary TEXT,
    error_message VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_iot_dispatch_event (event_id),
    INDEX idx_iot_dispatch_rule (rule_id, created_at),
    INDEX idx_iot_dispatch_task (task_id)
);
