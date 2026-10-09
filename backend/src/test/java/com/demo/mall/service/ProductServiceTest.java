package com.demo.mall.service;

import com.demo.mall.dto.request.ProductCreateRequest;
import com.demo.mall.dto.response.ProductResponse;
import com.demo.mall.repository.ProductRecord;
import com.demo.mall.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** 商品管理頁：getAllProducts 含缺貨商品，getAvailableProducts 過濾庫存 > 0。 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    private void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createProduct_usesGeneratedProductIdFromRepository() {
        setUp();
        when(productRepository.insertProduct("新商品", new BigDecimal("500"), 10))
                .thenReturn("P004");

        ProductResponse result = productService.createProduct(
                new ProductCreateRequest("新商品", new BigDecimal("500"), 10));

        assertEquals("P004", result.productId());
        assertEquals("新商品", result.productName());
    }

    @Test
    void getAllProducts_includesOutOfStockProducts() {
        setUp();
        when(productRepository.findAllProducts()).thenReturn(List.of(
                new ProductRecord("P001", "商品一", new BigDecimal("1000"), 10),
                new ProductRecord("P002", "商品二", new BigDecimal("500"), 0)
        ));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals(0, result.get(1).quantity());
    }

    @Test
    void getAvailableProducts_filtersOutZeroStockProducts() {
        setUp();
        when(productRepository.findAllProducts()).thenReturn(List.of(
                new ProductRecord("P001", "商品一", new BigDecimal("1000"), 10),
                new ProductRecord("P002", "商品二", new BigDecimal("500"), 0)
        ));

        List<ProductResponse> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals("P001", result.get(0).productId());
    }
}
