-- =============================================================
-- data.sql · V1 种子数据（与 schema.sql 严格对应）
-- 插入顺序按外键依赖：
--   sys_role → sys_user → sys_user_role → sys_menu → sys_role_menu
--   → base_crop_variety → agri_farmland → supplier_info
--   → agri_crop_batch → material_info
--   → agri_task → agri_task_material → agri_task_log
--   → material_stock_log → purchase_order → purchase_order_item
--   → agri_task_rule → iot_sensor_data
-- 密码统一为 123456 的 BCrypt 散列
-- 角色模型: ADMIN / FARM_OWNER / TECHNICIAN / WORKER (共 4 个)
-- =============================================================

-- ----------------------------------------------------------
-- 0. 幂等清理（保证重启不报重复键）
-- ----------------------------------------------------------
DELETE FROM payment_record;
DELETE FROM purchase_order_item;
DELETE FROM purchase_order;
DELETE FROM material_stock_log;
DELETE FROM agri_task_log;
DELETE FROM agri_task_material;
DELETE FROM agri_task;
DELETE FROM agri_crop_batch;
DELETE FROM material_info;
DELETE FROM supplier_info;
DELETE FROM agri_farmland;
DELETE FROM base_crop_variety;
DELETE FROM iot_sensor_data;
DELETE FROM agri_task_rule;
DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_menu;
DELETE FROM sys_user;
DELETE FROM sys_role;

-- ----------------------------------------------------------
-- 1. sys_role（4 个业务角色，不多不少）
-- ----------------------------------------------------------
INSERT INTO sys_role (role_id, role_name, role_key, create_time) VALUES
(1, '超级管理员', 'ADMIN',       NOW()),
(2, '场长',       'FARM_OWNER',  NOW()),
(3, '技术员',     'TECHNICIAN',  NOW()),
(4, '工人',       'WORKER',      NOW());

-- ----------------------------------------------------------
-- 2. sys_user（6 个种子账号）
--    BCrypt(123456) = $2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm
-- ----------------------------------------------------------
INSERT INTO sys_user (user_id, username, password, real_name, phone, dept_name, status, create_time) VALUES
(1, 'admin',      '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '系统管理员', '13800000001', '技术部',   1, NOW()),
(2, 'farm_owner', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '李场长',     '13800000002', '农场管理部', 1, NOW()),
(3, 'tech1',      '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '张技术员',   '13800000003', '技术组',   1, NOW()),
(4, 'worker1',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '王工人',     '13800000004', '作业一组', 1, NOW()),
(5, 'worker2',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '赵工人',     '13800000005', '作业二组', 1, NOW()),
(6, 'demo',       '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '演示账号',   '13800000006', '演示部门', 1, NOW());

-- ----------------------------------------------------------
-- 3. sys_user_role（用户 → 角色映射）
-- ----------------------------------------------------------
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin      → ADMIN
(2, 2),  -- farm_owner → FARM_OWNER
(3, 3),  -- tech1      → TECHNICIAN
(4, 4),  -- worker1    → WORKER
(5, 4),  -- worker2    → WORKER
(6, 2);  -- demo       → FARM_OWNER

-- ----------------------------------------------------------
-- 4. sys_menu（V1 菜单树）
-- ----------------------------------------------------------
INSERT INTO sys_menu (menu_id, parent_id, menu_name, path, perms, type, order_num) VALUES
-- 一级菜单
(1,  0, '仪表盘',   '/dashboard',        NULL, 1, 1),
(2,  0, '种植管理', NULL,                 NULL, 1, 2),
(3,  0, '任务管理', NULL,                 NULL, 1, 3),
(4,  0, '物资管理', NULL,                 NULL, 1, 4),
(5,  0, '采购管理', NULL,                 NULL, 1, 5),
(6,  0, '统计报表', '/report/analytics',  NULL, 1, 6),
(7,  0, '系统设置', NULL,                 NULL, 1, 7),
-- 种植管理子菜单
(20, 2, '农田管理', '/crop/farmland',     NULL, 2, 1),
(21, 2, '批次管理', '/crop/batch',        NULL, 2, 2),
(22, 2, '品种管理', '/crop/variety',      NULL, 2, 3),
-- 任务管理子菜单
(30, 3, '任务调度', '/task/list',         NULL, 2, 1),
(31, 3, '我的任务', '/task/my',           NULL, 2, 2),
(32, 3, '执行日志', '/task/log',          NULL, 2, 3),
-- 物资管理子菜单
(40, 4, '物资台账', '/material/inventory', NULL, 2, 1),
(41, 4, '库存流水', '/material/stock-log', NULL, 2, 2),
-- 采购管理子菜单
(50, 5, '采购单',   '/purchase/order',     NULL, 2, 1),
(51, 5, '供应商',   '/purchase/supplier',  NULL, 2, 2),
-- 系统设置子菜单
(70, 7, '用户管理', '/system/user',        NULL, 2, 1),
(71, 7, '角色管理', '/system/role',        NULL, 2, 2);

-- ----------------------------------------------------------
-- 5. sys_role_menu（角色 → 菜单权限）
-- ----------------------------------------------------------
-- ADMIN: 全部菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
(1,20),(1,21),(1,22),(1,30),(1,31),(1,32),(1,40),(1,41),(1,50),(1,51),(1,70),(1,71);

-- FARM_OWNER: 仪表盘+种植+任务调度+物资+采购+统计+系统设置
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),
(2,20),(2,21),(2,22),(2,30),(2,32),(2,40),(2,41),(2,50),(2,51),(2,70),(2,71);

-- TECHNICIAN: 仪表盘+种植+任务调度+物资+统计
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3,1),(3,2),(3,3),(3,4),(3,6),
(3,20),(3,21),(3,22),(3,30),(3,32),(3,40),(3,41);

-- WORKER: 仪表盘+我的任务
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4,1),(4,3),(4,31);

-- ----------------------------------------------------------
-- 6. base_crop_variety（作物品种）
-- ----------------------------------------------------------
INSERT INTO base_crop_variety (variety_id, crop_name, growth_cycle_days, ideal_humidity_min, ideal_humidity_max, ideal_temp_min, ideal_temp_max, create_time) VALUES
(1, '水稻', 150, 60.00, 90.00, 15.00, 30.00, NOW()),
(2, '小麦', 120, 40.00, 70.00, 10.00, 25.00, NOW()),
(3, '玉米', 100, 50.00, 80.00, 18.00, 35.00, NOW()),
(4, '大豆',  90, 55.00, 75.00, 15.00, 28.00, NOW());

-- ----------------------------------------------------------
-- 7. agri_farmland（3 块农田）
-- ----------------------------------------------------------
INSERT INTO agri_farmland (id, tenant_id, org_id, name, code, location, area, manager_user_id, crop_adapt_note, status, created_at) VALUES
(1, 1, 1, '1号水稻田',   'FL-A01', '东区-靠河',   15.50, 2, '适合水稻、莲藕等水生作物', 1, NOW()),
(2, 1, 1, '2号旱地',     'FL-B01', '北区-坡地',   20.00, 2, '适合小麦、大豆等旱地作物', 1, NOW()),
(3, 1, 1, '3号温室大棚', 'FL-C01', '南区-大棚区', 5.00,  3, '可种植蔬菜、瓜果',        1, NOW());

-- ----------------------------------------------------------
-- 8. supplier_info（2 个供应商）
-- ----------------------------------------------------------
INSERT INTO supplier_info (id, tenant_id, name, contact_name, phone, address, remark, status, created_at) VALUES
(1, 1, '绿丰农资有限公司', '陈经理', '13900001111', '市农资批发市场A区12号', '长期合作，肥料/农药主供',   1, NOW()),
(2, 1, '兴农工具商行',     '刘老板', '13900002222', '市农资批发市场B区5号',  '工具/管材/地膜专供',       1, NOW());

-- ----------------------------------------------------------
-- 9. agri_crop_batch（5 个批次，覆盖多种生命周期状态）
-- ----------------------------------------------------------
INSERT INTO agri_crop_batch (id, tenant_id, org_id, batch_no, farmland_id, variety_id, crop_variety, planting_date, estimated_harvest_date, actual_harvest_date, stage, status, owner_user_id, target_output, actual_output, abandon_reason, remark, created_at) VALUES
(1, 1, 1, 'B-2026-001', 1, 1, '水稻', '2026-01-10', '2026-06-10', NULL,          '分蘖期', 'in_progress', 2, 8000.00, NULL,    NULL, '春季水稻主批次',       NOW()),
(2, 1, 1, 'B-2026-002', 2, 2, '小麦', '2026-02-01', '2026-06-01', NULL,          '拔节期', 'in_progress', 2, 6000.00, NULL,    NULL, '冬小麦批次',           NOW()),
(3, 1, 1, 'B-2026-003', 3, 3, '玉米', '2026-03-01', '2026-06-10', NULL,          '苗期',   'in_progress', 2, 5000.00, NULL,    NULL, '温室玉米实验批',       NOW()),
(4, 1, 1, 'B-2025-010', 1, 1, '水稻', '2025-04-01', '2025-09-15', '2025-09-10', '采收完毕', 'harvested', 2, 7500.00, 7200.00, NULL, '2025年春季水稻（已收）', NOW()),
(5, 1, 1, 'B-2025-011', 2, 4, '大豆', '2025-06-01', '2025-09-01', NULL,          NULL,     'abandoned',   2, 3000.00, NULL,    '虫害严重，弃种', '2025年大豆（已废弃）', NOW());

-- ----------------------------------------------------------
-- 10. material_info（4 种物资，含安全阈值）
--     同时填充新旧列，确保旧 Java 代码兼容
-- ----------------------------------------------------------
INSERT INTO material_info (material_id, tenant_id, org_id, name, category, specification, unit, current_stock, safe_threshold, suggest_purchase_qty, supplier_id, unit_price, status, version, price, stock_quantity, update_time, created_at) VALUES
(1, 1, 1, '复合肥料', '肥料', '45%含量/50kg袋装', 'kg',  500.00, 100.00, 200.00, 1, 2.40,  1, 0, 120.00, 500.00, NOW(), NOW()),
(2, 1, 1, '杀虫剂',   '农药', '高效氯氰菊酯/1L瓶', 'L',  200.00, 50.00,  100.00, 1, 85.00, 1, 0, 85.00,  200.00, NOW(), NOW()),
(3, 1, 1, '灌溉水管', '工具', 'PE管/DN25/4米',     '根', 100.00, 20.00,  50.00,  2, 30.00, 1, 0, 30.00,  100.00, NOW(), NOW()),
(4, 1, 1, '薄膜地膜', '材料', '0.01mm厚/1米宽',    'm²', 300.00, 50.00,  100.00, 2, 1.50,  1, 0, 15.00,  300.00, NOW(), NOW());

-- ----------------------------------------------------------
-- 11. agri_task（12 条任务，覆盖 9 种 V1 状态）
--     status_v2: V1 新状态 (VARCHAR)
--     status:    旧状态 (INT, 兼容现有 Java 代码)
--     assign_by=2(farm_owner), reviewer=3(tech1)
--     assignee_id 指向 worker1(4) 或 worker2(5)
-- ----------------------------------------------------------
INSERT INTO agri_task
  (task_id, tenant_id, org_id, batch_id, task_no, task_name, task_type, task_source, risk_level, need_review, priority,
   plan_time, deadline_at, status_v2, status,
   executor_id, assignee_id, assign_time, assign_by, assign_remark, reviewer_user_id,
   accept_time, accept_by, completed_at,
   reject_time, reject_by, reject_reason, reject_reason_type,
   suspend_reason, cancel_reason, suggest_action, precaution_note,
   create_by, create_time, update_by, update_time, version)
VALUES
-- pending_review: 高风险任务等待技术员复核 (2 条)
(1, 1, 1, 1, 'T-2026-001', '水稻田农药喷洒',   '植保', 'manual', 'HIGH', 1, 2,
  '2026-03-15 08:00:00', '2026-03-16 18:00:00', 'pending_review', 0,
  NULL, NULL, NULL, NULL, NULL, 3,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '使用高效氯氰菊酯进行全田喷洒', '注意佩戴防护装备，避开风大时段',
  2, NOW(), NULL, NULL, 0),

(2, 1, 1, 3, 'T-2026-002', '温室大面积消毒处理', '消毒', 'manual', 'HIGH', 1, 2,
  '2026-03-18 07:00:00', '2026-03-19 18:00:00', 'pending_review', 0,
  NULL, NULL, NULL, NULL, NULL, 3,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '全棚消毒，需关闭通风24小时', '消毒期间禁止人员进入',
  2, NOW(), NULL, NULL, 0),

-- pending_accept: 已派发等待工人接单 (2 条)
(3, 1, 1, 1, 'T-2026-003', '水稻分蘖期除草', '除草', 'manual', 'LOW', 0, 1,
  '2026-03-08 08:00:00', '2026-03-10 18:00:00', 'pending_accept', 1,
  NULL, 4, '2026-03-06 10:00:00', 2, '请尽快接单', NULL,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '人工除草，注意保护稻苗', NULL,
  2, NOW(), 2, '2026-03-06 10:00:00', 0),

(4, 1, 1, 2, 'T-2026-004', '小麦追肥操作', '施肥', 'manual', 'MEDIUM', 0, 1,
  '2026-03-09 08:00:00', '2026-03-11 18:00:00', 'pending_accept', 1,
  NULL, 5, '2026-03-07 09:00:00', 2, '二号旱地追肥', NULL,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '按每亩15kg追施复合肥', '注意肥料浓度，避免烧苗',
  2, NOW(), 2, '2026-03-07 09:00:00', 0),

-- in_progress: 执行中 (2 条)
(5, 1, 1, 1, 'T-2026-005', '大田害虫防治',   '植保', 'manual', 'MEDIUM', 0, 1,
  '2026-03-10 07:00:00', '2026-03-12 18:00:00', 'in_progress', 2,
  4, 4, '2026-03-08 10:00:00', 2, '优先处理', NULL,
  '2026-03-08 11:00:00', 4, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '局部喷洒杀虫剂', '配合防护服使用',
  2, NOW(), 4, '2026-03-08 11:00:00', 0),

(6, 1, 1, 2, 'T-2026-006', '小麦田浇水作业', '灌溉', 'manual', 'LOW', 0, 0,
  '2026-03-11 06:00:00', '2026-03-13 18:00:00', 'in_progress', 2,
  5, 5, '2026-03-09 08:00:00', 2, '按计划执行', NULL,
  '2026-03-09 09:30:00', 5, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, '全田灌溉', NULL,
  2, NOW(), 5, '2026-03-09 09:30:00', 0),

-- completed: 已完成 (2 条)
(7, 1, 1, 1, 'T-2026-007', '水稻育苗移栽', '移栽', 'manual', 'LOW', 0, 2,
  '2026-02-20 07:00:00', '2026-02-25 18:00:00', 'completed', 3,
  4, 4, '2026-02-15 09:00:00', 2, '提前完成', NULL,
  '2026-02-15 10:00:00', 4, '2026-02-20 15:00:00',  NULL, NULL, NULL, NULL,
  NULL, NULL, '移栽至大田', NULL,
  2, NOW(), 4, '2026-02-20 15:00:00', 0),

(8, 1, 1, 2, 'T-2026-008', '小麦越冬期覆盖', '覆盖', 'manual', 'LOW', 0, 1,
  '2026-02-10 08:00:00', '2026-02-15 18:00:00', 'completed', 3,
  5, 5, '2026-02-05 10:00:00', 2, '注意保温材料', NULL,
  '2026-02-05 11:00:00', 5, '2026-02-12 14:00:00',  NULL, NULL, NULL, NULL,
  NULL, NULL, '覆盖地膜保温', NULL,
  2, NOW(), 5, '2026-02-12 14:00:00', 0),

-- rejected_reassign: 因人员原因拒绝待改派 (1 条)
(9, 1, 1, 1, 'T-2026-009', '稻田排水检查', '巡查', 'manual', 'LOW', 0, 0,
  '2026-03-05 08:00:00', '2026-03-07 18:00:00', 'rejected_reassign', 5,
  NULL, 4, '2026-03-03 09:00:00', 2, '检查排水', NULL,
  NULL, NULL, NULL,  '2026-03-03 14:00:00', 4, '当日有更高优先级任务', 'personnel',
  NULL, NULL, NULL, NULL,
  2, NOW(), 4, '2026-03-03 14:00:00', 0),

-- suspended: 因资源原因挂起 (1 条)
(10, 1, 1, 3, 'T-2026-010', '温室追肥作业', '施肥', 'manual', 'MEDIUM', 0, 1,
  '2026-03-12 08:00:00', '2026-03-14 18:00:00', 'suspended', 1,
  NULL, 5, '2026-03-10 08:00:00', 2, '温室追肥', NULL,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  '复合肥库存不足，等待补货', NULL, '追施复合肥20kg', '注意温室通风',
  2, NOW(), 5, '2026-03-11 10:00:00', 0),

-- overdue: 已超时 (1 条)
(11, 1, 1, 2, 'T-2026-011', '旱地排水沟疏通', '维护', 'manual', 'LOW', 0, 0,
  '2026-02-01 08:00:00', '2026-02-05 18:00:00', 'overdue', 4,
  NULL, 5, '2026-01-28 10:00:00', 2, '已逾期', NULL,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, NULL, NULL, NULL,
  2, NOW(), 2, NOW(), 0),

-- cancelled: 已取消 (1 条)
(12, 1, 1, 1, 'T-2026-012', '冬季大田清理（取消）', '整地', 'manual', 'LOW', 0, 1,
  '2026-01-25 08:00:00', '2026-01-30 18:00:00', 'cancelled', 0,
  NULL, NULL, NULL, NULL, NULL, NULL,
  NULL, NULL, NULL,  NULL, NULL, NULL, NULL,
  NULL, '批次调整，该任务不再需要', NULL, NULL,
  2, NOW(), 2, NOW(), 0);

-- ----------------------------------------------------------
-- 12. agri_task_material（已完成任务的耗材记录）
-- ----------------------------------------------------------
INSERT INTO agri_task_material (id, task_id, material_id, suggested_qty, actual_qty, unit_price, deviation_reason, created_at) VALUES
-- 任务5(执行中): 建议用量已填，实际用量待提交
(1, 5, 2, 5.00,  NULL,  85.00, NULL, NOW()),
-- 任务6(执行中): 建议用量已填
(2, 6, 3, 3.00,  NULL,  30.00, NULL, NOW()),
-- 任务7(已完成): 建议50m²地膜，实际用了55m²
(3, 7, 4, 50.00, 55.00, 1.50,  '实际面积略大于预估', NOW()),
-- 任务8(已完成): 建议80m²地膜，实际80m²
(4, 8, 4, 80.00, 80.00, 1.50,  NULL, NOW());

-- ----------------------------------------------------------
-- 13. agri_task_log（任务流转日志）
-- ----------------------------------------------------------
INSERT INTO agri_task_log (id, task_id, batch_id, action, from_status, to_status, operator_id, target_user_id, growth_note, image_urls, abnormal_note, remark, trace_id, created_at) VALUES
-- 任务3: 创建 → 派单
(1,  3, 1, 'create',  NULL,              'pending_accept',   2, NULL,  NULL, NULL, NULL, '创建除草任务',     UUID(), NOW()),
(2,  3, 1, 'assign',  'pending_accept',  'pending_accept',   2, 4,    NULL, NULL, NULL, '派发给王工人',     UUID(), '2026-03-06 10:00:00'),
-- 任务5: 派单 → 接单
(3,  5, 1, 'assign',  'pending_accept',  'pending_accept',   2, 4,    NULL, NULL, NULL, '派发防治任务',     UUID(), '2026-03-08 10:00:00'),
(4,  5, 1, 'accept',  'pending_accept',  'in_progress',      4, NULL,  NULL, NULL, NULL, '确认接单',         UUID(), '2026-03-08 11:00:00'),
-- 任务6: 派单 → 接单
(5,  6, 2, 'assign',  'pending_accept',  'pending_accept',   2, 5,    NULL, NULL, NULL, '安排灌溉',         UUID(), '2026-03-09 08:00:00'),
(6,  6, 2, 'accept',  'pending_accept',  'in_progress',      5, NULL,  NULL, NULL, NULL, '确认接单执行',     UUID(), '2026-03-09 09:30:00'),
-- 任务7: 完整流程含执行日志
(7,  7, 1, 'assign',  'pending_accept',  'pending_accept',   2, 4,    NULL, NULL, NULL, '移栽任务',         UUID(), '2026-02-15 09:00:00'),
(8,  7, 1, 'accept',  'pending_accept',  'in_progress',      4, NULL,  NULL, NULL, NULL, '接单',             UUID(), '2026-02-15 10:00:00'),
(9,  7, 1, 'execute_log', 'in_progress', 'in_progress',      4, NULL,  '移栽顺利完成，苗株成活率95%', NULL, NULL, '执行日志', UUID(), '2026-02-20 14:00:00'),
(10, 7, 1, 'complete','in_progress',     'completed',         4, NULL,  NULL, NULL, NULL, '任务完成',         UUID(), '2026-02-20 15:00:00'),
-- 任务9: 拒绝流程
(11, 9, 1, 'assign',  'pending_accept',  'pending_accept',   2, 4,    NULL, NULL, NULL, '排水检查',         UUID(), '2026-03-03 09:00:00'),
(12, 9, 1, 'reject',  'pending_accept',  'rejected_reassign', 4, NULL, NULL, NULL, NULL, '当日有更高优先级任务', UUID(), '2026-03-03 14:00:00'),
-- 任务10: 挂起
(13, 10, 3, 'assign', 'pending_accept',  'pending_accept',   2, 5,    NULL, NULL, NULL, '温室追肥',         UUID(), '2026-03-10 08:00:00'),
(14, 10, 3, 'suspend','pending_accept',  'suspended',         5, NULL, NULL, NULL, '复合肥库存不足', '等待补货后恢复', UUID(), '2026-03-11 10:00:00');

-- ----------------------------------------------------------
-- 14. material_stock_log（库存流水：初始入库 + 任务出库）
-- ----------------------------------------------------------
INSERT INTO material_stock_log (id, material_id, change_type, qty, before_stock, after_stock, related_type, related_id, operator_id, remark, created_at) VALUES
-- 初始入库
(1, 1, 'IN', 500.00, 0.00,   500.00, 'manual',   NULL, 1, '初始入库-复合肥',   NOW()),
(2, 2, 'IN', 200.00, 0.00,   200.00, 'manual',   NULL, 1, '初始入库-杀虫剂',   NOW()),
(3, 3, 'IN', 100.00, 0.00,   100.00, 'manual',   NULL, 1, '初始入库-水管',     NOW()),
(4, 4, 'IN', 300.00, 0.00,   300.00, 'manual',   NULL, 1, '初始入库-地膜',     NOW()),
-- 任务7完成后出库 (55m²地膜)
(5, 4, 'OUT', 55.00, 300.00, 245.00, 'task',      7,    4, '任务T-2026-007移栽用膜', NOW()),
-- 任务8完成后出库 (80m²地膜)
(6, 4, 'OUT', 80.00, 245.00, 165.00, 'task',      8,    5, '任务T-2026-008覆盖用膜', NOW());

-- ----------------------------------------------------------
-- 15. purchase_order + items（1 条草稿：地膜低库存触发）
-- ----------------------------------------------------------
INSERT INTO purchase_order (id, tenant_id, org_id, order_no, status, supplier_id, total_amount, pay_method, remark, created_by, confirmed_by, created_at) VALUES
(1, 1, 1, 'PO-20260312-001', 'draft', 2, 150.00, NULL, '地膜库存低于阈值(165<50不触发，此为演示草稿)', NULL, NULL, NOW());

INSERT INTO purchase_order_item (id, purchase_order_id, material_id, purchase_qty, receive_qty, unit_price, line_amount, remark) VALUES
(1, 1, 4, 100.00, 0, 1.50, 150.00, '补充薄膜地膜库存');

-- ----------------------------------------------------------
-- 16. agri_task_rule（IoT 自动规则，V1.5 保留）
-- ----------------------------------------------------------
INSERT INTO agri_task_rule (rule_id, rule_name, sensor_type, min_val, max_val, auto_task_type, priority, is_enable) VALUES
(1, '低温灌溉警报', 'temperature', 0.00,  10.00, '灌溉保温', 2, 1),
(2, '高温预警',     'temperature', 35.00, 50.00, '遮荫降温', 1, 1),
(3, '土壤干旱预警', 'humidity',    0.00,  30.00, '补水灌溉', 2, 1);

-- ----------------------------------------------------------
-- 17. iot_sensor_data（传感器数据样例，V1.5 保留）
-- ----------------------------------------------------------
INSERT INTO iot_sensor_data (data_id, plot_id, sensor_type, value, unit, create_time) VALUES
(1, 'FL-A01', 'temperature', 18.50, '℃', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 'FL-A01', 'humidity',    72.30, '%',  DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'FL-B01', 'temperature', 16.80, '℃', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4, 'FL-B01', 'humidity',    65.40, '%',  DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 'FL-C01', 'temperature', 20.10, '℃', NOW()),
(6, 'FL-C01', 'humidity',    58.90, '%',  NOW());
