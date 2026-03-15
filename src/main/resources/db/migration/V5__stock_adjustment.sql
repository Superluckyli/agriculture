-- =================================================================
-- Flyway V5: 库存调整审核表
-- =================================================================

CREATE TABLE stock_adjustment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT       NOT NULL,
    adjust_type VARCHAR(32)  NOT NULL COMMENT 'INCREASE/DECREASE/SET',
    qty         DECIMAL(12, 3) NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    applicant_id BIGINT      NOT NULL,
    reviewer_id  BIGINT,
    review_remark VARCHAR(255),
    review_time  DATETIME,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_adj_material (material_id),
    INDEX idx_adj_status (status)
);
