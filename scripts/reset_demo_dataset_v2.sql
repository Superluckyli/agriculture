SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM iot_task_dispatch_record;
DELETE FROM iot_warning_event;
DELETE FROM iot_simulation_profile;
DELETE FROM iot_device_binding;
DELETE FROM iot_device;
DELETE FROM iot_sensor_data;

DELETE FROM payment_record;
DELETE FROM purchase_order_item;
DELETE FROM purchase_order;
DELETE FROM stock_adjustment;
DELETE FROM material_stock_log;

DELETE FROM agri_task_log;
DELETE FROM agri_task_material;
DELETE FROM agri_task;
DELETE FROM agri_crop_batch;

DELETE FROM material_info;
DELETE FROM supplier_info;
DELETE FROM agri_farmland;
DELETE FROM base_crop_variety;

DELETE FROM chat_read_state;
DELETE FROM chat_message;
DELETE FROM chat_conversation;
DELETE FROM sys_audit_log;

DELETE FROM agri_task_rule;

DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_menu;
DELETE FROM sys_user;
DELETE FROM sys_role;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_role (role_id, role_name, role_key, create_time) VALUES
(1, '管理员', 'ADMIN', NOW()),
(2, '场长', 'FARM_OWNER', NOW()),
(3, '技术员', 'TECHNICIAN', NOW()),
(4, '工人', 'WORKER', NOW());

INSERT INTO sys_user (user_id, username, password, real_name, phone, dept_name, status, create_time) VALUES
(1, 'admin', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '系统管理员', '13800000001', '管理部', 1, NOW()),
(2, 'owner_chen', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '陈场长', '13800000002', '农场运营部', 1, NOW()),
(3, 'tech_li', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '李技术', '13800000003', '农技组', 1, NOW()),
(4, 'worker_wang', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '王工', '13800000004', '作业一组', 1, NOW()),
(5, 'worker_zhao', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '赵工', '13800000005', '作业二组', 1, NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 4);

INSERT INTO sys_menu (menu_id, parent_id, menu_name, path, perms, type, order_num) VALUES
(1, 0, '仪表盘', '/dashboard', NULL, 1, 1),
(2, 0, '种植管理', NULL, NULL, 1, 2),
(3, 0, '任务管理', NULL, NULL, 1, 3),
(4, 0, '物资管理', NULL, NULL, 1, 4),
(5, 0, '采购管理', NULL, NULL, 1, 5),
(6, 0, '设备监测', NULL, NULL, 1, 6),
(7, 0, '统计报表', '/report/analytics', NULL, 1, 7),
(8, 0, '系统设置', NULL, NULL, 1, 8),
(20, 2, '农田管理', '/crop/farmland', NULL, 2, 1),
(21, 2, '批次管理', '/crop/batch', NULL, 2, 2),
(22, 2, '品种管理', '/crop/variety', NULL, 2, 3),
(30, 3, '任务调度', '/task/list', NULL, 2, 1),
(31, 3, '我的任务', '/task/my', NULL, 2, 2),
(32, 3, '执行日志', '/task/log', NULL, 2, 3),
(40, 4, '物资台账', '/material/inventory', NULL, 2, 1),
(41, 4, '库存流水', '/material/stock-log', NULL, 2, 2),
(50, 5, '采购订单', '/purchase/order', NULL, 2, 1),
(51, 5, '供应商', '/purchase/supplier', NULL, 2, 2),
(60, 6, '设备监测', '/iot/monitor', NULL, 2, 1),
(61, 6, '预警规则', '/iot/rule', NULL, 2, 2),
(70, 8, '用户管理', '/system/user', NULL, 2, 1),
(71, 8, '角色管理', '/system/role', NULL, 2, 2),
(72, 8, '聊天中心', '/chat', NULL, 2, 3);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),
(1,20),(1,21),(1,22),(1,30),(1,31),(1,32),(1,40),(1,41),(1,50),(1,51),(1,60),(1,61),(1,70),(1,71),(1,72),
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),
(2,20),(2,21),(2,22),(2,30),(2,32),(2,40),(2,41),(2,50),(2,51),(2,60),(2,61),(2,70),(2,71),(2,72),
(3,1),(3,2),(3,3),(3,4),(3,6),(3,7),
(3,20),(3,21),(3,22),(3,30),(3,32),(3,40),(3,41),(3,60),(3,61),
(4,1),(4,3),(4,31),(4,32);

INSERT INTO base_crop_variety (variety_id, crop_name, growth_cycle_days, ideal_humidity_min, ideal_humidity_max, ideal_temp_min, ideal_temp_max, create_time) VALUES
(1, '土豆', 110, 55.00, 80.00, 12.00, 26.00, NOW()),
(2, '大麦', 120, 45.00, 70.00, 8.00, 24.00, NOW()),
(3, '番茄', 95, 60.00, 85.00, 18.00, 30.00, NOW()),
(4, '草莓', 130, 65.00, 90.00, 12.00, 24.00, NOW());

INSERT INTO agri_farmland (id, tenant_id, org_id, name, code, location, area, manager_user_id, crop_adapt_note, status, created_at, updated_at) VALUES
(1, 1, 1, '东区露天地块', 'FL-D01', '东区主灌溉渠旁', 18.50, 2, '适合春季块茎作物', 1, NOW(), NOW()),
(2, 1, 1, '北区轮作地块', 'FL-E02', '北区缓坡地', 22.00, 2, '适合秋冬粮食作物', 1, NOW(), NOW()),
(3, 1, 1, '南区智能温室', 'GH-S03', '南区一号温室', 6.20, 3, '适合果蔬与试验性种植', 1, NOW(), NOW());

INSERT INTO supplier_info (id, tenant_id, name, contact_name, phone, address, remark, status, created_at) VALUES
(1, 1, '丰禾农资供应', '陈经理', '13900001001', '市农资市场A区12号', '肥料、农药常规供应', 1, NOW()),
(2, 1, '润泽灌溉设备', '刘经理', '13900001002', '市农机园B区5号', '滴灌与管件配套供应', 1, NOW()),
(3, 1, '青禾温室耗材', '王经理', '13900001003', '高新区园艺路18号', '温室膜与苗盘供应', 1, NOW());

INSERT INTO material_info (material_id, tenant_id, org_id, name, category, specification, unit, current_stock, safe_threshold, suggest_purchase_qty, supplier_id, unit_price, status, version, created_at, updated_at) VALUES
(1, 1, 1, '高钾复合肥', '肥料', '40kg/袋', 'kg', 420.000, 180.000, 220.000, 1, 3.60, 1, 0, NOW(), NOW()),
(2, 1, 1, '滴灌带', '工具', 'PE 16mm', '卷', 18.000, 30.000, 25.000, 2, 48.00, 1, 0, NOW(), NOW()),
(3, 1, 1, '生物杀虫剂', '农药', '1L/瓶', '瓶', 26.000, 20.000, 18.000, 1, 78.00, 1, 0, NOW(), NOW()),
(4, 1, 1, '温室棚膜', '材料', '8m*50m', '卷', 6.000, 12.000, 10.000, 3, 320.00, 1, 0, NOW(), NOW()),
(5, 1, 1, '大麦种子', '种子', '25kg/袋', 'kg', 90.000, 120.000, 160.000, 1, 5.20, 1, 0, NOW(), NOW());

INSERT INTO agri_crop_batch (
    id, tenant_id, org_id, batch_no, farmland_id, variety_id, crop_variety,
    planting_date, estimated_harvest_date, actual_harvest_date, stage, status,
    owner_user_id, target_output, actual_output, abandon_reason, remark, created_at, updated_at
) VALUES
(1, 1, 1, 'B-2026-POTATO-01', 1, 1, '土豆', '2026-03-05', '2026-06-25', NULL, '膨大期', 'in_progress', 2, 7500.00, NULL, NULL, '春季主力土豆批次', NOW(), NOW()),
(2, 1, 1, 'B-2026-BARLEY-01', 2, 2, '大麦', '2026-09-10', '2026-12-28', NULL, '计划中', 'not_started', 2, 6800.00, NULL, NULL, '秋季轮作大麦批次', NOW(), NOW()),
(3, 1, 1, 'B-2026-TOMATO-01', 3, 3, '番茄', '2026-02-20', '2026-06-05', NULL, '结果期', 'in_progress', 3, 5200.00, NULL, NULL, '温室番茄试验批次', NOW(), NOW());

INSERT INTO agri_task (
    task_id, tenant_id, org_id, batch_id, task_no, task_name, task_type, task_source, risk_level,
    need_review, priority, plan_time, deadline_at, status_v2, assignee_id, assign_time, assign_by,
    assign_remark, reviewer_user_id, accept_time, accept_by, completed_at, reject_time, reject_by,
    reject_reason, reject_reason_type, suspend_reason, cancel_reason, source_rule_id, source_farmland_id,
    suggest_action, precaution_note, create_by, create_time, update_by, update_time, version
) VALUES
(1, 1, 1, 1, 'TASK-20260319-001', '东区土豆地块巡检', '巡检', 'manual', 'LOW', 0, 2,
 DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 'pending_accept', 4, NOW(), 3,
 '今日下午完成例行巡检', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 '检查块茎膨大与叶面病斑', '关注土壤墒情变化', 3, NOW(), 3, NOW(), 0),
(2, 1, 1, 3, 'TASK-20260319-002', '温室番茄高温处置任务', '降温处理', 'iot_warning', 'HIGH', 1, 1,
 DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), 'pending_review', 3, NOW(), 2,
 'IoT 高温预警自动生成', 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, 3,
 '建议先打开通风并检查喷雾降温系统', '温室当前温度持续偏高', 2, NOW(), 2, NOW(), 0),
(3, 1, 1, 1, 'TASK-20260319-003', '滴灌带更换准备', '设备准备', 'manual', 'MEDIUM', 0, 2,
 DATE_ADD(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), 'created', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 '准备低库存滴灌带并核对采购计划', '先确认现有库存与缺口', 3, NOW(), 3, NOW(), 0);

INSERT INTO agri_task_log (id, task_id, batch_id, action, from_status, to_status, operator_id, target_user_id, growth_note, image_urls, abnormal_note, remark, trace_id, created_at) VALUES
(1, 1, 1, 'create', NULL, 'pending_accept', 3, 4, NULL, NULL, NULL, '技术员创建巡检任务', 'TRACE-TASK-001', NOW()),
(2, 2, 3, 'create', NULL, 'pending_review', 0, NULL, NULL, NULL, NULL, 'IoT 预警自动创建任务', 'TRACE-IOT-001', NOW()),
(3, 3, 1, 'create', NULL, 'created', 3, NULL, NULL, NULL, NULL, '低库存准备任务', 'TRACE-TASK-003', NOW());

INSERT INTO material_stock_log (id, material_id, change_type, qty, before_stock, after_stock, related_type, related_id, operator_id, remark, created_at) VALUES
(1, 2, 'OUT', 8.000, 26.000, 18.000, 'task', 3, 3, '滴灌带领用准备', NOW()),
(2, 4, 'OUT', 2.000, 8.000, 6.000, 'manual', NULL, 3, '温室棚膜抽检耗材', NOW());

INSERT INTO purchase_order (id, tenant_id, org_id, order_no, status, supplier_id, total_amount, pay_method, remark, created_by, confirmed_by, version, created_at, updated_at) VALUES
(1, 1, 1, 'PO-20260319-001', 'draft', 2, 1200.00, NULL, '滴灌带低库存补货草稿', 3, NULL, 0, NOW(), NOW()),
(2, 1, 1, 'PO-20260319-002', 'confirmed', 3, 3200.00, 'bank_transfer', '温室棚膜季度补货', 2, 2, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

INSERT INTO purchase_order_item (id, purchase_order_id, material_id, purchase_qty, receive_qty, unit_price, line_amount, remark) VALUES
(1, 1, 2, 25.000, 0.000, 48.00, 1200.00, '滴灌带缺口补货'),
(2, 2, 4, 10.000, 10.000, 320.00, 3200.00, '温室棚膜季度采购');

INSERT INTO payment_record (id, purchase_order_id, pay_method, pay_amount, status, pay_time, operator_id, remark) VALUES
(1, 2, 'bank_transfer', 3200.00, 'paid', DATE_SUB(NOW(), INTERVAL 4 DAY), 2, '已完成对公付款');

INSERT INTO iot_device (device_id, device_code, device_name, source_type, device_status, last_reported_at, remark, created_at, updated_at) VALUES
(1, 'SIM-FL-D01', '东区露天地块模拟设备', 'SIMULATED', 'ONLINE', NOW(), '用于东区块茎作物监测', NOW(), NOW()),
(2, 'SIM-FL-E02', '北区轮作地块模拟设备', 'SIMULATED', 'ONLINE', NOW(), '用于北区轮作地块监测', NOW(), NOW()),
(3, 'SIM-GH-S03', '南区温室模拟设备', 'SIMULATED', 'ONLINE', NOW(), '用于温室环境监测', NOW(), NOW());

INSERT INTO iot_device_binding (binding_id, device_id, farmland_id, is_active, started_at, ended_at, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), NULL, NOW(), NOW()),
(2, 2, 2, 1, NOW(), NULL, NOW(), NOW()),
(3, 3, 3, 1, NOW(), NULL, NOW(), NOW());

INSERT INTO iot_simulation_profile (profile_id, device_id, sensor_type, base_value, fluctuation_range, warning_value, warning_probability, interval_seconds, is_enabled, created_at, updated_at) VALUES
(1, 1, 'temperature', 21.00, 4.00, 8.00, 0.12, 600, 1, NOW(), NOW()),
(2, 1, 'humidity', 62.00, 8.00, 28.00, 0.18, 600, 1, NOW(), NOW()),
(3, 1, 'light', 1850.00, 500.00, 600.00, 0.08, 600, 1, NOW(), NOW()),
(4, 1, 'soil_moisture', 43.00, 7.00, 22.00, 0.18, 600, 1, NOW(), NOW()),
(5, 1, 'ph', 6.30, 0.40, 4.80, 0.06, 600, 1, NOW(), NOW()),
(6, 2, 'temperature', 17.00, 5.00, 5.00, 0.10, 900, 1, NOW(), NOW()),
(7, 2, 'humidity', 58.00, 9.00, 26.00, 0.15, 900, 1, NOW(), NOW()),
(8, 2, 'light', 1600.00, 420.00, 500.00, 0.08, 900, 1, NOW(), NOW()),
(9, 2, 'soil_moisture', 39.00, 6.00, 20.00, 0.15, 900, 1, NOW(), NOW()),
(10, 2, 'ph', 6.60, 0.30, 5.10, 0.05, 900, 1, NOW(), NOW()),
(11, 3, 'temperature', 27.00, 3.00, 38.00, 0.20, 300, 1, NOW(), NOW()),
(12, 3, 'humidity', 70.00, 6.00, 32.00, 0.10, 300, 1, NOW(), NOW()),
(13, 3, 'light', 2200.00, 650.00, 700.00, 0.05, 300, 1, NOW(), NOW()),
(14, 3, 'soil_moisture', 47.00, 5.00, 24.00, 0.12, 300, 1, NOW(), NOW()),
(15, 3, 'ph', 6.10, 0.25, 5.00, 0.04, 300, 1, NOW(), NOW());

INSERT INTO agri_task_rule (
    rule_id, rule_name, sensor_type, trigger_condition, min_value, max_value, create_mode,
    task_type, task_priority, dispatch_cooldown_minutes, is_enabled, created_at, updated_at
) VALUES
(1, '土豆地块低温预警', 'temperature', 'LT', 10.00, NULL, 'MANUAL', '低温巡查', 2, 90, 1, NOW(), NOW()),
(2, '温室番茄高温预警', 'temperature', 'GT', NULL, 35.00, 'AUTO_AI', '降温处理', 1, 60, 1, NOW(), NOW()),
(3, '露天地块低湿预警', 'humidity', 'LT', 30.00, NULL, 'AUTO', '灌溉处理', 2, 45, 1, NOW(), NOW());

INSERT INTO iot_sensor_data (
    data_id, device_id, farmland_id, plot_id, sensor_type, sensor_value, unit,
    source_type, quality_status, reported_at, created_at
) VALUES
(1, 1, 1, 'FL-D01', 'temperature', 9.20, '℃', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 35 MINUTE), DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(2, 1, 1, 'FL-D01', 'humidity', 29.50, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(3, 1, 1, 'FL-D01', 'light', 1780.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 28 MINUTE), DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(4, 1, 1, 'FL-D01', 'soil_moisture', 24.60, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 22 MINUTE), DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
(5, 1, 1, 'FL-D01', 'ph', 6.25, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 18 MINUTE), DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(6, 2, 2, 'FL-E02', 'temperature', 16.60, '℃', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(7, 2, 2, 'FL-E02', 'humidity', 54.20, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(8, 2, 2, 'FL-E02', 'light', 1490.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 17 MINUTE), DATE_SUB(NOW(), INTERVAL 17 MINUTE)),
(9, 2, 2, 'FL-E02', 'soil_moisture', 38.40, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 14 MINUTE), DATE_SUB(NOW(), INTERVAL 14 MINUTE)),
(10, 2, 2, 'FL-E02', 'ph', 6.58, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 12 MINUTE), DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(11, 3, 3, 'GH-S03', 'temperature', 37.80, '℃', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 12 MINUTE), DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(12, 3, 3, 'GH-S03', 'humidity', 66.40, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(13, 3, 3, 'GH-S03', 'light', 2360.00, 'lx', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(14, 3, 3, 'GH-S03', 'soil_moisture', 45.80, '%', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 6 MINUTE), DATE_SUB(NOW(), INTERVAL 6 MINUTE)),
(15, 3, 3, 'GH-S03', 'ph', 6.08, '', 'SIMULATED', 'VALID', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE));

INSERT INTO iot_warning_event (
    event_id, rule_id, sensor_data_id, device_id, farmland_id, batch_id, sensor_type,
    trigger_value, triggered_at, handle_status, dispatch_mode, task_id, failure_reason, created_at, updated_at
) VALUES
(1, 1, 1, 1, 1, 1, 'temperature', 9.20, DATE_SUB(NOW(), INTERVAL 35 MINUTE), 'PENDING', 'MANUAL', NULL, NULL, NOW(), NOW()),
(2, 2, 5, 3, 3, 3, 'temperature', 37.80, DATE_SUB(NOW(), INTERVAL 12 MINUTE), 'TASK_CREATED', 'AUTO_AI', 2, NULL, NOW(), NOW()),
(3, 3, 8, 1, 1, 1, 'humidity', 27.80, DATE_SUB(NOW(), INTERVAL 3 MINUTE), 'TASK_LINKED', 'AUTO', 1, NULL, NOW(), NOW());

INSERT INTO iot_task_dispatch_record (
    dispatch_id, event_id, rule_id, task_id, dispatch_mode, dispatch_status, operator_id, ai_summary, error_message, created_at
) VALUES
(1, 2, 2, 2, 'AUTO_AI', 'SUCCESS', NULL, 'AI 建议优先检查温室通风和遮阳联动设备。', NULL, NOW()),
(2, 3, 3, 1, 'AUTO', 'LINKED_EXISTING', NULL, NULL, NULL, NOW());
