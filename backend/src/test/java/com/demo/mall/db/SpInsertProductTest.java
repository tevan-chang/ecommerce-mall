package com.demo.mall.db;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 商品編號自動產生：連續遞增、併發不撞號、流水號進位不截斷。 */
class SpInsertProductTest extends AbstractDbTest {

    private static final int THREAD_COUNT = 20;

    @Test
    void consecutiveCalls_incrementFromSeedValue() throws SQLException {
        assertEquals("P004", insertProduct(connection, "商品A"));
        assertEquals("P005", insertProduct(connection, "商品B"));
    }

    @Test
    void concurrentCalls_produceDistinctProductIds() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        Set<String> generatedIds = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            pool.submit(() -> {
                try (Connection conn = DriverManager.getConnection(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
                    startLatch.await();
                    generatedIds.add(insertProduct(conn, "併發商品" + index));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "所有執行緒應在時限內完成");
        pool.shutdown();

        assertEquals(THREAD_COUNT, generatedIds.size(), "所有執行緒產生的商品編號應互不重複");

        try (Statement st = connection.createStatement();
             var rs = st.executeQuery("SELECT last_seq FROM product_seq WHERE seq_key = 'P'")) {
            rs.next();
            assertEquals(3 + THREAD_COUNT, rs.getInt("last_seq"), "計數器應恰好增加執行緒數");
        }
    }

    @Test
    void overflowPastThreeDigits_doesNotTruncate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("UPDATE product_seq SET last_seq = 999 WHERE seq_key = 'P'");
        }

        assertEquals("P1000", insertProduct(connection, "第一千號商品"));
    }

    private String insertProduct(Connection conn, String name) throws SQLException {
        try (CallableStatement cs = conn.prepareCall("{call sp_insert_product(?, ?, ?, ?)}")) {
            cs.setString(1, name);
            cs.setBigDecimal(2, new BigDecimal("100"));
            cs.setInt(3, 1);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.execute();
            return cs.getString(4);
        }
    }
}
