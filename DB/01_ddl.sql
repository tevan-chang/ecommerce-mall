SET NAMES utf8mb4;

CREATE TABLE product (
  product_id   VARCHAR(20)   NOT NULL PRIMARY KEY,
  product_name VARCHAR(100)  NOT NULL,
  price        DECIMAL(12,0) NOT NULL CHECK (price >= 0),
  quantity     INT           NOT NULL CHECK (quantity >= 0),
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
  order_id   VARCHAR(20)   NOT NULL PRIMARY KEY,
  member_id  VARCHAR(20)   NOT NULL,
  price      DECIMAL(12,0) NOT NULL,
  pay_status TINYINT       NOT NULL DEFAULT 0 CHECK (pay_status IN (0,1)),
  created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_detail (
  order_item_sn BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id      VARCHAR(20)   NOT NULL,
  product_id    VARCHAR(20)   NOT NULL,
  quantity      INT           NOT NULL CHECK (quantity > 0),
  stand_price   DECIMAL(12,0) NOT NULL,
  item_price    DECIMAL(12,0) NOT NULL,
  KEY idx_od_order (order_id),
  KEY idx_od_product (product_id),
  CONSTRAINT fk_od_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id),
  CONSTRAINT fk_od_product FOREIGN KEY (product_id) REFERENCES product(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_seq (
  seq_date CHAR(8) NOT NULL PRIMARY KEY,
  last_seq INT     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_seq (
  seq_key  VARCHAR(10) NOT NULL PRIMARY KEY,
  last_seq INT         NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
