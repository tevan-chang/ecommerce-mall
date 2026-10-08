package com.demo.mall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OrderItemRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,20}$", message = "格式不符")
        String productId,

        @NotNull
        @Min(value = 1, message = "數量需大於等於 1")
        Integer quantity
) {
}
