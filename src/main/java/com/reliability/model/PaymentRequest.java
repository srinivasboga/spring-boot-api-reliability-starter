package com.reliability.model;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record PaymentRequest(@NotBlank String orderId, @Positive BigDecimal amount) {}
