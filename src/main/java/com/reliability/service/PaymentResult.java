package com.reliability.service;

import com.reliability.model.PaymentResponse;

public record PaymentResult(PaymentResponse response, boolean replayed) {}
