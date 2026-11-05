
package com.reliability.store;

import com.reliability.model.IdempotencyRecord;
import com.reliability.model.PaymentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryIdempotencyStoreTest {

 @Test
 void sameKeyReturnsOriginalResultAndExecutesOperationOnlyOnce() {

  var store = new InMemoryIdempotencyStore(
          Duration.ofHours(24)
  );
  AtomicInteger executions = new AtomicInteger();

  var first = store.executeOnce("key-123", () -> {
   executions.incrementAndGet();

   var response = new PaymentResponse(
           "payment-1",
           "order-1",
           new BigDecimal("25.00"),
           "APPROVED"
   );

   return new IdempotencyRecord("fingerprint-1", response);
  });

  var second = store.executeOnce("key-123", () -> {
   executions.incrementAndGet();

   var response = new PaymentResponse(
           "payment-2",
           "order-1",
           new BigDecimal("25.00"),
           "APPROVED"
   );

   return new IdempotencyRecord("fingerprint-1", response);
  });

  assertSame(first, second);
  assertEquals(1, executions.get());
 }

 @Test
 void differentKeysAreIndependent() {

  var store = new InMemoryIdempotencyStore(
          Duration.ofHours(24)
  );
  AtomicInteger executions = new AtomicInteger();

  store.executeOnce("key-a", () -> {
   executions.incrementAndGet();

   return new IdempotencyRecord(
           "fingerprint-a",
           new PaymentResponse(
                   "payment-1", "order-1",
                   BigDecimal.TEN, "APPROVED"
           )
   );
  });

  store.executeOnce("key-b", () -> {
   executions.incrementAndGet();

   return new IdempotencyRecord(
           "fingerprint-b",
           new PaymentResponse(
                   "payment-2", "order-2",
                   BigDecimal.TEN, "APPROVED"
           )
   );
  });

  assertEquals(2, executions.get());
 }

 @Test
 void shouldExecuteOperationOnlyOnceFor100ConcurrentRequests()
         throws Exception {

  var store = new InMemoryIdempotencyStore(
          Duration.ofHours(24)
  );

  int numberOfThreads = 100;

  AtomicInteger executionCount = new AtomicInteger();

  ExecutorService executor =
          Executors.newFixedThreadPool(numberOfThreads);

  CountDownLatch ready =
          new CountDownLatch(numberOfThreads);

  CountDownLatch start =
          new CountDownLatch(1);

  List<Future<IdempotencyRecord>> futures = new ArrayList<>();

  try {
   for (int i = 0; i < numberOfThreads; i++) {

    Future<IdempotencyRecord> future = executor.submit(() -> {

     ready.countDown();
     start.await();

     return store.executeOnce("payment-123", () -> {

      executionCount.incrementAndGet();

      try {
       Thread.sleep(100);
      } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       throw new RuntimeException(e);
      }

      return new IdempotencyRecord(
              "fingerprint-123",
              new PaymentResponse(
                      "payment-001",
                      "order-100",
                      BigDecimal.valueOf(500),
                      "APPROVED"
              )
      );
     });
    });

    futures.add(future);
   }

   assertTrue(ready.await(5, TimeUnit.SECONDS));

   start.countDown();

   List<IdempotencyRecord> records = new ArrayList<>();

   for (Future<IdempotencyRecord> future : futures) {
    records.add(future.get(10, TimeUnit.SECONDS));
   }

   assertEquals(1, executionCount.get());

   assertTrue(
           records.stream()
                   .allMatch(record ->
                           record.response().paymentId()
                                   .equals("payment-001"))
   );

  } finally {
   start.countDown();
   executor.shutdownNow();
  }
 }
}
