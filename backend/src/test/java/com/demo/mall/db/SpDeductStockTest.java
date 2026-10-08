package com.demo.mall.db;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 案例 1、2：sp_deduct_stock 的庫存不足與商品不存在情境。 */
class SpDeductStockTest extends AbstractDbTest {

    @Test
    void insufficientStock_throwsSqlState45002() {
        // P001 初始庫存為 5，要求扣 999 必定不足
        SQLException ex = assertThrows(SQLException.class, () -> callDeductStock("P001", 999));
        assertEquals("45002", ex.getSQLState());
    }

    @Test
    void productNotFound_throwsSqlState45001() {
        SQLException ex = assertThrows(SQLException.class, () -> callDeductStock("NOPE", 1));
        assertEquals("45001", ex.getSQLState());
    }

    private void callDeductStock(String productId, int qty) throws SQLException {
        try (CallableStatement cs = connection.prepareCall("{call sp_deduct_stock(?, ?, ?)}")) {
            cs.setString(1, productId);
            cs.setInt(2, qty);
            cs.registerOutParameter(3, Types.DECIMAL);
            cs.execute();
        }
    }
}
