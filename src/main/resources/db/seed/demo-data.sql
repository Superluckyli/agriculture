-- =================================================================
-- 演示数据 (用户/农田/批次/任务/物资/采购 — 仅开发/演示环境)
-- 生产环境不应执行此文件
-- =================================================================

-- 1. sys_user (BCrypt(123456))
INSERT IGNORE INTO sys_user (user_id, username, password, real_name, phone, dept_name, status, create_time) VALUES
(1, 'admin',      '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '系统管理员', '13800000001', '技术部',   1, NOW()),
(2, 'farm_owner', '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '李场长',     '13800000002', '农场管理部', 1, NOW()),
(3, 'technician1','$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '张技术员',   '13800000003', '技术组',   1, NOW()),
(4, 'worker1',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '王工人',     '13800000004', '作业一组', 1, NOW()),
(5, 'worker2',    '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '赵工人',     '13800000005', '作业二组', 1, NOW()),
(6, 'demo',       '$2a$10$dBOv.8AE0nH4iBICIHQWzOt/u/sfi8pXy1dYiASbbGu/23FEmhRcm', '演示账号',   '13800000006', '演示部门', 1, NOW());

-- 2. sys_user_role
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 4), (5, 4), (6, 2);

-- 3. agri_farmland
INSERT IGNORE INTO agri_farmland (id, tenant_id, org_id, name, code, location, area, manager_user_id, crop_adapt_note, status, created_at) VALUES
(1, 1, 1, '1号水稻田',   'FL-A01', '东区-靠河',   15.50, 2, '适合水稻、莲藕等水生作物', 1, NOW()),
(2, 1, 1, '2号旱地',     'FL-B01', '北区-坡地',   20.00, 2, '适合小麦、大豆等旱地作物', 1, NOW()),
(3, 1, 1, '3号温室大棚', 'FL-C01', '南区-大棚区', 5.00,  3, '可种植蔬菜、瓜果',        1, NOW());

-- 4. supplier_info
INSERT IGNORE INTO supplier_info (id, tenant_id, name, contact_name, phone, address, remark, status, created_at) VALUES
(1, 1, '绿丰农资有限公司', '陈经理', '13900001111', '市农资批发市场A区12号', '长期合作，肥料/农药主供',   1, NOW()),
(2, 1, '兴农工具商行',     '刘老板', '13900002222', '市农资批发市场B区5号',  '工具/管材/地膜专供',       1, NOW());

-- 5. material_info
INSERT IGNORE INTO material_info (material_id, tenant_id, org_id, name, category, specification, unit, current_stock, safe_threshold, suggest_purchase_qty, supplier_id, unit_price, status, version, created_at) VALUES
(1, 1, 1, '复合肥料', '肥料', '45%含量/50kg袋装', 'kg',  500.00, 100.00, 200.00, 1, 2.40,  1, 0, NOW()),
(2, 1, 1, '杀虫剂',   '农药', '高效氯氰菊酯/1L瓶', 'L',  200.00, 50.00,  100.00, 1, 85.00, 1, 0, NOW()),
(3, 1, 1, '灌溉水管', '工具', 'PE管/DN25/4米',     '根', 100.00, 20.00,  50.00,  2, 30.00, 1, 0, NOW()),
(4, 1, 1, '薄膜地膜', '材料', '0.01mm厚/1米宽',    'm²', 300.00, 50.00,  100.00, 2, 1.50,  1, 0, NOW());

-- (省略 agri_crop_batch / agri_task / material_stock_log 等大量演示数据，完整版见 V1.1__seed_data.sql)
