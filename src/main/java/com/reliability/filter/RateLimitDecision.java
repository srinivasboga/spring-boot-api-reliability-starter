package com.reliability.filter;

public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {}
