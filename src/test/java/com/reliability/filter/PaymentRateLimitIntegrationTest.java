package com.reliability.filter;

import com.reliability.util.CorrelationIdContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "reliability.rate-limit.enabled=true",
        "reliability.rate-limit.requests-per-second=0.1",
        "reliability.rate-limit.burst-capacity=1"
})
@AutoConfigureMockMvc
class PaymentRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectExcessRequestsWithRetryAfterAndCorrelationId()
            throws Exception {
        String requestBody = """
                {"orderId":"ORDER-RATE-1","amount":25}
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "rate-limit-allowed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "rate-limit-rejected")
                        .header(CorrelationIdContext.HEADER_NAME,
                                "rate-limit-correlation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "10"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.correlationId")
                        .value("rate-limit-correlation"));
    }
}
