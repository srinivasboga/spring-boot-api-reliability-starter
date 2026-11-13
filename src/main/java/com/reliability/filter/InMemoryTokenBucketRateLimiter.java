package com.reliability.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryTokenBucketRateLimiter {

    private static final int MAX_STRIPE_COUNT = 64;

    private final Stripe[] stripes;
    private final double requestsPerSecond;
    private final int burstCapacity;

    public InMemoryTokenBucketRateLimiter(
            @Value("${reliability.rate-limit.requests-per-second:10}")
            double requestsPerSecond,
            @Value("${reliability.rate-limit.burst-capacity:20}")
            int burstCapacity,
            @Value("${reliability.rate-limit.max-tracked-clients:10000}")
            int maxTrackedClients
    ) {
        if (!Double.isFinite(requestsPerSecond) || requestsPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "Rate limit requests-per-second must be positive"
            );
        }
        if (burstCapacity <= 0 || maxTrackedClients <= 0) {
            throw new IllegalArgumentException(
                    "Rate limit burst capacity and max clients must be positive"
            );
        }

        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
        int stripeCount = 1;
        while (stripeCount < MAX_STRIPE_COUNT
                && stripeCount * 2 <= maxTrackedClients) {
            stripeCount *= 2;
        }
        stripes = new Stripe[stripeCount];
        int clientsPerStripe = maxTrackedClients / stripeCount;
        int extraClients = maxTrackedClients % stripeCount;
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new Stripe(
                    clientsPerStripe + (i < extraClients ? 1 : 0)
            );
        }
    }

    public RateLimitDecision tryAcquire(String clientKey) {
        int hash = clientKey.hashCode();
        int spreadHash = hash ^ (hash >>> 16);
        return stripes[spreadHash & (stripes.length - 1)].tryAcquire(
                clientKey,
                System.nanoTime(),
                requestsPerSecond,
                burstCapacity
        );
    }

    private static final class Stripe {

        private final int maxClients;
        private final Map<String, Bucket> buckets =
                new LinkedHashMap<>(16, 0.75f, true);

        private Stripe(int maxClients) {
            this.maxClients = maxClients;
        }

        private synchronized RateLimitDecision tryAcquire(
                String clientKey,
                long now,
                double requestsPerSecond,
                int burstCapacity
        ) {
            Bucket bucket = buckets.get(clientKey);
            if (bucket == null) {
                if (buckets.size() >= maxClients) {
                    String leastRecentlyUsed = buckets.keySet()
                            .iterator()
                            .next();
                    buckets.remove(leastRecentlyUsed);
                }
                bucket = new Bucket(burstCapacity, now);
                buckets.put(clientKey, bucket);
            }

            double elapsedSeconds = Math.max(0L, now - bucket.lastRefillNanos)
                    / 1_000_000_000.0;
            bucket.tokens = Math.min(
                    burstCapacity,
                    bucket.tokens + elapsedSeconds * requestsPerSecond
            );
            bucket.lastRefillNanos = now;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return new RateLimitDecision(true, 0);
            }

            long retryAfter = Math.max(
                    1L,
                    (long) Math.ceil((1.0 - bucket.tokens) / requestsPerSecond)
            );
            return new RateLimitDecision(false, retryAfter);
        }
    }

    private static final class Bucket {

        private double tokens;
        private long lastRefillNanos;

        private Bucket(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillNanos = now;
        }
    }
}
