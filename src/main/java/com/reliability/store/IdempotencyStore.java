package com.reliability.store;

import com.reliability.model.IdempotencyRecord;
import com.reliability.model.IdempotencyExecution;

import java.util.Optional;
import java.util.function.Supplier;

public interface IdempotencyStore {

 IdempotencyExecution executeOnceWithMetadata(
         String key,
         Supplier<IdempotencyRecord> operation
 );

 default IdempotencyRecord executeOnce(
         String key,
         Supplier<IdempotencyRecord> operation
 ) {
  return executeOnceWithMetadata(key, operation).record();
 }

 Optional<IdempotencyRecord> find(String key);
}
