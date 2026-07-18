-- =====================================================
-- 分包计划表按截图更新字段
-- 说明：保留原有字段兼容，新增截图业务字段
-- =====================================================
USE rpw_db;

SET @table_name = 'resource_plan_subcontract';

-- 通用添加列存储过程（避免 IF NOT EXISTS 语法差异）
DELIMITER $$
DROP PROCEDURE IF EXISTS add_col_if_not_exists$$
CREATE PROCEDURE add_col_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_col_if_not_exists(@table_name, 'project_name', 'VARCHAR(200) NULL COMMENT "项目名称（冗余存储）"');
CALL add_col_if_not_exists(@table_name, 'specialty_engineering', 'VARCHAR(200) NULL COMMENT "专业工程"');
CALL add_col_if_not_exists(@table_name, 'subcontract_mode', 'VARCHAR(100) NULL COMMENT "分包模式"');
CALL add_col_if_not_exists(@table_name, 'team_source', 'VARCHAR(100) NULL COMMENT "分包队伍来源"');
CALL add_col_if_not_exists(@table_name, 'latest_entry_date', 'DATE NULL COMMENT "最晚进场日期（与总进度计划相符）"');
CALL add_col_if_not_exists(@table_name, 'actual_entry_date', 'DATE NULL COMMENT "实际进场日期（按招标进度推算）"');
CALL add_col_if_not_exists(@table_name, 'start_prepare_bid_date', 'DATE NULL COMMENT "开始编制招标文件日期"');
CALL add_col_if_not_exists(@table_name, 'actual_bid_date', 'DATE NULL COMMENT "实际招标日期"');
CALL add_col_if_not_exists(@table_name, 'planned_online_bid_date', 'DATE NULL COMMENT "挂网招标日期"');
CALL add_col_if_not_exists(@table_name, 'actual_online_bid_date', 'DATE NULL COMMENT "实际挂网日期"');
CALL add_col_if_not_exists(@table_name, 'planned_award_date', 'DATE NULL COMMENT "定标日期"');
CALL add_col_if_not_exists(@table_name, 'actual_award_date', 'DATE NULL COMMENT "实际定标日期"');
CALL add_col_if_not_exists(@table_name, 'mobilization_period', 'INT NULL COMMENT "动员期（天）"');

DROP PROCEDURE IF EXISTS add_col_if_not_exists;

-- 为已有数据做兼容回填：若存在 wbs_name 且 specialty_engineering 为空，则把 wbs_name 作为专业工程展示（可选）
-- UPDATE `resource_plan_subcontract` SET `specialty_engineering` = `wbs_name` WHERE `specialty_engineering` IS NULL AND `wbs_name` IS NOT NULL;
