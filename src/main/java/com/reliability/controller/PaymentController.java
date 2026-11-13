package com.reliability.controller;
import com.reliability.model.PaymentRequest;
import com.reliability.model.PaymentResponse;
import com.reliability.service.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
 private final IdempotencyService service;
 public PaymentController(IdempotencyService service) { this.service = service; }
 @PostMapping
 public ResponseEntity<PaymentResponse> create(
  @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PaymentRequest request) {
  if (key.isBlank()) {
   throw new InvalidIdempotencyKeyException(
           "Idempotency-Key header must not be blank"
   );
  }
  return ResponseEntity.status(HttpStatus.CREATED).body(service.processPayment(key, request));
 }
}
