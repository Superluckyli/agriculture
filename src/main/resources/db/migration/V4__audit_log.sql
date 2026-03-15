-- =================================================================
-- Flyway V4: 操作审计日志表
-- =================================================================

CREATE TABLE sys_audit_log (
    log_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT,
    username  VARCHAR(50),
    module    VARCHAR(50)  NOT NULL COMMENT '模块名称',
    action    VARCHAR(50)  NOT NULL COMMENT '操作类型 (LOGIN/CREATE/UPDATE/DELETE/ASSIGN)',
    target    VARCHAR(200) COMMENT '操作对象描述',
    ip        VARCHAR(45),
    method    VARCHAR(10)  COMMENT 'HTTP 方法',
    uri       VARCHAR(200) COMMENT '请求路径',
    status    INT          COMMENT 'HTTP 响应状态码',
    duration  BIGINT       COMMENT '耗时(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user_time ON sys_audit_log (user_id, create_time);
CREATE INDEX idx_audit_module_action ON sys_audit_log (module, action);
