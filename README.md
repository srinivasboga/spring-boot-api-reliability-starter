# Spring Boot API Reliability Starter
A learning-driven open-source project exploring reusable reliability patterns for Spring Boot APIs.

- Java 17
- Spring Boot 3.5.6
- Maven

## First feature: idempotent payment endpoint
`POST /api/payments` accepts an `Idempotency-Key` header. Repeating a request with the same key returns the original response rather than executing the operation again.

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

`ConcurrentHashMap.computeIfAbsent` ensures same-key computation is atomic within one JVM.

## Limitations
This is a first learning milestone, not production-ready payment infrastructure. Data is lost on restart; separate instances do not share keys; expiry and request-payload fingerprint validation are not implemented. A real payment integration must coordinate idempotency with the payment provider or durable transaction storage.

Next: payload fingerprint validation, TTL, Redis adapter, concurrency tests and Spring Boot auto-configuration.
