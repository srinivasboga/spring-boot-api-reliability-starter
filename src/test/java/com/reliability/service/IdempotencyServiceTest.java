package com.reliability.service;

import com.reliability.model.PaymentRequest;
import com.reliability.model.PaymentResponse;
import com.reliability.store.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private final IdempotencyService service =
            new IdempotencyService(
                    new InMemoryIdempotencyStore(),
                    new RequestFingerprint()
            );

    @Test
    void sameKeyAndSamePayloadShouldReturnOriginalResponse() {

        var request = new PaymentRequest(
                "order-100",
                new BigDecimal("500.00")
        );

        PaymentResponse first =
                service.processPayment("payment-123", request);

        PaymentResponse second =
                service.processPayment("payment-123", request);

        assertEquals(first.paymentId(), second.paymentId());
    }

    @Test
    void sameKeyAndDifferentAmountShouldThrowConflict() {

        var firstRequest = new PaymentRequest(
                "order-100",
                new BigDecimal("500.00")
        );

        var secondRequest = new PaymentRequest(
                "order-100",
                new BigDecimal("5000.00")
        );

        service.processPayment("payment-123", firstRequest);

        assertThrows(
                IdempotencyConflictException.class,
                () -> service.processPayment(
                        "payment-123", secondRequest
                )
        );
    }

    @Test
    void differentKeysShouldAllowDifferentPayloads() {

        var first = service.processPayment(
                "payment-123",
                new PaymentRequest("order-100", new BigDecimal("500"))
        );

        var second = service.processPayment(
                "payment-456",
                new PaymentRequest("order-100", new BigDecimal("5000"))
        );

        assertNotEquals(first.paymentId(), second.paymentId());
    }

    @Test
    void equivalentBigDecimalScalesShouldProduceSameFingerprint() {

        RequestFingerprint generator = new RequestFingerprint();

        var first = new PaymentRequest(
                "order-100", new BigDecimal("500.0")
        );

        var second = new PaymentRequest(
                "order-100", new BigDecimal("500.00")
        );

        assertEquals(
                generator.generate(first),
                generator.generate(second)
        );
    }
}