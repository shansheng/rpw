-- 更新 resource_plan_material 表，增加审批字段
-- 使用存储过程来安全地添加字段（如果不存在）

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_not_exists$$
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    -- 添加 approval_status 字段
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = 'rpw_db' 
        AND TABLE_NAME = 'resource_plan_material' 
        AND COLUMN_NAME = 'approval_status'
    ) THEN
        ALTER TABLE resource_plan_material 
        ADD COLUMN approval_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '审批状态(DRAFT/SUBMITTED/APPROVED/REJECTED)';
    END IF;

    -- 添加 process_instance_id 字段
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = 'rpw_db' 
        AND TABLE_NAME = 'resource_plan_material' 
        AND COLUMN_NAME = 'process_instance_id'
    ) THEN
        ALTER TABLE resource_plan_material 
        ADD COLUMN process_instance_id VARCHAR(100) DEFAULT NULL COMMENT 'Flowable流程实例ID';
    END IF;

    -- 添加 approval_comment 字段
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = 'rpw_db' 
        AND TABLE_NAME = 'resource_plan_material' 
        AND COLUMN_NAME = 'approval_comment'
    ) THEN
        ALTER TABLE resource_plan_material 
        ADD COLUMN approval_comment TEXT DEFAULT NULL COMMENT '审批意见';
    END IF;
END$$

DELIMITER ;

-- 执行存储过程
CALL add_column_if_not_exists();

-- 删除存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
