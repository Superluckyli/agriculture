-- =============================================================
-- Supplemental IoT seed data for the refactored IoT schema.
-- Loaded after the legacy data.sql in tests.
-- =============================================================

DELETE FROM iot_task_dispatch_record;
DELETE FROM iot_warning_event;
DELETE FROM iot_simulation_profile;
DELETE FROM iot_device_binding;
DELETE FROM iot_device;
DELETE FROM iot_sensor_data;
DELETE FROM agri_task_rule;

INSERT INTO iot_device (device_id, device_code, device_name, source_type, device_status, last_reported_at, remark, created_at, updated_at) VALUES
(1, 'SIM-FL-A01', 'Farmland A simulator', 'SIMULATED', 'ONLINE', NOW(), 'Test simulator', NOW(), NOW()),
(2, 'SIM-FL-B01', 'Farmland B simulator', 'SIMULATED', 'ONLINE', NOW(), 'Test simulator', NOW(), NOW()),
(3, 'SIM-FL-C01', 'Greenhouse simulator', 'SIMULATED', 'ONLINE', NOW(), 'Test simulator', NOW(), NOW());

INSERT INTO iot_device_binding (binding_id, device_id, farmland_id, is_active, started_at, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), NOW(), NOW()),
(2, 2, 2, 1, NOW(), NOW(), NOW()),
(3, 3, 3, 1, NOW(), NOW(), NOW());

INSERT INTO iot_simulation_profile (profile_id, device_id, sensor_type, base_value, fluctuation_range, warning_value, warning_probability, interval_seconds, is_enabled, created_at, updated_at) VALUES
(1, 1, 'temperature', 26.00, 4.00, 38.00, 0.15, 600, 1, NOW(), NOW()),
(2, 1, 'humidity', 65.00, 8.00, 25.00, 0.20, 600, 1, NOW(), NOW()),
(3, 1, 'light', 1800.00, 500.00, 600.00, 0.08, 600, 1, NOW(), NOW()),
(4, 1, 'soil_moisture', 43.00, 7.00, 22.00, 0.18, 600, 1, NOW(), NOW()),
(5, 1, 'ph', 6.30, 0.40, 4.80, 0.06, 600, 1, NOW(), NOW()),
(6, 2, 'temperature', 24.00, 5.00, 36.00, 0.12, 600, 1, NOW(), NOW()),
(7, 2, 'humidity', 60.00, 9.00, 28.00, 0.22, 600, 1, NOW(), NOW()),
(8, 2, 'light', 1600.00, 420.00, 500.00, 0.08, 600, 1, NOW(), NOW()),
(9, 2, 'soil_moisture', 39.00, 6.00, 20.00, 0.15, 600, 1, NOW(), NOW()),
(10, 2, 'ph', 6.60, 0.30, 5.10, 0.05, 600, 1, NOW(), NOW()),
(11, 3, 'temperature', 27.00, 3.00, 39.00, 0.18, 600, 1, NOW(), NOW()),
(12, 3, 'humidity', 68.00, 7.00, 24.00, 0.20, 600, 1, NOW(), NOW()),
(13, 3, 'light', 2200.00, 650.00, 700.00, 0.05, 600, 1, NOW(), NOW()),
(14, 3, 'soil_moisture', 47.00, 5.00, 24.00, 0.12, 600, 1, NOW(), NOW()),
(15, 3, 'ph', 6.10, 0.25, 5.00, 0.04, 600, 1, NOW(), NOW());

INSERT INTO agri_task_rule (
    rule_id, rule_name, sensor_type, trigger_condition, min_value, max_value,
    create_mode, task_type, task_priority, dispatch_cooldown_minutes, is_enabled, created_at, updated_at
) VALUES
(1, 'Low temperature alert', 'temperature', 'LT', 10.00, NULL, 'AUTO', 'Insulation', 2, 60, 1, NOW(), NOW()),
(2, 'High temperature alert', 'temperature', 'GT', NULL, 35.00, 'AUTO_AI', 'Cooling', 1, 60, 1, NOW(), NOW()),
(3, 'Low humidity alert', 'humidity', 'LT', 30.00, NULL, 'AUTO', 'Irrigation', 2, 45, 1, NOW(), NOW());

INSERT INTO iot_sensor_data (
    data_id, device_id, farmland_id, plot_id, sensor_type, sensor_value, unit,
    source_type, quality_status, reported_at, created_at
) VALUES
(1, 1, 1, 'FL-A01', 'temperature', 18.50, '℃', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 1, 1, 'FL-A01', 'humidity', 72.30, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 1, 1, 'FL-A01', 'light', 1850.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 110 MINUTE), DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
(4, 1, 1, 'FL-A01', 'soil_moisture', 41.20, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 100 MINUTE), DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
(5, 1, 1, 'FL-A01', 'ph', 6.20, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 95 MINUTE), DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
(6, 2, 2, 'FL-B01', 'temperature', 16.80, '℃', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(7, 2, 2, 'FL-B01', 'humidity', 65.40, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(8, 2, 2, 'FL-B01', 'light', 1520.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 55 MINUTE), DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(9, 2, 2, 'FL-B01', 'soil_moisture', 37.80, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(10, 2, 2, 'FL-B01', 'ph', 6.55, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(11, 3, 3, 'FL-C01', 'temperature', 20.10, '℃', 'SIMULATED', 'VALID', NOW(), NOW()),
(12, 3, 3, 'FL-C01', 'humidity', 58.90, '%', 'SIMULATED', 'VALID', NOW(), NOW()),
(13, 3, 3, 'FL-C01', 'light', 2310.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(14, 3, 3, 'FL-C01', 'soil_moisture', 46.50, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(15, 3, 3, 'FL-C01', 'ph', 6.05, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE));
