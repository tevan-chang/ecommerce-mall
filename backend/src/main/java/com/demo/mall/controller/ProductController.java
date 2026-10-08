package com.demo.mall.controller;

import com.demo.mall.common.ApiResponse;
import com.demo.mall.dto.request.ProductCreateRequest;
import com.demo.mall.dto.response.ProductResponse;
import com.demo.mall.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> listProducts(
            @RequestParam(value = "inStock", required = false) Boolean inStock) {
        return ApiResponse.success(productService.getAvailableProducts());
    }
}
