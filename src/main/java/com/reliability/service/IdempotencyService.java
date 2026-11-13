package com.reliability.service;

import com.reliability.model.IdempotencyRecord;
import com.reliability.model.IdempotencyExecution;
import com.reliability.model.PaymentRequest;
import com.reliability.model.PaymentResponse;
import com.reliability.store.IdempotencyStore;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

 private final IdempotencyStore store;
 private final RequestFingerprint fingerprintGenerator;

 public IdempotencyService(
         IdempotencyStore store,
         RequestFingerprint fingerprintGenerator
 ) {
  this.store = store;
  this.fingerprintGenerator = fingerprintGenerator;
 }

 public PaymentResponse processPayment(
         String idempotencyKey,
         PaymentRequest request
 ) {
  return processPaymentWithMetadata(idempotencyKey, request).response();
 }

 public PaymentResult processPaymentWithMetadata(
         String idempotencyKey,
         PaymentRequest request
 ) {
  String fingerprint = fingerprintGenerator.generate(request);

  IdempotencyExecution execution = store.executeOnceWithMetadata(
          idempotencyKey,
          () -> new IdempotencyRecord(
                  fingerprint,
                  PaymentResponse.approved(request)
          )
  );
  IdempotencyRecord record = execution.record();

  if (!record.fingerprint().equals(fingerprint)) {
   throw new IdempotencyConflictException(
           "Idempotency key was already used with a different request"
   );
  }

  return new PaymentResult(record.response(), execution.replayed());
 }
}
