-- =============================================================
-- data.sql  ·  测试数据（与 schema.sql 严格对应）
-- 插入顺序：sys_role → sys_user → sys_user_role
--           → base_crop_variety → crop_batch
--           → agri_task_rule → material_info
--           → agri_task → task_flow_log → task_execution_log
--           → material_inout_log → growth_stage_log → iot_sensor_data
-- 密码统一为 123456 的 BCrypt 散列
-- =============================================================

-- ----------------------------------------------------------
-- 0. 幂等清理（保证重启不报重复键）
-- ----------------------------------------------------------
DELETE FROM material_inout_log;
DELETE FROM task_execution_log;
DELETE FROM task_flow_log;
DELETE FROM agri_task;
DELETE FROM growth_stage_log;
DELETE FROM crop_batch;
DELETE FROM iot_sensor_data;
DELETE FROM agri_task_rule;
DELETE FROM material_info;
DELETE FROM base_crop_variety;
DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_menu;
DELETE FROM sys_user;
DELETE FROM sys_role;

-- ----------------------------------------------------------
-- 1. sys_role（角色）
-- ----------------------------------------------------------
INSERT INTO sys_role (role_id, role_name, role_key, create_time) VALUES
(1, '超级管理员', 'ADMIN',      NOW()),
(2, '农场主',     'FARM_OWNER', NOW()),
(3, '农户',       'FARMER',     NOW()),
(4, '工人',       'WORKER',     NOW()),
(5, '管理员',     'MANAGER',    NOW()),
(6, '演示账号',   'DEMO',       NOW());

-- ----------------------------------------------------------
-- 2. sys_user（8 个用户）
--    BCrypt(123456) = $2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm
-- ----------------------------------------------------------
INSERT INTO sys_user (user_id, username, password, real_name, phone, dept_name, status, create_time) VALUES
(1,  'admin',      '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '系统管理员', '13800000001', '技术部',   1, NOW()),
(2,  'farm_owner', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '张大地',     '13800000002', '农场管理部', 1, NOW()),
(3,  'farmer1',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '李田一',     '13800000003', '种植一组',  1, NOW()),
(4,  'farmer2',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '王田二',     '13800000004', '种植二组',  1, NOW()),
(5,  'worker1',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '赵工一',     '13800000005', '作业组',    1, NOW()),
(6,  'worker2',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '孙工二',     '13800000006', '作业组',    1, NOW()),
(7,  'manager',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '钱经理',     '13800000007', '运营管理部', 1, NOW()),
(8,  'demo',       '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '演示用户',   '13800000008', '演示部门',  1, NOW());

-- ----------------------------------------------------------
-- 3. sys_user_role（用户-角色映射）
-- ----------------------------------------------------------
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin       → ADMIN
(2, 2),  -- farm_owner  → FARM_OWNER
(3, 3),  -- farmer1     → FARMER
(4, 3),  -- farmer2     → FARMER
(5, 4),  -- worker1     → WORKER
(6, 4),  -- worker2     → WORKER
(7, 5),  -- manager     → MANAGER
(8, 6);  -- demo        → DEMO

-- ----------------------------------------------------------
-- 4. base_crop_variety（作物品种）
-- ----------------------------------------------------------
INSERT INTO base_crop_variety (variety_id, crop_name, growth_cycle_days, ideal_humidity_min, ideal_humidity_max, ideal_temp_min, ideal_temp_max, create_time) VALUES
(1, '水稻',     150, 60.00, 90.00, 15.00, 30.00, NOW()),
(2, '小麦',     120, 40.00, 70.00, 10.00, 25.00, NOW()),
(3, '玉米',     100, 50.00, 80.00, 18.00, 35.00, NOW()),
(4, '大豆',      90, 55.00, 75.00, 15.00, 28.00, NOW());

-- ----------------------------------------------------------
-- 5. crop_batch（种植批次）
-- ----------------------------------------------------------
INSERT INTO crop_batch (batch_id, variety_id, plot_id, sowing_date, expected_harvest_date, current_stage, is_active) VALUES
(1, 1, 'PLOT-A01', '2026-01-10', '2026-06-10', '分蘖期', 1),
(2, 2, 'PLOT-B01', '2026-02-01', '2026-06-01', '拔节期', 1),
(3, 3, 'PLOT-C01', '2026-03-01', '2026-06-10', '苗期',   1);

-- ----------------------------------------------------------
-- 6. agri_task_rule（IoT 自动任务规则）
-- ----------------------------------------------------------
INSERT INTO agri_task_rule (rule_id, rule_name, sensor_type, min_val, max_val, auto_task_type, priority, is_enable) VALUES
(1, '低温灌溉警报', 'temperature', 0.00,  10.00, '灌溉保温', 2, 1),
(2, '高温预警',     'temperature', 35.00, 50.00, '遮荫降温', 1, 1),
(3, '土壤干旱预警', 'humidity',    0.00,  30.00, '补水灌溉', 2, 1);

-- ----------------------------------------------------------
-- 7. material_info（农资物料）
-- ----------------------------------------------------------
INSERT INTO material_info (material_id, name, category, price, stock_quantity, unit, update_time) VALUES
(1, '复合肥料',   '肥料', 120.00, 500.00, 'kg',  NOW()),
(2, '杀虫剂',     '农药', 85.00,  200.00, 'L',   NOW()),
(3, '灌溉水管',   '工具', 30.00,  100.00, '根',  NOW()),
(4, '薄膜地膜',   '材料', 15.00,  300.00, 'm²',  NOW());

-- ----------------------------------------------------------
-- 8. agri_task（12 条任务，覆盖全部状态）
--
--  status: 0=待分配 1=待接单 2=已接单/执行中 3=已完成 4=已逾期 5=已拒单
--  assign_by=2(farm_owner) 负责派单
--  assignee_id 指向 farmer1(3) 或 farmer2(4)
--  accept_by   同 assignee_id（农户接单）
--  reject_by   同 assignee_id（农户拒单）
-- ----------------------------------------------------------
INSERT INTO agri_task
  (task_id, batch_id, task_name, task_type, priority, plan_time, status,
   executor_id, assignee_id, assign_time, assign_by, assign_remark,
   accept_time, accept_by, reject_time, reject_by, reject_reason,
   create_by, create_time, update_by, update_time, version)
VALUES
-- status=0 待分配（3条）
(1,  1, '水稻田春季施肥',     '施肥', 1, '2026-03-05 08:00:00', 0,
  NULL, NULL, NULL, NULL, NULL,  NULL, NULL, NULL, NULL, NULL,  1, NOW(), NULL, NULL, 0),
(2,  2, '小麦病虫害检测',     '巡查', 0, '2026-03-06 09:00:00', 0,
  NULL, NULL, NULL, NULL, NULL,  NULL, NULL, NULL, NULL, NULL,  1, NOW(), NULL, NULL, 0),
(3,  3, '玉米地灌溉补水',     '灌溉', 2, '2026-03-07 07:00:00', 0,
  NULL, NULL, NULL, NULL, NULL,  NULL, NULL, NULL, NULL, NULL,  1, NOW(), NULL, NULL, 0),

-- status=1 待接单（2条，已派发给农户但未接单）
(4,  1, '水稻分蘖期除草',     '除草', 1, '2026-03-08 08:00:00', 1,
  NULL, 3, '2026-03-01 10:00:00', 2, '请尽快接单',  NULL, NULL, NULL, NULL, NULL,  1, NOW(), 2, '2026-03-01 10:00:00', 0),
(5,  2, '小麦追肥操作',       '施肥', 2, '2026-03-09 08:00:00', 1,
  NULL, 4, '2026-03-02 09:00:00', 2, '二号地块追肥', NULL, NULL, NULL, NULL, NULL, 1, NOW(), 2, '2026-03-02 09:00:00', 0),

-- status=2 已接单/执行中（3条）
(6,  1, '大田害虫防治喷药',   '植保', 1, '2026-03-10 07:00:00', 2,
  3, 3, '2026-03-03 10:00:00', 2, '优先处理',  '2026-03-03 11:00:00', 3, NULL, NULL, NULL,  1, NOW(), 3, '2026-03-03 11:00:00', 0),
(7,  2, '小麦田浇水作业',     '灌溉', 0, '2026-03-11 06:00:00', 2,
  4, 4, '2026-03-04 08:00:00', 2, '按计划执行', '2026-03-04 09:30:00', 4, NULL, NULL, NULL,  1, NOW(), 4, '2026-03-04 09:30:00', 0),
(8,  3, '玉米苗期追肥',       '施肥', 1, '2026-03-12 08:00:00', 2,
  3, 3, '2026-03-05 08:00:00', 2, '注意用量',  '2026-03-05 09:00:00', 3, NULL, NULL, NULL,  1, NOW(), 3, '2026-03-05 09:00:00', 0),

-- status=3 已完成（2条）
(9,  1, '水稻育苗移栽',       '移栽', 2, '2026-02-20 07:00:00', 3,
  3, 3, '2026-02-15 09:00:00', 2, '提前完成',  '2026-02-15 10:00:00', 3, NULL, NULL, NULL,  1, NOW(), 3, '2026-02-20 16:00:00', 0),
(10, 2, '小麦越冬期保温覆盖', '覆盖', 1, '2026-02-10 08:00:00', 3,
  4, 4, '2026-02-05 10:00:00', 2, '注意保温材料', '2026-02-05 11:00:00', 4, NULL, NULL, NULL, 1, NOW(), 4, '2026-02-12 15:00:00', 0),

-- status=4 已逾期（1条）
(11, 3, '玉米地排水沟疏通',   '维护', 0, '2026-02-01 08:00:00', 4,
  NULL, 4, '2026-01-28 10:00:00', 2, '已逾期',   NULL, NULL, NULL, NULL, NULL,  1, NOW(), 2, NOW(), 0),

-- status=5 已拒单（1条）
(12, 1, '冬季大田清理整地',   '整地', 1, '2026-01-25 08:00:00', 5,
  NULL, 3, '2026-01-20 09:00:00', 2, '清理整地', NULL, NULL, '2026-01-21 14:00:00', 3, '当日有其他任务无法完成',  1, NOW(), 3, '2026-01-21 14:00:00', 0);

-- ----------------------------------------------------------
-- 9. task_flow_log（流转日志，>=6 条）
-- ----------------------------------------------------------
INSERT INTO task_flow_log (log_id, task_id, action, from_status, to_status, operator_id, target_user_id, remark, trace_id, create_time) VALUES
-- 任务4: 派单 → 待接单
(1, 4,  'assign',  0, 1, 2, 3, '分配给农户李田一', UUID(), '2026-03-01 10:00:00'),
-- 任务5: 派单 → 待接单
(2, 5,  'assign',  0, 1, 2, 4, '分配给农户王田二', UUID(), '2026-03-02 09:00:00'),
-- 任务6: 派单 → 待接单 → 接单
(3, 6,  'assign',  0, 1, 2, 3, '派发防治任务',     UUID(), '2026-03-03 10:00:00'),
(4, 6,  'accept',  1, 2, 3, NULL, '农户确认接单',  UUID(), '2026-03-03 11:00:00'),
-- 任务7: 派单 → 接单
(5, 7,  'assign',  0, 1, 2, 4, '安排灌溉作业',     UUID(), '2026-03-04 08:00:00'),
(6, 7,  'accept',  1, 2, 4, NULL, '确认接单执行',  UUID(), '2026-03-04 09:30:00'),
-- 任务8: 派单 → 接单
(7, 8,  'assign',  0, 1, 2, 3, '追肥任务下达',     UUID(), '2026-03-05 08:00:00'),
(8, 8,  'accept',  1, 2, 3, NULL, '接单开始作业',  UUID(), '2026-03-05 09:00:00'),
-- 任务9: 完成流程
(9, 9,  'assign',  0, 1, 2, 3, '移栽任务',         UUID(), '2026-02-15 09:00:00'),
(10, 9, 'accept',  1, 2, 3, NULL, '接单',           UUID(), '2026-02-15 10:00:00'),
-- 任务12: 拒单
(11, 12, 'assign', 0, 1, 2, 3, '整地任务',         UUID(), '2026-01-20 09:00:00'),
(12, 12, 'reject', 1, 5, 3, NULL, '当日有其他任务无法完成', UUID(), '2026-01-21 14:00:00');

-- ----------------------------------------------------------
-- 10. task_execution_log（执行日志，>=6 条，对应 status>=2 的任务）
-- ----------------------------------------------------------
INSERT INTO task_execution_log (log_id, task_id, actual_start_time, actual_end_time, status_snapshot, photo_url, material_cost_json, problem_desc, create_time) VALUES
-- 任务6（执行中）
(1, 6,  '2026-03-03 11:30:00', NULL,                   2, NULL,
  '{"items":[{"material_id":2,"name":"杀虫剂","qty":5,"unit":"L"}]}',
  '正在喷药，部分区域已完成', NOW()),
-- 任务7（执行中）
(2, 7,  '2026-03-04 10:00:00', NULL,                   2, NULL,
  '{"items":[{"material_id":3,"name":"灌溉水管","qty":3,"unit":"根"}]}',
  NULL, NOW()),
-- 任务8（执行中）
(3, 8,  '2026-03-05 09:30:00', NULL,                   2, NULL,
  '{"items":[{"material_id":1,"name":"复合肥料","qty":20,"unit":"kg"}]}',
  '追肥进行中', NOW()),
-- 任务9（已完成）
(4, 9,  '2026-02-15 10:30:00', '2026-02-20 15:00:00',  3, NULL,
  '{"items":[{"material_id":4,"name":"薄膜地膜","qty":50,"unit":"m²"}]}',
  NULL, NOW()),
-- 任务10（已完成）
(5, 10, '2026-02-05 11:30:00', '2026-02-12 14:00:00',  3, NULL,
  '{"items":[{"material_id":4,"name":"薄膜地膜","qty":80,"unit":"m²"}]}',
  NULL, NOW()),
-- 任务11（已逾期，仅有开始记录）
(6, 11, '2026-01-29 08:00:00', NULL,                   4, NULL,
  NULL, '排水沟疏通超时未完成，已标记逾期', NOW());

-- ----------------------------------------------------------
-- 11. material_inout_log（农资出入库日志）
-- ----------------------------------------------------------
INSERT INTO material_inout_log (log_id, material_id, type, quantity, related_task_id, remark, create_time) VALUES
(1, 1, 1, 500.00, NULL, '初始入库-复合肥',   NOW()),
(2, 2, 1, 200.00, NULL, '初始入库-杀虫剂',   NOW()),
(3, 3, 1, 100.00, NULL, '初始入库-水管',      NOW()),
(4, 4, 1, 300.00, NULL, '初始入库-地膜',      NOW()),
(5, 2, 2,   5.00, 6,    '任务6领用杀虫剂',   NOW()),
(6, 1, 2,  20.00, 8,    '任务8领用复合肥',   NOW());

-- ----------------------------------------------------------
-- 12. growth_stage_log（作物生长日志）
-- ----------------------------------------------------------
INSERT INTO growth_stage_log (log_id, batch_id, stage_name, log_date, image_url, description) VALUES
(1, 1, '播种期', '2026-01-10 08:00:00', NULL, '完成水稻播种'),
(2, 1, '分蘖期', '2026-02-15 08:00:00', NULL, '进入分蘖期，长势良好'),
(3, 2, '出苗期', '2026-02-05 08:00:00', NULL, '小麦出苗整齐'),
(4, 2, '拔节期', '2026-03-01 08:00:00', NULL, '进入拔节期'),
(5, 3, '出苗期', '2026-03-05 08:00:00', NULL, '玉米出苗率92%');

-- ----------------------------------------------------------
-- 13. iot_sensor_data（传感器数据样例）
-- ----------------------------------------------------------
INSERT INTO iot_sensor_data (data_id, plot_id, sensor_type, value, unit, create_time) VALUES
(1,  'PLOT-A01', 'temperature', 18.50, '℃',  DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2,  'PLOT-A01', 'humidity',    72.30, '%',   DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3,  'PLOT-B01', 'temperature', 16.80, '℃',  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4,  'PLOT-B01', 'humidity',    65.40, '%',   DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5,  'PLOT-C01', 'temperature', 20.10, '℃',  NOW()),
(6,  'PLOT-C01', 'humidity',    58.90, '%',   NOW());
