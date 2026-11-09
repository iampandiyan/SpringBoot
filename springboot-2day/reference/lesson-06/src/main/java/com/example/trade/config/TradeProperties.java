package com.example.trade.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Type-safe configuration: keys under "trade." in application*.yml are bound to these fields at startup.
// @Validated makes a bad value (missing, blank) fail the startup instead of failing at 3 a.m. in production.
@ConfigurationProperties(prefix = "trade")
@Validated
public record TradeProperties(@NotNull BigDecimal maxQuantity, @NotBlank String venue) {}
