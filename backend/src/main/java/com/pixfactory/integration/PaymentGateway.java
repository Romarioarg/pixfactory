package com.pixfactory.integration;

import com.pixfactory.domain.Payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    Payment charge(Long contractId, String method, BigDecimal amount);
}
