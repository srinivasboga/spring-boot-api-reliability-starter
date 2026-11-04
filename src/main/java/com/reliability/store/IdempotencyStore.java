package com.reliability.store;

import com.reliability.model.IdempotencyRecord;

import java.util.Optional;
import java.util.function.Supplier;

public interface IdempotencyStore {

 IdempotencyRecord executeOnce(
         String key,
         Supplier<IdempotencyRecord> operation
 );

 Optional<IdempotencyRecord> find(String key);
}