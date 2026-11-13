package com.reliability.model;

public record IdempotencyExecution(
        IdempotencyRecord record,
        boolean replayed
) {}
