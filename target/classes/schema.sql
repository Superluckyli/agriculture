-- ==========================================
-- 1. System Module
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

-- ==========================================
-- 2. Crop Module
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

CREATE TABLE IF NOT EXISTS crop_batch (
    batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variety_id BIGINT NOT NULL,
    plot_id VARCHAR(50) NOT NULL,
    sowing_date DATE,
    expected_harvest_date DATE,
    current_stage VARCHAR(50),
    is_active INT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS growth_stage_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    stage_name VARCHAR(50),
    log_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    image_url VARCHAR(500),
    description TEXT
);

-- ==========================================
-- 3. IoT Module
-- ==========================================

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
    is_enable INT DEFAULT 1
);

-- ==========================================
-- 4. Task Module
-- ==========================================

CREATE TABLE IF NOT EXISTS agri_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT,
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50),
    priority INT DEFAULT 0,
    plan_time DATETIME,
    status INT DEFAULT 0 COMMENT '0待分配 1待接单 2已接单 3已完成 4已逾期 5已拒单',
    executor_id BIGINT COMMENT 'legacy field, compatibility',
    assignee_id BIGINT COMMENT 'current assigned farmer',
    assign_time DATETIME,
    assign_by BIGINT,
    assign_remark VARCHAR(255),
    accept_time DATETIME,
    accept_by BIGINT,
    reject_time DATETIME,
    reject_by BIGINT,
    reject_reason VARCHAR(255),
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time DATETIME,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS task_execution_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    actual_start_time DATETIME,
    actual_end_time DATETIME,
    status_snapshot INT,
    photo_url VARCHAR(500),
    material_cost_json JSON,
    problem_desc TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_flow_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL COMMENT 'assign/accept/reject',
    from_status INT NOT NULL,
    to_status INT NOT NULL,
    operator_id BIGINT NOT NULL,
    target_user_id BIGINT,
    remark VARCHAR(255),
    trace_id VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. Material Module
-- ==========================================

CREATE TABLE IF NOT EXISTS material_info (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10, 2),
    stock_quantity DECIMAL(10, 2) DEFAULT 0,
    unit VARCHAR(20),
    update_time DATETIME
);

CREATE TABLE IF NOT EXISTS material_inout_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    type INT,
    quantity DECIMAL(10, 2),
    related_task_id BIGINT,
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
