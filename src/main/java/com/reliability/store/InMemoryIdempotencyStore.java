
package com.reliability.store;

import com.reliability.model.IdempotencyRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

 private final ConcurrentHashMap<String, StoredRecord> results =
         new ConcurrentHashMap<>();

 private final Duration ttl;

 public InMemoryIdempotencyStore(
         @Value("${reliability.idempotency.ttl:24h}")
         Duration ttl
 ) {
  if (ttl.isZero() || ttl.isNegative()) {
   throw new IllegalArgumentException(
           "Idempotency TTL must be positive"
   );
  }

  this.ttl = ttl;
 }

 @Override
 public IdempotencyRecord executeOnce(
         String key,
         Supplier<IdempotencyRecord> operation
 ) {

  StoredRecord stored = results.compute(key, (ignored, existing) -> {

   if (existing != null && !existing.isExpired()) {
    return existing;
   }

   return new StoredRecord(
           operation.get(),
           Instant.now().plus(ttl)
   );
  });

  return stored.record();
 }

 @Override
 public Optional<IdempotencyRecord> find(String key) {

  StoredRecord stored = results.computeIfPresent(
          key,
          (ignored, existing) ->
                  existing.isExpired() ? null : existing
  );

  return Optional.ofNullable(stored)
          .map(StoredRecord::record);
 }

 private record StoredRecord(
         IdempotencyRecord record,
         Instant expiresAt
 ) {
  boolean isExpired() {
   return !Instant.now().isBefore(expiresAt);
  }
 }
}
