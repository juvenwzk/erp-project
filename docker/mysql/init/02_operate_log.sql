-- 操作日志表（AOP @Log 切面写入）
USE erp;

CREATE TABLE IF NOT EXISTS operate_log (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    operate_user_id INT UNSIGNED NULL COMMENT '操作人ID',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    class_name VARCHAR(100) NOT NULL COMMENT '类名',
    method_name VARCHAR(100) NOT NULL COMMENT '方法名',
    method_params VARCHAR(2000) NULL COMMENT '方法参数',
    return_value VARCHAR(2000) NULL COMMENT '返回值摘要',
    cost_time BIGINT UNSIGNED NULL COMMENT '耗时(ms)',
    PRIMARY KEY (id),
    KEY idx_operate_time (operate_time),
    KEY idx_operate_user_id (operate_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';
