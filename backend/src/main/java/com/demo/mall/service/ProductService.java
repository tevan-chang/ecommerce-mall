package com.demo.mall.service;

import com.demo.mall.common.BusinessException;
import com.demo.mall.common.ErrorCode;
import com.demo.mall.common.SqlStateUtils;
import com.demo.mall.dto.request.ProductCreateRequest;
import com.demo.mall.dto.response.ProductResponse;
import com.demo.mall.repository.ProductRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        try {
            String productId = productRepository.insertProduct(
                    request.productName(), request.price(), request.quantity());
            return new ProductResponse(productId, request.productName(), request.price(), request.quantity());
        } catch (DataAccessException ex) {
            if ("45003".equals(SqlStateUtils.extract(ex))) {
                throw new BusinessException(ErrorCode.DUPLICATE_PRODUCT, "商品編號已存在");
            }
            throw ex;
        }
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllProducts().stream()
                .map(p -> new ProductResponse(p.productId(), p.productName(), p.price(), p.quantity()))
                .toList();
    }

    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findAllProducts().stream()
                .filter(p -> p.quantity() > 0)
                .map(p -> new ProductResponse(p.productId(), p.productName(), p.price(), p.quantity()))
                .toList();
    }
}
