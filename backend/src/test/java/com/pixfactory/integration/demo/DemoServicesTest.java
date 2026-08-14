package com.pixfactory.integration.demo;

import com.pixfactory.domain.Payment;
import com.pixfactory.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DemoServicesTest {
    @Test
    void pixDemoGeneratesTxidAndPendingStatus() {
        DemoPixService pix = new DemoPixService();
        Payment payment = pix.createCharge(1L, new BigDecimal("250.00"));
        assertThat(payment.getTxid()).startsWith("PIX-DEMO-");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getProvider()).isEqualTo("DEMO");
        assertThat(payment.getQrPayload()).contains(payment.getTxid());
        pix.confirm(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        pix.fail(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void openFinanceDemoExposesFictionalBalance() {
        DemoOpenFinanceService service = new DemoOpenFinanceService();
        assertThat(service.institutions()).isNotEmpty();
        var snapshot = service.snapshot("DEMOBANK");
        assertThat(snapshot.get("demo")).isEqualTo(true);
        assertThat(snapshot.get("account")).isNotNull();
    }
}
