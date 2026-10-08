package com.demo.mall.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProductCreateRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,20}$", message = "格式不符")
        String productId,

        @NotBlank
        @Pattern(regexp = "^[^<>]{1,100}$", message = "格式不符或包含不允許字元")
        String productName,

        @NotNull
        @DecimalMin(value = "0", message = "價格需介於 0 ~ 99999999")
        @DecimalMax(value = "99999999", message = "價格需介於 0 ~ 99999999")
        @Digits(integer = 8, fraction = 0, message = "價格需為整數")
        BigDecimal price,

        @NotNull
        @Min(value = 0, message = "庫存需介於 0 ~ 1000000")
        @Max(value = 1000000, message = "庫存需介於 0 ~ 1000000")
        Integer quantity
) {
}
