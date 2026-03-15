-- =================================================================
-- Flyway V3: 报表查询优化索引
-- =================================================================

-- 作物批次按状态分组 (Dashboard 饼图)
CREATE INDEX idx_batch_status ON agri_crop_batch (status);

-- 作物批次按品种关联 (饼图 JOIN)
CREATE INDEX idx_batch_variety ON agri_crop_batch (variety_id);

-- 传感器数据按地块+类型+时间 (Dashboard 覆盖索引)
CREATE INDEX idx_sensor_plot_type_time ON iot_sensor_data (plot_id, sensor_type, create_time);
