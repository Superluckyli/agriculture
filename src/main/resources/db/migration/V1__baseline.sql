-- =================================================================
-- Flyway V1 Baseline: 智慧农业管理系统完整 Schema
-- 策略: CREATE TABLE IF NOT EXISTS (幂等，兼容已有数据库)
-- =================================================================

-- 1. System Module
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

-- 2. Crop Module
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

CREATE TABLE IF NOT EXISTS agri_farmland (
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

-- 3. Supplier Module
CREATE TABLE IF NOT EXISTS supplier_info (
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

-- 4. Crop Batch
CREATE TABLE IF NOT EXISTS agri_crop_batch (
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
    status VARCHAR(32) NOT NULL DEFAULT 'not_started',
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

-- 5. Material Info
CREATE TABLE IF NOT EXISTS material_info (
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
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_tenant_name (tenant_id, name),
    INDEX idx_material_supplier (supplier_id),
    INDEX idx_material_stock (current_stock, safe_threshold)
);

-- 6. Task
CREATE TABLE IF NOT EXISTS agri_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    batch_id BIGINT NOT NULL,
    task_no VARCHAR(64),
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50),
    task_source VARCHAR(16) NOT NULL DEFAULT 'manual',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
    need_review TINYINT NOT NULL DEFAULT 0,
    priority INT DEFAULT 0,
    plan_time DATETIME,
    deadline_at DATETIME,
    status_v2 VARCHAR(32) NOT NULL DEFAULT 'pending_accept',
    assignee_id BIGINT,
    assign_time DATETIME,
    assign_by BIGINT,
    assign_remark VARCHAR(255),
    reviewer_user_id BIGINT,
    accept_time DATETIME,
    accept_by BIGINT,
    completed_at DATETIME,
    reject_time DATETIME,
    reject_by BIGINT,
    reject_reason VARCHAR(255),
    reject_reason_type VARCHAR(32),
    suspend_reason VARCHAR(255),
    cancel_reason VARCHAR(255),
    source_rule_id BIGINT,
    source_farmland_id BIGINT,
    suggest_action TEXT,
    precaution_note TEXT,
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

-- 7. Task Material
CREATE TABLE IF NOT EXISTS agri_task_material (
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

-- 8. Task Log
CREATE TABLE IF NOT EXISTS agri_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    batch_id BIGINT,
    action VARCHAR(32) NOT NULL,
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

-- 9. Material Stock Log
CREATE TABLE IF NOT EXISTS material_stock_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    qty DECIMAL(12, 3) NOT NULL,
    before_stock DECIMAL(12, 3) NOT NULL,
    after_stock DECIMAL(12, 3) NOT NULL,
    related_type VARCHAR(32),
    related_id BIGINT,
    operator_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_log_material (material_id, created_at),
    INDEX idx_stock_log_related (related_type, related_id)
);

-- 10. Purchase Order
CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    org_id BIGINT NOT NULL DEFAULT 1,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
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

-- 11. Purchase Order Item
CREATE TABLE IF NOT EXISTS purchase_order_item (
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

-- 12. Payment Record
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    pay_method VARCHAR(32),
    pay_amount DECIMAL(14, 2),
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    pay_time DATETIME,
    operator_id BIGINT,
    remark VARCHAR(255),
    INDEX idx_payment_order (purchase_order_id)
);

-- 13. IoT Module
CREATE TABLE IF NOT EXISTS iot_sensor_data (
    data_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plot_id VARCHAR(50),
    sensor_type VARCHAR(20),
    value DECIMAL(10, 2),
    unit VARCHAR(10),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plot_time (plot_id, create_time)
);

CREATE TABLE IF NOT EXISTS agri_task_rule (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(50),
    sensor_type VARCHAR(20),
    min_val DECIMAL(10, 2),
    max_val DECIMAL(10, 2),
    auto_task_type VARCHAR(50),
    priority INT DEFAULT 1,
    is_enable INT DEFAULT 1,
    cooldown_minutes INT DEFAULT 60
);
