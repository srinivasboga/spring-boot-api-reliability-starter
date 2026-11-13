
package com.reliability.filter;

import com.reliability.util.CorrelationIdContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSameCorrelationIdInResponse() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .header(CorrelationIdContext.HEADER_NAME,
                                "integration-test-123")
                        .header("Idempotency-Key", "integration-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": "ORDER-101",
                                    "amount": 500
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(header().string(
                        CorrelationIdContext.HEADER_NAME,
                        "integration-test-123"
                ));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "integration-key-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": "ORDER-102",
                                    "amount": 750
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        CorrelationIdContext.HEADER_NAME,
                        matchesPattern("^[0-9a-fA-F-]{36}$")
                ));
    }

    @Test
    void repeatedPaymentShouldBeMarkedAsReplayed()
            throws Exception {

        String requestBody = """
                {"orderId":"ORDER-107","amount":99}
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "replay-indicator-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "replay-indicator-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"));
    }

    @Test
    void idempotencyConflictShouldReturnProblemDetailWithCorrelationId()
            throws Exception {

        String key = "problem-conflict-key";
        String correlationId = "problem-conflict-trace";

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORDER-103","amount":500}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .header(CorrelationIdContext.HEADER_NAME, correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORDER-104","amount":500}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Idempotency key conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code")
                        .value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.correlationId").value(correlationId));
    }

    @Test
    void invalidPaymentShouldReturnProblemDetailWithFieldErrors()
            throws Exception {

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "problem-validation-key")
                        .header(CorrelationIdContext.HEADER_NAME,
                                "problem-validation-trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"","amount":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.correlationId")
                        .value("problem-validation-trace"))
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void missingIdempotencyKeyShouldReturnProblemDetail()
            throws Exception {

        mockMvc.perform(post("/api/payments")
                        .header(CorrelationIdContext.HEADER_NAME,
                                "problem-missing-header-trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORDER-105","amount":50}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("MISSING_REQUIRED_HEADER"))
                .andExpect(jsonPath("$.correlationId")
                        .value("problem-missing-header-trace"));
    }

    @Test
    void blankIdempotencyKeyShouldReturnProblemDetail()
            throws Exception {

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", " ")
                        .header(CorrelationIdContext.HEADER_NAME,
                                "problem-blank-key-trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORDER-106","amount":50}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_IDEMPOTENCY_KEY"))
                .andExpect(jsonPath("$.correlationId")
                        .value("problem-blank-key-trace"));
    }
}
