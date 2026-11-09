package com.example.trade.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

// Keys under "trade.security." in the yml. A secret that is missing or too short stops the application at startup,
// instead of silently running with a guessable signing key.
@ConfigurationProperties(prefix = "trade.security")
@Validated
public record SecurityProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters (256 bits) for HS256") String jwtSecret,
        @DefaultValue("15m") Duration tokenTtl) {}
