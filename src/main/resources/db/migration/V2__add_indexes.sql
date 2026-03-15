-- =================================================================
-- Flyway V2: 补充缺失索引
-- =================================================================

-- 任务按状态+截止日期查询 (超期调度器 + 列表过滤)
CREATE INDEX idx_task_status_deadline ON agri_task (status_v2, deadline_at);

-- 任务按创建时间排序/过滤 (报表近7天趋势)
CREATE INDEX idx_task_create_time ON agri_task (create_time);

-- 支付记录按订单+时间查询 (累计支付校验)
CREATE INDEX idx_payment_order_time ON payment_record (purchase_order_id, pay_time);

-- 任务日志按操作类型查询 (审计追溯)
CREATE INDEX idx_task_log_action ON agri_task_log (task_id, action);

-- IoT 传感器按类型+时间查询 (报表均值计算)
CREATE INDEX idx_sensor_type_time ON iot_sensor_data (sensor_type, create_time);

-- 供应商按名称检索
CREATE INDEX idx_supplier_name ON supplier_info (name);
