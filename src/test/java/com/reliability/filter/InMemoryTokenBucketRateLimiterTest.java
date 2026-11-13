package com.reliability.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTokenBucketRateLimiterTest {

    @Test
    void limitsRequestsPerClientAndKeepsClientBucketsIndependent() {
        var limiter = new InMemoryTokenBucketRateLimiter(
                0.1,
                1,
                100
        );

        assertTrue(limiter.tryAcquire("client-a").allowed());
        assertFalse(limiter.tryAcquire("client-a").allowed());
        assertTrue(limiter.tryAcquire("client-b").allowed());
    }
}
