SET NAMES utf8mb4;

DELIMITER $$

CREATE PROCEDURE sp_insert_product(
  IN p_id VARCHAR(20), IN p_name VARCHAR(100), IN p_price DECIMAL(12,0), IN p_qty INT)
BEGIN
  DECLARE EXIT HANDLER FOR 1062
    SIGNAL SQLSTATE '45003' SET MESSAGE_TEXT = 'DUPLICATE_PRODUCT';
  INSERT INTO product (product_id, product_name, price, quantity)
  VALUES (p_id, p_name, p_price, p_qty);
END$$

CREATE PROCEDURE sp_get_available_products()
BEGIN
  SELECT product_id, product_name, price, quantity, created_at
  FROM product
  WHERE quantity > 0
  ORDER BY product_id;
END$$

CREATE PROCEDURE sp_deduct_stock(
  IN p_product_id VARCHAR(20), IN p_qty INT, OUT p_price DECIMAL(12,0))
BEGIN
  UPDATE product SET quantity = quantity - p_qty
   WHERE product_id = p_product_id AND quantity >= p_qty;   -- 原子操作，防超賣
  IF ROW_COUNT() = 0 THEN
    IF EXISTS (SELECT 1 FROM product WHERE product_id = p_product_id) THEN
      SIGNAL SQLSTATE '45002' SET MESSAGE_TEXT = 'INSUFFICIENT_STOCK';
    ELSE
      SIGNAL SQLSTATE '45001' SET MESSAGE_TEXT = 'PRODUCT_NOT_FOUND';
    END IF;
  END IF;
  SELECT price INTO p_price FROM product WHERE product_id = p_product_id;
END$$

CREATE PROCEDURE sp_next_order_id(IN p_date CHAR(8), OUT p_order_id VARCHAR(20))
BEGIN
  DECLARE v_seq INT;
  INSERT INTO order_seq (seq_date, last_seq) VALUES (p_date, 1)
    ON DUPLICATE KEY UPDATE last_seq = last_seq + 1;        -- 持有 row lock 至交易結束
  SELECT last_seq INTO v_seq FROM order_seq WHERE seq_date = p_date;
  SET p_order_id = CONCAT('Ms', p_date, LPAD(v_seq, 6, '0'));
END$$

CREATE PROCEDURE sp_insert_order(
  IN p_order_id VARCHAR(20), IN p_member_id VARCHAR(20), IN p_total DECIMAL(12,0))
BEGIN
  INSERT INTO orders (order_id, member_id, price, pay_status)
  VALUES (p_order_id, p_member_id, p_total, 0);
END$$

CREATE PROCEDURE sp_insert_order_detail(
  IN p_order_id VARCHAR(20), IN p_product_id VARCHAR(20), IN p_qty INT,
  IN p_stand_price DECIMAL(12,0), IN p_item_price DECIMAL(12,0))
BEGIN
  INSERT INTO order_detail (order_id, product_id, quantity, stand_price, item_price)
  VALUES (p_order_id, p_product_id, p_qty, p_stand_price, p_item_price);
END$$

DELIMITER ;
