
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
}
