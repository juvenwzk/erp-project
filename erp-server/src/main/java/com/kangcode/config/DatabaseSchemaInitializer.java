package com.kangcode.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动时确保业务表 id 列具备 AUTO_INCREMENT 主键，并补全历史 NULL 主键。
 * 解决远程库/旧库无自增导致「下拉无法选中、入库报 null」等问题。
 */
@Slf4j
@Component
public class DatabaseSchemaInitializer {

    private static final Map<String, String> TABLE_ID_TYPES = new LinkedHashMap<>();

    static {
        TABLE_ID_TYPES.put("goods", "BIGINT");
        TABLE_ID_TYPES.put("purchase_order", "BIGINT");
        TABLE_ID_TYPES.put("sale_order", "INT");
        TABLE_ID_TYPES.put("customer", "BIGINT");
        TABLE_ID_TYPES.put("supplier", "BIGINT");
        TABLE_ID_TYPES.put("user", "BIGINT");
        TABLE_ID_TYPES.put("goods_type", "INT");
        TABLE_ID_TYPES.put("operate_log", "INT UNSIGNED");
        TABLE_ID_TYPES.put("goods_supplier", "BIGINT");
    }

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ensureGoodsSupplierTable();
        for (Map.Entry<String, String> entry : TABLE_ID_TYPES.entrySet()) {
            ensureAutoIncrementId(entry.getKey(), entry.getValue());
        }
    }

    private void ensureGoodsSupplierTable() {
        try {
            if (tableExists("goods_supplier")) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE TABLE goods_supplier ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                            + "goods_id BIGINT NOT NULL,"
                            + "supplier_id BIGINT NOT NULL,"
                            + "supply_price DECIMAL(10, 2) NULL,"
                            + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "UNIQUE KEY uk_goods_supplier (goods_id, supplier_id),"
                            + "KEY idx_supplier_id (supplier_id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            log.info("已创建 goods_supplier 关联表");
        } catch (Exception e) {
            log.warn("goods_supplier 表自检失败: {}", e.getMessage());
        }
    }

    private void ensureAutoIncrementId(String table, String sqlType) {
        try {
            if (!tableExists(table)) {
                return;
            }
            backfillNullIds(table);
            if (!hasAutoIncrementId(table)) {
                enableAutoIncrement(table, sqlType);
                log.info("表 {} 已启用 AUTO_INCREMENT 主键", table);
            }
        } catch (Exception e) {
            log.warn("表 {} 主键自检失败（请用管理员执行 scripts/sql/ensure_auto_increment.sql）: {}",
                    table, e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private void backfillNullIds(String table) {
        Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id IS NULL",
                Integer.class);
        if (nullCount == null || nullCount == 0) {
            return;
        }
        jdbcTemplate.execute("SET @erp_n := (SELECT IFNULL(MAX(id), 0) FROM " + table + ")");
        int updated = jdbcTemplate.update(
                "UPDATE " + table + " SET id = (@erp_n := @erp_n + 1) WHERE id IS NULL");
        log.info("表 {} 已补全 {} 条 NULL 主键", table, updated);
    }

    private boolean hasAutoIncrementId(String table) {
        String extra = jdbcTemplate.queryForObject(
                "SELECT IFNULL(EXTRA, '') FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME = ? AND COLUMN_NAME = 'id'",
                String.class,
                table);
        return extra != null && extra.toLowerCase().contains("auto_increment");
    }

    private boolean hasPrimaryKeyOnId(String table) {
        String key = jdbcTemplate.queryForObject(
                "SELECT IFNULL(COLUMN_KEY, '') FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME = ? AND COLUMN_NAME = 'id'",
                String.class,
                table);
        return "PRI".equalsIgnoreCase(key);
    }

    /** 已有主键时只加 AUTO_INCREMENT，避免 Multiple primary key defined */
    private void enableAutoIncrement(String table, String sqlType) {
        String alter = "ALTER TABLE " + table + " MODIFY COLUMN id " + sqlType + " NOT NULL AUTO_INCREMENT";
        if (!hasPrimaryKeyOnId(table)) {
            alter += " PRIMARY KEY";
        }
        jdbcTemplate.execute(alter);
    }
}
