package com.reliability.util;


public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Correlation-ID";

    public static final String MDC_KEY = "correlationId";

    public static final String REQUEST_ATTRIBUTE = "correlationId";

    private CorrelationIdContext() {
        // Utility class - prevent instantiation
    }
}
