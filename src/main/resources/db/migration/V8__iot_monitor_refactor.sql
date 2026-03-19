CREATE TABLE IF NOT EXISTS iot_device (
    device_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(20) NOT NULL COMMENT 'PHYSICAL/SIMULATED',
    device_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/OFFLINE/DISABLED',
    last_reported_at DATETIME,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_device_source_status (source_type, device_status)
);

CREATE TABLE IF NOT EXISTS iot_device_binding (
    binding_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    farmland_id BIGINT NOT NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_binding_device (device_id, is_active),
    INDEX idx_iot_binding_farmland (farmland_id, is_active)
);

CREATE TABLE IF NOT EXISTS iot_simulation_profile (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    sensor_type VARCHAR(30) NOT NULL,
    base_value DECIMAL(10, 2) NOT NULL,
    fluctuation_range DECIMAL(10, 2) NOT NULL,
    warning_value DECIMAL(10, 2) NOT NULL,
    warning_probability DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    interval_seconds INT NOT NULL DEFAULT 600,
    is_enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_iot_sim_profile_device_sensor (device_id, sensor_type),
    INDEX idx_iot_sim_profile_enabled (is_enabled)
);

CREATE TABLE IF NOT EXISTS iot_warning_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    sensor_data_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    farmland_id BIGINT NOT NULL,
    batch_id BIGINT,
    sensor_type VARCHAR(30) NOT NULL,
    trigger_value DECIMAL(10, 2) NOT NULL,
    triggered_at DATETIME NOT NULL,
    handle_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/TASK_CREATED/TASK_LINKED/FAILED',
    dispatch_mode VARCHAR(20) COMMENT 'MANUAL/AUTO/AUTO_AI',
    task_id BIGINT,
    failure_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iot_warning_rule_time (rule_id, triggered_at),
    INDEX idx_iot_warning_farmland_time (farmland_id, triggered_at),
    INDEX idx_iot_warning_handle_status (handle_status, triggered_at),
    INDEX idx_iot_warning_batch (batch_id)
);

CREATE TABLE IF NOT EXISTS iot_task_dispatch_record (
    dispatch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    task_id BIGINT,
    dispatch_mode VARCHAR(20) NOT NULL COMMENT 'MANUAL/AUTO/AUTO_AI',
    dispatch_status VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/LINKED_EXISTING',
    operator_id BIGINT,
    ai_summary TEXT,
    error_message VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_iot_dispatch_event (event_id),
    INDEX idx_iot_dispatch_rule (rule_id, created_at),
    INDEX idx_iot_dispatch_task (task_id)
);

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'min_val'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'min_value'
        ),
        'ALTER TABLE agri_task_rule CHANGE COLUMN min_val min_value DECIMAL(10,2) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'max_val'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'max_value'
        ),
        'ALTER TABLE agri_task_rule CHANGE COLUMN max_val max_value DECIMAL(10,2) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'trigger_condition'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN trigger_condition VARCHAR(20) NOT NULL DEFAULT ''OUTSIDE_RANGE'' COMMENT ''LT/GT/OUTSIDE_RANGE'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'create_mode'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN create_mode VARCHAR(20) NOT NULL DEFAULT ''AUTO'' COMMENT ''MANUAL/AUTO/AUTO_AI'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'task_type'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN task_type VARCHAR(50) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'task_priority'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN task_priority INT NOT NULL DEFAULT 2'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'dispatch_cooldown_minutes'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN dispatch_cooldown_minutes INT NOT NULL DEFAULT 60'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'is_enabled'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN is_enabled TINYINT NOT NULL DEFAULT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'created_at'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'updated_at'
        ),
        'SELECT 1',
        'ALTER TABLE agri_task_rule ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'auto_task_type'
        ),
        'UPDATE agri_task_rule SET task_type = COALESCE(task_type, auto_task_type) WHERE task_type IS NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'priority'
        ),
        'UPDATE agri_task_rule SET task_priority = COALESCE(task_priority, priority, 2)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'cooldown_minutes'
        ),
        'UPDATE agri_task_rule SET dispatch_cooldown_minutes = COALESCE(dispatch_cooldown_minutes, cooldown_minutes, 60)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'is_enable'
        ),
        'UPDATE agri_task_rule SET is_enabled = CASE WHEN is_enable IS NULL OR is_enable = 0 THEN 0 ELSE 1 END',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE agri_task_rule
SET trigger_condition = COALESCE(NULLIF(trigger_condition, ''), 'OUTSIDE_RANGE'),
    create_mode = COALESCE(NULLIF(create_mode, ''), 'AUTO'),
    task_priority = COALESCE(task_priority, 2),
    dispatch_cooldown_minutes = COALESCE(dispatch_cooldown_minutes, 60),
    is_enabled = COALESCE(is_enabled, 1);

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'auto_task_type'
        ),
        'ALTER TABLE agri_task_rule DROP COLUMN auto_task_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'priority'
        ),
        'ALTER TABLE agri_task_rule DROP COLUMN priority',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'is_enable'
        ),
        'ALTER TABLE agri_task_rule DROP COLUMN is_enable',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND column_name = 'cooldown_minutes'
        ),
        'ALTER TABLE agri_task_rule DROP COLUMN cooldown_minutes',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'agri_task_rule'
              AND index_name = 'idx_iot_rule_sensor_enabled'
        ),
        'SELECT 1',
        'CREATE INDEX idx_iot_rule_sensor_enabled ON agri_task_rule (sensor_type, is_enabled)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'device_id'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN device_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'farmland_id'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN farmland_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'source_type'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT ''SIMULATED'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'quality_status'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN quality_status VARCHAR(20) NOT NULL DEFAULT ''VALID'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'reported_at'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN reported_at DATETIME NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'created_at'
        ),
        'SELECT 1',
        'ALTER TABLE iot_sensor_data ADD COLUMN created_at DATETIME NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE iot_sensor_data
SET reported_at = COALESCE(reported_at, create_time)
WHERE reported_at IS NULL
  AND create_time IS NOT NULL;

UPDATE iot_sensor_data
SET created_at = COALESCE(created_at, create_time, NOW())
WHERE created_at IS NULL;

UPDATE iot_sensor_data
SET source_type = COALESCE(NULLIF(source_type, ''), 'SIMULATED'),
    quality_status = COALESCE(NULLIF(quality_status, ''), 'VALID');

UPDATE iot_sensor_data
SET sensor_type = CASE
    WHEN LOWER(sensor_type) IN ('temp', 'temperature') THEN 'temperature'
    WHEN LOWER(sensor_type) IN ('humidity', 'air_humidity') THEN 'humidity'
    WHEN LOWER(sensor_type) IN ('light', 'illumination') THEN 'light'
    WHEN LOWER(sensor_type) IN ('soil_moisture', 'soilmoisture', 'moisture') THEN 'soil_moisture'
    WHEN LOWER(sensor_type) = 'ph' THEN 'ph'
    WHEN LOWER(sensor_type) = 'ec' THEN 'ec'
    ELSE LOWER(sensor_type)
END;

UPDATE iot_sensor_data d
LEFT JOIN agri_farmland f ON f.code = d.plot_id
SET d.farmland_id = COALESCE(
        d.farmland_id,
        f.id,
        CASE
            WHEN d.plot_id REGEXP '^[0-9]+$' THEN CAST(d.plot_id AS UNSIGNED)
            ELSE NULL
        END
    );

INSERT INTO iot_device (device_code, device_name, source_type, device_status, last_reported_at, remark, created_at, updated_at)
SELECT
    CONCAT('SIM-', COALESCE(NULLIF(f.code, ''), CONCAT('FARMLAND-', f.id))),
    CONCAT(f.name, '模拟设备'),
    'SIMULATED',
    'ONLINE',
    NOW(),
    'IoT migration bootstrap',
    NOW(),
    NOW()
FROM agri_farmland f
WHERE NOT EXISTS (
    SELECT 1
    FROM iot_device d
    WHERE d.device_code = CONCAT('SIM-', COALESCE(NULLIF(f.code, ''), CONCAT('FARMLAND-', f.id)))
);

INSERT INTO iot_device_binding (device_id, farmland_id, is_active, started_at, created_at, updated_at)
SELECT
    d.device_id,
    f.id,
    1,
    NOW(),
    NOW(),
    NOW()
FROM agri_farmland f
JOIN iot_device d
    ON d.device_code = CONCAT('SIM-', COALESCE(NULLIF(f.code, ''), CONCAT('FARMLAND-', f.id)))
WHERE NOT EXISTS (
    SELECT 1
    FROM iot_device_binding b
    WHERE b.device_id = d.device_id
      AND b.farmland_id = f.id
      AND b.is_active = 1
);

INSERT INTO iot_simulation_profile (
    device_id,
    sensor_type,
    base_value,
    fluctuation_range,
    warning_value,
    warning_probability,
    interval_seconds,
    is_enabled,
    created_at,
    updated_at
)
SELECT d.device_id, 'temperature', 26.00, 4.00, 38.00, 0.15, 600, 1, NOW(), NOW()
FROM iot_device d
WHERE d.source_type = 'SIMULATED'
  AND NOT EXISTS (
      SELECT 1
      FROM iot_simulation_profile p
      WHERE p.device_id = d.device_id
        AND p.sensor_type = 'temperature'
  );

INSERT INTO iot_simulation_profile (
    device_id,
    sensor_type,
    base_value,
    fluctuation_range,
    warning_value,
    warning_probability,
    interval_seconds,
    is_enabled,
    created_at,
    updated_at
)
SELECT d.device_id, 'humidity', 65.00, 8.00, 25.00, 0.20, 600, 1, NOW(), NOW()
FROM iot_device d
WHERE d.source_type = 'SIMULATED'
  AND NOT EXISTS (
      SELECT 1
      FROM iot_simulation_profile p
      WHERE p.device_id = d.device_id
        AND p.sensor_type = 'humidity'
  );

INSERT INTO iot_simulation_profile (
    device_id,
    sensor_type,
    base_value,
    fluctuation_range,
    warning_value,
    warning_probability,
    interval_seconds,
    is_enabled,
    created_at,
    updated_at
)
SELECT d.device_id, 'light', 1800.00, 500.00, 600.00, 0.08, 600, 1, NOW(), NOW()
FROM iot_device d
WHERE d.source_type = 'SIMULATED'
  AND NOT EXISTS (
      SELECT 1
      FROM iot_simulation_profile p
      WHERE p.device_id = d.device_id
        AND p.sensor_type = 'light'
  );

INSERT INTO iot_simulation_profile (
    device_id,
    sensor_type,
    base_value,
    fluctuation_range,
    warning_value,
    warning_probability,
    interval_seconds,
    is_enabled,
    created_at,
    updated_at
)
SELECT d.device_id, 'soil_moisture', 42.00, 7.00, 22.00, 0.16, 600, 1, NOW(), NOW()
FROM iot_device d
WHERE d.source_type = 'SIMULATED'
  AND NOT EXISTS (
      SELECT 1
      FROM iot_simulation_profile p
      WHERE p.device_id = d.device_id
        AND p.sensor_type = 'soil_moisture'
  );

INSERT INTO iot_simulation_profile (
    device_id,
    sensor_type,
    base_value,
    fluctuation_range,
    warning_value,
    warning_probability,
    interval_seconds,
    is_enabled,
    created_at,
    updated_at
)
SELECT d.device_id, 'ph', 6.30, 0.40, 4.80, 0.06, 600, 1, NOW(), NOW()
FROM iot_device d
WHERE d.source_type = 'SIMULATED'
  AND NOT EXISTS (
      SELECT 1
      FROM iot_simulation_profile p
      WHERE p.device_id = d.device_id
        AND p.sensor_type = 'ph'
  );

UPDATE iot_sensor_data d
JOIN iot_device_binding b
    ON b.farmland_id = d.farmland_id
   AND b.is_active = 1
JOIN iot_device dev
    ON dev.device_id = b.device_id
   AND dev.source_type = 'SIMULATED'
SET d.device_id = dev.device_id
WHERE d.farmland_id IS NOT NULL
  AND d.device_id IS NULL;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_plot_time'
        ),
        'DROP INDEX idx_plot_time ON iot_sensor_data',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_sensor_type_time'
        ),
        'DROP INDEX idx_sensor_type_time ON iot_sensor_data',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_sensor_plot_type_time'
        ),
        'DROP INDEX idx_sensor_plot_type_time ON iot_sensor_data',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'value'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'sensor_value'
        ),
        'ALTER TABLE iot_sensor_data CHANGE COLUMN value sensor_value DECIMAL(10,2) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE iot_sensor_data
    MODIFY COLUMN sensor_type VARCHAR(30) NOT NULL,
    MODIFY COLUMN plot_id VARCHAR(50) NULL,
    MODIFY COLUMN source_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN quality_status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    MODIFY COLUMN reported_at DATETIME NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND column_name = 'create_time'
        ),
        'ALTER TABLE iot_sensor_data DROP COLUMN create_time',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_iot_sensor_plot_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_iot_sensor_plot_time ON iot_sensor_data (plot_id, reported_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_iot_sensor_farmland_type_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_iot_sensor_farmland_type_time ON iot_sensor_data (farmland_id, sensor_type, reported_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_iot_sensor_device_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_iot_sensor_device_time ON iot_sensor_data (device_id, reported_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'iot_sensor_data'
              AND index_name = 'idx_iot_sensor_source'
        ),
        'SELECT 1',
        'CREATE INDEX idx_iot_sensor_source ON iot_sensor_data (source_type, quality_status)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
