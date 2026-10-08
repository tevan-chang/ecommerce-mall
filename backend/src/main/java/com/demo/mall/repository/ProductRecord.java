package com.demo.mall.repository;

import java.math.BigDecimal;

public record ProductRecord(String productId, String productName, BigDecimal price, int quantity) {
}
