package com.demo.mall.integration;

import com.demo.mall.common.BusinessException;
import com.demo.mall.common.ErrorCode;
import com.demo.mall.dto.request.OrderCreateRequest;
import com.demo.mall.dto.request.OrderItemRequest;
import com.demo.mall.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 案例 5：兩品項訂單中，依 productId 排序後處理的第二項庫存不足時，第一項已扣的庫存需整筆 rollback。 */
class OrderRollbackIntegrationTest extends AbstractOrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void secondItemInsufficientStock_rollsBackFirstItemDeduction() {
        jdbcTemplate.update("INSERT INTO product (product_id, product_name, price, quantity) VALUES (?,?,?,?)",
                "P001", "商品一", new BigDecimal("1000"), 5);
        jdbcTemplate.update("INSERT INTO product (product_id, product_name, price, quantity) VALUES (?,?,?,?)",
                "P002", "商品二", new BigDecimal("1200"), 0);

        OrderCreateRequest request = new OrderCreateRequest("458", List.of(
                new OrderItemRequest("P001", 1),
                new OrderItemRequest("P002", 1)
        ));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());

        Integer p001Stock = jdbcTemplate.queryForObject(
                "SELECT quantity FROM product WHERE product_id = ?", Integer.class, "P001");
        assertEquals(5, p001Stock, "第一項（P001）庫存應維持不變，因第二項失敗而整筆 rollback");

        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        assertEquals(0L, orderCount, "交易 rollback 後不應留下任何訂單紀錄");
    }
}
