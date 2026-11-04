package com.reliability.service;


import com.reliability.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RequestFingerprint {

    public String generate(PaymentRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Normalize amount so 500.0 and 500.00 are treated equally.
            String normalizedAmount =
                    request.amount().stripTrailingZeros().toPlainString();

            // Include the order ID length to make the representation unambiguous.
            String canonicalRequest =
                    request.orderId().length() + ":" +
                            request.orderId() + ":" +
                            normalizedAmount;

            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", exception
            );
        }
    }
}