package com.demo.mall.service;

import com.demo.mall.common.BusinessException;
import com.demo.mall.common.ErrorCode;
import com.demo.mall.dto.request.OrderCreateRequest;
import com.demo.mall.dto.request.OrderItemRequest;
import com.demo.mall.dto.response.OrderResponse;
import com.demo.mall.repository.OrderRepository;
import com.demo.mall.repository.ProductRecord;
import com.demo.mall.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 案例 6、7：訂單 Service 的重複品項驗證與排序／金額加總。 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderService orderService;

    private void setUp() {
        orderService = new OrderService(orderRepository, productRepository, transactionTemplate);
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionTemplateToRunCallback() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<OrderResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction((TransactionStatus) null);
        });
    }

    @Test
    void duplicateProductId_throwsValidationError() {
        setUp();
        OrderCreateRequest request = new OrderCreateRequest("458", List.of(
                new OrderItemRequest("P001", 1),
                new OrderItemRequest("P001", 2)
        ));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(request));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verifyNoInteractions(orderRepository, transactionTemplate);
    }

    @Test
    void itemsAreProcessedInProductIdOrder_andTotalIsSumOfItemPrices() {
        setUp();
        stubTransactionTemplateToRunCallback();

        when(productRepository.findAllProducts()).thenReturn(List.of(
                new ProductRecord("P001", "商品一", new BigDecimal("1000"), 10),
                new ProductRecord("P002", "商品二", new BigDecimal("1200"), 10)
        ));
        when(productRepository.deductStock(eq("P001"), eq(1))).thenReturn(new BigDecimal("1000"));
        when(productRepository.deductStock(eq("P002"), eq(2))).thenReturn(new BigDecimal("1200"));
        when(orderRepository.nextOrderId(any())).thenReturn("Ms20261009000001");

        OrderCreateRequest request = new OrderCreateRequest("458", List.of(
                new OrderItemRequest("P002", 2),
                new OrderItemRequest("P001", 1)
        ));

        OrderResponse response = orderService.createOrder(request);

        assertEquals(new BigDecimal("3400"), response.totalPrice());

        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).deductStock("P001", 1);
        inOrder.verify(productRepository).deductStock("P002", 2);
    }
}
