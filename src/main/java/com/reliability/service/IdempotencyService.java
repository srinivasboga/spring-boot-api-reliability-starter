package com.reliability.service;
import com.reliability.model.PaymentRequest;
import com.reliability.model.PaymentResponse;
import com.reliability.store.IdempotencyStore;
import org.springframework.stereotype.Service;
@Service
public class IdempotencyService {
 private final IdempotencyStore store;
 public IdempotencyService(IdempotencyStore store) { this.store = store; }
 public PaymentResponse processPayment(String key, PaymentRequest request) {
  return store.executeOnce(key, () -> PaymentResponse.approved(request));
 }
}
