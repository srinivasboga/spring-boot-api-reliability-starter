package com.reliability.store;
import com.reliability.model.PaymentResponse;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {
 private final ConcurrentHashMap<String, PaymentResponse> results = new ConcurrentHashMap<>();
 public PaymentResponse executeOnce(String key, Supplier<PaymentResponse> operation) {
  return results.computeIfAbsent(key, ignored -> operation.get());
 }
 public Optional<PaymentResponse> find(String key) { return Optional.ofNullable(results.get(key)); }
}
