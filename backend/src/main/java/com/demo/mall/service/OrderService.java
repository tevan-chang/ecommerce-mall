package com.demo.mall.service;

import com.demo.mall.common.BusinessException;
import com.demo.mall.common.ErrorCode;
import com.demo.mall.common.SqlStateUtils;
import com.demo.mall.dto.request.OrderCreateRequest;
import com.demo.mall.dto.request.OrderItemRequest;
import com.demo.mall.dto.response.OrderItemResponse;
import com.demo.mall.dto.response.OrderResponse;
import com.demo.mall.repository.OrderRepository;
import com.demo.mall.repository.ProductRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                         TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        validateNoDuplicateProductIds(request.items());

        Map<String, String> productNameById = new HashMap<>();
        productRepository.findAvailableProducts()
                .forEach(p -> productNameById.put(p.productId(), p.productName()));

        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        return transactionTemplate.execute(status -> {
            List<OrderItemResponse> itemResponses = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (OrderItemRequest item : sortedItems) {
                BigDecimal price = deductStock(item.productId(), item.quantity());
                BigDecimal itemPrice = price.multiply(BigDecimal.valueOf(item.quantity()));
                total = total.add(itemPrice);
                String productName = productNameById.getOrDefault(item.productId(), "");
                itemResponses.add(new OrderItemResponse(item.productId(), productName,
                        item.quantity(), price, itemPrice));
            }

            String date = LocalDate.now(TAIPEI).format(DATE_FORMAT);
            String orderId = orderRepository.nextOrderId(date);
            orderRepository.insertOrder(orderId, request.memberId(), total);
            for (OrderItemResponse item : itemResponses) {
                orderRepository.insertOrderDetail(orderId, item.productId(), item.quantity(),
                        item.standPrice(), item.itemPrice());
            }

            return new OrderResponse(orderId, request.memberId(), total, 0, itemResponses);
        });
    }

    private BigDecimal deductStock(String productId, int quantity) {
        try {
            return productRepository.deductStock(productId, quantity);
        } catch (DataAccessException ex) {
            String sqlState = SqlStateUtils.extract(ex);
            if ("45001".equals(sqlState)) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在：" + productId);
            }
            if ("45002".equals(sqlState)) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "庫存不足：" + productId);
            }
            throw ex;
        }
    }

    private void validateNoDuplicateProductIds(List<OrderItemRequest> items) {
        Set<String> seen = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!seen.add(item.productId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "重複的商品編號：" + item.productId());
            }
        }
    }
}
