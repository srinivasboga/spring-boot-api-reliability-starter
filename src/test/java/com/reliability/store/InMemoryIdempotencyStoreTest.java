package com.reliability.store;
import com.reliability.model.PaymentResponse;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class InMemoryIdempotencyStoreTest {
 @Test void sameKeyExecutesOperationOnlyOnce() {
  var store = new InMemoryIdempotencyStore(); var count = new AtomicInteger();
  var first = store.executeOnce("key-1", () -> { count.incrementAndGet(); return new PaymentResponse("p1","o1",new BigDecimal("25.00"),"APPROVED"); });
  var second = store.executeOnce("key-1", () -> { count.incrementAndGet(); return new PaymentResponse("p2","o1",new BigDecimal("25.00"),"APPROVED"); });
  assertEquals(first, second); assertEquals(1, count.get());
 }
 @Test void differentKeysAreIndependent() {
  var store = new InMemoryIdempotencyStore(); var count = new AtomicInteger();
  store.executeOnce("a", () -> { count.incrementAndGet(); return new PaymentResponse("p1","o1",BigDecimal.TEN,"APPROVED"); });
  store.executeOnce("b", () -> { count.incrementAndGet(); return new PaymentResponse("p2","o2",BigDecimal.TEN,"APPROVED"); });
  assertEquals(2, count.get());
 }

 @Test
 void shouldExecuteOperationOnlyOnceFor100ConcurrentRequests()
         throws Exception {

  var store = new InMemoryIdempotencyStore();

  int numberOfThreads = 100;

  AtomicInteger executionCount = new AtomicInteger();

  ExecutorService executor =
          Executors.newFixedThreadPool(numberOfThreads);

  CountDownLatch ready =
          new CountDownLatch(numberOfThreads);

  CountDownLatch start =
          new CountDownLatch(1);

  List<Future<PaymentResponse>> futures = new ArrayList<>();

  try {
   for (int i = 0; i < numberOfThreads; i++) {

    Future<PaymentResponse> future = executor.submit(() -> {

     ready.countDown();

     // Wait until all threads are ready
     start.await();

     return store.executeOnce("payment-123", () -> {

      executionCount.incrementAndGet();

      try {
       // Simulate a little processing time
       Thread.sleep(100);
      } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       throw new RuntimeException(e);
      }

      return new PaymentResponse(
              "payment-001",
              "order-100",
              BigDecimal.valueOf(500),
              "APPROVED"
      );
     });
    });

    futures.add(future);
   }

   // Wait until all 100 threads are ready
   assertTrue(ready.await(5, TimeUnit.SECONDS));

   // Release all threads together
   start.countDown();

   List<PaymentResponse> responses = new ArrayList<>();

   for (Future<PaymentResponse> future : futures) {
    responses.add(future.get(10, TimeUnit.SECONDS));
   }

   // Verify operation executed only once
   assertEquals(1, executionCount.get());

   // Verify all callers received the same payment ID
   assertTrue(
           responses.stream()
                   .allMatch(r -> r.paymentId()
                           .equals("payment-001"))
   );

  } finally {
   start.countDown();
   executor.shutdownNow();
  }
 }
}
