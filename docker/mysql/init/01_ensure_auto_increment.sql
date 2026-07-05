-- 若库中已有业务表但 id 无自增，则在 Docker 首次初始化时修复（表不存在则跳过）
USE erp;

DELIMITER $$

DROP PROCEDURE IF EXISTS erp_ensure_auto_increment$$
CREATE PROCEDURE erp_ensure_auto_increment()
BEGIN
    DECLARE has_pri INT DEFAULT 0;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'goods') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM goods);
        UPDATE goods SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'goods' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE goods MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE goods MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'purchase_order') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM purchase_order);
        UPDATE purchase_order SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'purchase_order' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE purchase_order MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE purchase_order MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sale_order') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM sale_order);
        UPDATE sale_order SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sale_order' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE sale_order MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE sale_order MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'customer') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM customer);
        UPDATE customer SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'customer' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE customer MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE customer MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM supplier);
        UPDATE supplier SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'supplier' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE supplier MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE supplier MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM user);
        UPDATE user SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE user MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE user MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'goods_type') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM goods_type);
        UPDATE goods_type SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'goods_type' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE goods_type MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE goods_type MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'operate_log') > 0 THEN
        SET @n := (SELECT IFNULL(MAX(id), 0) FROM operate_log);
        UPDATE operate_log SET id = (@n := @n + 1) WHERE id IS NULL;
        SET has_pri := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'operate_log' AND column_name = 'id' AND column_key = 'PRI');
        IF has_pri > 0 THEN
            ALTER TABLE operate_log MODIFY COLUMN id INT UNSIGNED NOT NULL AUTO_INCREMENT;
        ELSE
            ALTER TABLE operate_log MODIFY COLUMN id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY;
        END IF;
    END IF;
END$$

DELIMITER ;

CALL erp_ensure_auto_increment();
DROP PROCEDURE IF EXISTS erp_ensure_auto_increment;
