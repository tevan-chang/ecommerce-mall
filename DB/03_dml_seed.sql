SET NAMES utf8mb4;

INSERT INTO product (product_id, product_name, price, quantity) VALUES
  ('P001', 'osii 舒壓按摩椅', 98000, 5),
  ('P002', '網友最愛起司蛋糕', 1200, 50),
  ('P003', '真愛密碼項鍊', 8500, 20);

INSERT INTO orders (order_id, member_id, price, pay_status) VALUES
  ('Ms20250801186230', '458', 98000, 1),
  ('Ms20250805157824', '55688', 9700, 0),
  ('Ms20250805258200', '1713', 2400, 1);

INSERT INTO order_detail (order_item_sn, order_id, product_id, quantity, stand_price, item_price) VALUES
  (1, 'Ms20250801186230', 'P001', 1, 98000, 98000),
  (2, 'Ms20250805157824', 'P002', 1, 1200, 1200),
  (3, 'Ms20250805157824', 'P003', 1, 8500, 8500),
  (4, 'Ms20250805258200', 'P002', 2, 1200, 2400);
