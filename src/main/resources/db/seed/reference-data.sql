-- =================================================================
-- 引用数据 (角色/菜单/角色菜单 — 系统必需，不可删除)
-- 用途: 新环境初始化时使用，或 Flyway afterMigrate 回调
-- =================================================================

-- 1. sys_role
INSERT IGNORE INTO sys_role (role_id, role_name, role_key, create_time) VALUES
(1, '超级管理员', 'ADMIN',       NOW()),
(2, '场长',       'FARM_OWNER',  NOW()),
(3, '技术员',     'TECHNICIAN',  NOW()),
(4, '工人',       'WORKER',      NOW());

-- 2. sys_menu
INSERT IGNORE INTO sys_menu (menu_id, parent_id, menu_name, path, perms, type, order_num) VALUES
(1,  0, '仪表盘',   '/dashboard',        NULL, 1, 1),
(2,  0, '种植管理', NULL,                 NULL, 1, 2),
(3,  0, '任务管理', NULL,                 NULL, 1, 3),
(4,  0, '物资管理', NULL,                 NULL, 1, 4),
(5,  0, '采购管理', NULL,                 NULL, 1, 5),
(6,  0, '统计报表', '/report/analytics',  NULL, 1, 6),
(7,  0, '系统设置', NULL,                 NULL, 1, 7),
(20, 2, '农田管理', '/crop/farmland',     NULL, 2, 1),
(21, 2, '批次管理', '/crop/batch',        NULL, 2, 2),
(22, 2, '品种管理', '/crop/variety',      NULL, 2, 3),
(30, 3, '任务调度', '/task/list',         NULL, 2, 1),
(31, 3, '我的任务', '/task/my',           NULL, 2, 2),
(32, 3, '执行日志', '/task/log',          NULL, 2, 3),
(40, 4, '物资台账', '/material/inventory', NULL, 2, 1),
(41, 4, '库存流水', '/material/stock-log', NULL, 2, 2),
(50, 5, '采购单',   '/purchase/order',     NULL, 2, 1),
(51, 5, '供应商',   '/purchase/supplier',  NULL, 2, 2),
(70, 7, '用户管理', '/system/user',        NULL, 2, 1),
(71, 7, '角色管理', '/system/role',        NULL, 2, 2);

-- 3. sys_role_menu
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
(1,20),(1,21),(1,22),(1,30),(1,31),(1,32),(1,40),(1,41),(1,50),(1,51),(1,70),(1,71),
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),
(2,20),(2,21),(2,22),(2,30),(2,32),(2,40),(2,41),(2,50),(2,51),(2,70),(2,71),
(3,1),(3,2),(3,3),(3,4),(3,6),
(3,20),(3,21),(3,22),(3,30),(3,32),(3,40),(3,41),
(4,1),(4,3),(4,31);

-- 4. base_crop_variety
INSERT IGNORE INTO base_crop_variety (variety_id, crop_name, growth_cycle_days, ideal_humidity_min, ideal_humidity_max, ideal_temp_min, ideal_temp_max, create_time) VALUES
(1, '水稻', 150, 60.00, 90.00, 15.00, 30.00, NOW()),
(2, '小麦', 120, 40.00, 70.00, 10.00, 25.00, NOW()),
(3, '玉米', 100, 50.00, 80.00, 18.00, 35.00, NOW()),
(4, '大豆',  90, 55.00, 75.00, 15.00, 28.00, NOW());

-- 5. agri_task_rule
INSERT IGNORE INTO agri_task_rule (rule_id, rule_name, sensor_type, min_val, max_val, auto_task_type, priority, is_enable) VALUES
(1, '低温灌溉警报', 'temperature', 0.00,  10.00, '灌溉保温', 2, 1),
(2, '高温预警',     'temperature', 35.00, 50.00, '遮荫降温', 1, 1),
(3, '土壤干旱预警', 'humidity',    0.00,  30.00, '补水灌溉', 2, 1);
