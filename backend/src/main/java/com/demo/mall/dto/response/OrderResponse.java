package com.demo.mall.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(String orderId, String memberId, BigDecimal totalPrice,
                             Integer payStatus, List<OrderItemResponse> items) {
}
