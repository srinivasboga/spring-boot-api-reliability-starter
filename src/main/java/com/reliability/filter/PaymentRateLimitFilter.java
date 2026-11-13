package com.reliability.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliability.util.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PaymentRateLimitFilter extends OncePerRequestFilter {

    private static final String PAYMENT_PATH = "/api/payments";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final InMemoryTokenBucketRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public PaymentRateLimitFilter(
            InMemoryTokenBucketRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            @Value("${reliability.rate-limit.enabled:true}") boolean enabled
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath == null || servletPath.isEmpty()) {
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            servletPath = requestUri.substring(contextPath.length());
        }

        return !enabled
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !PAYMENT_PATH.equals(servletPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitDecision decision = rateLimiter.tryAcquire(
                request.getRemoteAddr()
        );

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitProblem(request, response, decision.retryAfterSeconds());
    }

    private void writeRateLimitProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(RETRY_AFTER_HEADER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Too many requests");
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("detail", "Request rate limit exceeded");
        body.put("instance", request.getRequestURI());
        body.put("code", "RATE_LIMIT_EXCEEDED");
        body.put(
                "correlationId",
                request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE)
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
