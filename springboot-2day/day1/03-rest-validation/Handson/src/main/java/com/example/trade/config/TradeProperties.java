package com.example.trade.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@ConfigurationProperties (prefix="trade")
@Validated 
public record TradeProperties(@NotNull BigDecimal maxQuantity,@NotBlank String venue) {
}
