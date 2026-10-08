package com.demo.mall.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextOrderId(String date) {
        Map<String, Object> out = jdbcTemplate.call(con -> {
            CallableStatement cs = con.prepareCall("{call sp_next_order_id(?, ?)}");
            cs.setString(1, date);
            cs.registerOutParameter(2, Types.VARCHAR);
            return cs;
        }, List.of(
                new SqlParameter("p_date", Types.CHAR),
                new SqlOutParameter("p_order_id", Types.VARCHAR)));
        return (String) out.get("p_order_id");
    }

    public void insertOrder(String orderId, String memberId, BigDecimal total) {
        jdbcTemplate.update("{call sp_insert_order(?, ?, ?)}", orderId, memberId, total);
    }

    public void insertOrderDetail(String orderId, String productId, int quantity,
                                   BigDecimal standPrice, BigDecimal itemPrice) {
        jdbcTemplate.update("{call sp_insert_order_detail(?, ?, ?, ?, ?)}",
                orderId, productId, quantity, standPrice, itemPrice);
    }
}
