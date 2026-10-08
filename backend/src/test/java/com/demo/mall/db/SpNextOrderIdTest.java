package com.demo.mall.db;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 案例 3：sp_next_order_id 連續呼叫不重複；不同日期各自從 1 起算。 */
class SpNextOrderIdTest extends AbstractDbTest {

    @Test
    void consecutiveCalls_incrementWithinSameDate() throws SQLException {
        assertEquals("Ms20261009000001", nextOrderId("20261009"));
        assertEquals("Ms20261009000002", nextOrderId("20261009"));
    }

    @Test
    void differentDate_restartsFromOne() throws SQLException {
        assertEquals("Ms20261009000001", nextOrderId("20261009"));
        assertEquals("Ms20261009000002", nextOrderId("20261009"));
        assertEquals("Ms20261010000001", nextOrderId("20261010"));
    }

    private String nextOrderId(String date) throws SQLException {
        try (CallableStatement cs = connection.prepareCall("{call sp_next_order_id(?, ?)}")) {
            cs.setString(1, date);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        }
    }
}
