package com.pixfactory.integration.demo;

import com.pixfactory.domain.Payment;
import com.pixfactory.domain.PaymentStatus;
import com.pixfactory.integration.PixService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Primary
public class DemoPixService implements PixService {
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Payment createCharge(Long contractId, BigDecimal amount) {
        int n = sequence.getAndIncrement();
        String txid = "PIX-DEMO-" + LocalDate.now().getYear() + "-" + String.format("%05d", n);
        Payment payment = new Payment();
        payment.setContractId(contractId);
        payment.setMethod("PIX");
        payment.setTxid(txid);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider("DEMO");
        payment.setCopyPaste("00020126580014br.gov.bcb.pix0136" + txid + "520400005303986540" + amount.toPlainString());
        payment.setQrPayload("PIX DEMO|" + txid + "|BRL|" + amount.toPlainString());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    @Override
    public Payment confirm(Payment payment) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    @Override
    public Payment fail(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    @Override
    public Payment cancel(Payment payment) {
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(Instant.now());
        return payment;
    }
}
