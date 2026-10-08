package com.demo.mall.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(String productId, String productName, Integer quantity,
                                 BigDecimal standPrice, BigDecimal itemPrice) {
}
