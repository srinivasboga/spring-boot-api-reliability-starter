package com.reliability.model;
import java.math.BigDecimal;
import java.util.UUID;
public record PaymentResponse(String paymentId, String orderId, BigDecimal amount, String status) {
 public static PaymentResponse approved(PaymentRequest request) {
  return new PaymentResponse(UUID.randomUUID().toString(), request.orderId(), request.amount(), "APPROVED");
 }
}
