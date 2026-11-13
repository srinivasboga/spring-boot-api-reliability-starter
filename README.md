# Spring Boot API Reliability Starter
A learning-driven open-source project exploring reusable reliability patterns for Spring Boot APIs.

- Java 17
- Spring Boot 3.5.6
- Maven

## Features
- `POST /api/payments` accepts an `Idempotency-Key` header. Repeating the same request with the same key returns the original response; reusing a key with a different payload returns `409 Conflict`.
- Idempotency records expire after a configurable TTL (`reliability.idempotency.ttl`, default `24h`).
- New payment responses include `Idempotency-Replayed: false`; retries that return the stored response include `Idempotency-Replayed: true`.
- A per-client token bucket limits payment requests. The default allows a burst of 20 and refills at 10 requests per second; excess requests receive `429 Too Many Requests` and a `Retry-After` header.
- Request payloads are fingerprinted with SHA-256 so semantically different requests cannot reuse a key.
- Correlation IDs are accepted or generated for every request, returned in `X-Correlation-Id`, and included in application logs.
- API errors use RFC Problem Details responses with stable error codes and the request correlation ID. Validation errors include field-level details.

## Run
Requirements: JDK 17+ and Maven 3.9+.
```bash
mvn test
mvn spring-boot:run
```

## Try it
```bash
curl -i -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" -H "Idempotency-Key: payment-001" -d '{"orderId":"order-1001","amount":149.99}'
```
Repeat the request with the same key and compare `paymentId`.

## Architecture
- `controller`: HTTP boundary and validation
- `service`: application use case
- `store`: storage abstraction and in-memory adapter
- `model`: request and response records

`ConcurrentHashMap.compute` ensures same-key computation and TTL replacement are atomic within one JVM.

Rate limiting can be configured with `reliability.rate-limit.enabled`, `reliability.rate-limit.requests-per-second`, `reliability.rate-limit.burst-capacity`, and `reliability.rate-limit.max-tracked-clients`. Buckets use the servlet remote address and are kept in memory per instance.

## Limitations
This is a learning project, not production-ready payment infrastructure. Idempotency and rate-limit state are held in memory, so it is lost on restart and is not shared across instances. Deployments behind a proxy should configure the servlet container to resolve client addresses from trusted proxy metadata. A real payment integration must coordinate idempotency with the payment provider or durable transaction storage.

Next: Redis-backed storage and Spring Boot auto-configuration.
