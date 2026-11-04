package com.reliability.store;
import com.reliability.model.PaymentResponse;
import java.util.Optional;
import java.util.function.Supplier;
public interface IdempotencyStore {
 PaymentResponse executeOnce(String key, Supplier<PaymentResponse> operation);
 Optional<PaymentResponse> find(String key);
}
