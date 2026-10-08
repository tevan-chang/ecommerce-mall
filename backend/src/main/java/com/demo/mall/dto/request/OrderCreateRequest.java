package com.demo.mall.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record OrderCreateRequest(

        @NotBlank
        @Pattern(regexp = "^[0-9A-Za-z]{1,20}$", message = "格式不符")
        String memberId,

        @NotEmpty(message = "商品清單至少需 1 筆")
        @Valid
        List<OrderItemRequest> items
) {
}
