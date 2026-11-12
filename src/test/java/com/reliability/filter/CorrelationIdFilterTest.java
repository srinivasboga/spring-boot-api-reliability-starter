
package com.reliability.filter;

import com.reliability.util.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanup() {
        MDC.remove(CorrelationIdContext.MDC_KEY);
    }

    @Test
    void shouldPreserveValidCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                CorrelationIdContext.HEADER_NAME,
                "payment-12345"
        );

        FilterChain chain = (req, res) -> {
            assertEquals(
                    "payment-12345",
                    MDC.get(CorrelationIdContext.MDC_KEY)
            );
        };

        filter.doFilter(request, response, chain);

        assertEquals(
                "payment-12345",
                response.getHeader(CorrelationIdContext.HEADER_NAME)
        );

        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String correlationId = response.getHeader(
                CorrelationIdContext.HEADER_NAME
        );

        assertNotNull(correlationId);
        assertTrue(correlationId.matches(
                "^[0-9a-fA-F-]{36}$"
        ));
    }

    @Test
    void shouldReplaceInvalidCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                CorrelationIdContext.HEADER_NAME,
                "invalid correlation id!"
        );

        filter.doFilter(request, response, (req, res) -> {});

        String correlationId = response.getHeader(
                CorrelationIdContext.HEADER_NAME
        );

        assertNotEquals("invalid correlation id!", correlationId);
        assertTrue(correlationId.matches(
                "^[0-9a-fA-F-]{36}$"
        ));
    }

    @Test
    void shouldCleanMdcWhenFilterChainThrowsException()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                CorrelationIdContext.HEADER_NAME,
                "payment-error-123"
        );

        FilterChain chain = (req, res) -> {
            assertEquals(
                    "payment-error-123",
                    MDC.get(CorrelationIdContext.MDC_KEY)
            );

            throw new ServletException("Simulated failure");
        };

        assertThrows(
                ServletException.class,
                () -> filter.doFilter(request, response, chain)
        );

        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }
}
