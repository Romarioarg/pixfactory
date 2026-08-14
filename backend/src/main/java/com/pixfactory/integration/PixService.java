package com.pixfactory.integration;

import com.pixfactory.domain.Payment;

import java.math.BigDecimal;

public interface PixService {
    Payment createCharge(Long contractId, BigDecimal amount);
    Payment confirm(Payment payment);
    Payment fail(Payment payment);
    Payment cancel(Payment payment);
}
