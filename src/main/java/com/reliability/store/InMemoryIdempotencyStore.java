package com.reliability.store;

import com.reliability.model.IdempotencyRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

 private final ConcurrentHashMap<String, IdempotencyRecord> results =
         new ConcurrentHashMap<>();

 @Override
 public IdempotencyRecord executeOnce(
         String key,
         Supplier<IdempotencyRecord> operation
 ) {
  return results.computeIfAbsent(key, ignored -> operation.get());
 }

 @Override
 public Optional<IdempotencyRecord> find(String key) {
  return Optional.ofNullable(results.get(key));
 }
}