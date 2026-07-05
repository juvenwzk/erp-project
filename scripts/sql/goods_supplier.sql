-- 商品-供应商关联表（多对多），可在已有库手动执行
CREATE TABLE IF NOT EXISTS goods_supplier (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    supply_price DECIMAL(10, 2) NULL COMMENT '该供应商供货价',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_goods_supplier (goods_id, supplier_id),
    KEY idx_supplier_id (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品供应商关联';
