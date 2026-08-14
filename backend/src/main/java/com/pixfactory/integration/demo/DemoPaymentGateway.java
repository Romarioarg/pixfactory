package com.pixfactory.integration.demo;

import com.pixfactory.domain.Payment;
import com.pixfactory.integration.PaymentGateway;
import com.pixfactory.integration.PixService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Primary
public class DemoPaymentGateway implements PaymentGateway {
    private final PixService pixService;

    public DemoPaymentGateway(PixService pixService) {
        this.pixService = pixService;
    }

    @Override
    public Payment charge(Long contractId, String method, BigDecimal amount) {
        String normalized = method == null ? "PIX" : method.toUpperCase();
        if ("PIX".equals(normalized)) {
            return pixService.createCharge(contractId, amount);
        }
        Payment payment = pixService.createCharge(contractId, amount);
        payment.setMethod(normalized);
        payment.setQrPayload(null);
        payment.setCopyPaste("DEMO-" + normalized + "-" + payment.getTxid());
        return payment;
    }
}
