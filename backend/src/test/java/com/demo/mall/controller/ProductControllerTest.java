package com.demo.mall.controller;

import com.demo.mall.dto.response.ProductResponse;
import com.demo.mall.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 案例 8：Controller 驗證失敗情境，包含商品名稱含 &lt;script&gt;。 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void productNameWithScriptTag_isRejectedWith400() throws Exception {
        String body = objectMapper.writeValueAsString(new ProductRequestFixture(
                "<script>alert(1)</script>", 1000, 10));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(productService);
    }

    @Test
    void missingRequiredFields_isRejectedWith400() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listProducts_withoutInStockParam_returnsAllProductsIncludingOutOfStock() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                new ProductResponse("P001", "商品一", new BigDecimal("1000"), 10),
                new ProductResponse("P002", "商品二", new BigDecimal("500"), 0)
        ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].quantity").value(0));

        verify(productService).getAllProducts();
    }

    @Test
    void listProducts_withInStockTrue_returnsOnlyAvailableProducts() throws Exception {
        when(productService.getAvailableProducts()).thenReturn(List.of(
                new ProductResponse("P001", "商品一", new BigDecimal("1000"), 10)
        ));

        mockMvc.perform(get("/api/v1/products").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(productService).getAvailableProducts();
    }

    @Test
    void createProduct_returnsGeneratedProductId() throws Exception {
        when(productService.createProduct(any())).thenReturn(
                new ProductResponse("P004", "新商品", new BigDecimal("500"), 10));

        String body = objectMapper.writeValueAsString(new ProductRequestFixture("新商品", 500, 10));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value("P004"));
    }

    private record ProductRequestFixture(String productName, int price, int quantity) {
    }
}
