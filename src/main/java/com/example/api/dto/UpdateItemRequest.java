package com.example.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateItemRequest(
        String name,
        String description,
        @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer quantity
) {}
