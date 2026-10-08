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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 案例 4：10 個 thread 以 CountDownLatch 同步起跑，搶購庫存只剩 1 件的商品。 */
class OrderConcurrencyIntegrationTest extends AbstractOrderIntegrationTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private OrderService orderService;

    @Test
    void tenThreadsRaceForLastUnit_onlyOneSucceeds_andStockNeverGoesNegative() throws InterruptedException {
        jdbcTemplate.update("INSERT INTO product (product_id, product_name, price, quantity) VALUES (?,?,?,?)",
                "P900", "搶購商品", new BigDecimal("100"), 1);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            String memberId = "M" + i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    OrderCreateRequest request = new OrderCreateRequest(memberId,
                            List.of(new OrderItemRequest("P900", 1)));
                    orderService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        conflictCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "所有 thread 應在時限內完成");
        pool.shutdown();

        assertEquals(1, successCount.get(), "恰好 1 筆下單成功");
        assertEquals(THREAD_COUNT - 1, conflictCount.get(), "其餘 9 筆應因庫存不足回報 409");

        Integer finalStock = jdbcTemplate.queryForObject(
                "SELECT quantity FROM product WHERE product_id = ?", Integer.class, "P900");
        assertEquals(0, finalStock, "最終庫存應為 0，不可為負");
    }
}
