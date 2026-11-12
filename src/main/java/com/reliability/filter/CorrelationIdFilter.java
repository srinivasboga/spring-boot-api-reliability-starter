
package com.reliability.filter;

import com.reliability.util.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        request.setAttribute(
                CorrelationIdContext.REQUEST_ATTRIBUTE,
                correlationId
        );

        response.setHeader(
                CorrelationIdContext.HEADER_NAME,
                correlationId
        );

        MDC.put(CorrelationIdContext.MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {

        String incomingId = request.getHeader(
                CorrelationIdContext.HEADER_NAME
        );

        if (incomingId != null
                && VALID_CORRELATION_ID.matcher(incomingId).matches()) {
            return incomingId;
        }

        return UUID.randomUUID().toString();
    }
}
