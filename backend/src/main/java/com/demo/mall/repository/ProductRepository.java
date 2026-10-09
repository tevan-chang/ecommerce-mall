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
public class ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String insertProduct(String productName, BigDecimal price, int quantity) {
        Map<String, Object> out = jdbcTemplate.call(con -> {
            CallableStatement cs = con.prepareCall("{call sp_insert_product(?, ?, ?, ?)}");
            cs.setString(1, productName);
            cs.setBigDecimal(2, price);
            cs.setInt(3, quantity);
            cs.registerOutParameter(4, Types.VARCHAR);
            return cs;
        }, List.of(
                new SqlParameter("p_name", Types.VARCHAR),
                new SqlParameter("p_price", Types.DECIMAL),
                new SqlParameter("p_qty", Types.INTEGER),
                new SqlOutParameter("p_id", Types.VARCHAR)));
        return (String) out.get("p_id");
    }

    public List<ProductRecord> findAllProducts() {
        return jdbcTemplate.query("{call sp_get_available_products()}", (rs, rowNum) -> new ProductRecord(
                rs.getString("product_id"),
                rs.getString("product_name"),
                rs.getBigDecimal("price"),
                rs.getInt("quantity")
        ));
    }

    public BigDecimal deductStock(String productId, int quantity) {
        Map<String, Object> out = jdbcTemplate.call(con -> {
            CallableStatement cs = con.prepareCall("{call sp_deduct_stock(?, ?, ?)}");
            cs.setString(1, productId);
            cs.setInt(2, quantity);
            cs.registerOutParameter(3, Types.DECIMAL);
            return cs;
        }, List.of(
                new SqlParameter("p_product_id", Types.VARCHAR),
                new SqlParameter("p_qty", Types.INTEGER),
                new SqlOutParameter("p_price", Types.DECIMAL)));
        return (BigDecimal) out.get("p_price");
    }
}
