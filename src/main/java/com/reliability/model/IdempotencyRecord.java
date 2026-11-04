package com.reliability.model;

public record IdempotencyRecord(
        String fingerprint,
        PaymentResponse response
) {}