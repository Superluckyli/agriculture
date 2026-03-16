-- =================================================================
-- Flyway V6: 修正可能遗留的旧角色 key
--
-- 背景: V1.1 使用 INSERT IGNORE，若数据库在迁移前已有旧数据
-- (如 role_key='FARMER' 而非 'TECHNICIAN')，INSERT IGNORE 不会覆盖。
-- 此迁移强制将 role_id=3 的 role_key 统一为 'TECHNICIAN'。
-- =================================================================

UPDATE sys_role
SET role_key  = 'TECHNICIAN',
    role_name = '技术员'
WHERE role_id = 3
  AND role_key != 'TECHNICIAN';
