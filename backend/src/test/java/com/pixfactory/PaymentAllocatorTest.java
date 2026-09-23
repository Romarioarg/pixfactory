package com.pixfactory;

import com.pixfactory.service.PaymentAllocator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAllocatorTest {
    @Test
    void defaultOrderPaysInterestFineChargesThenPrincipal() {
        Map<String, BigDecimal> due = Map.of(
                "juros", new BigDecimal("200.00"),
                "multa", new BigDecimal("50.00"),
                "encargos", new BigDecimal("30.00"),
                "principal", new BigDecimal("1000.00")
        );
        Map<String, BigDecimal> first = PaymentAllocator.allocate(new BigDecimal("200"), due, null);
        assertThat(first.get("juros")).isEqualByComparingTo("200.00");
        assertThat(first.get("multa")).isEqualByComparingTo("0.00");
        assertThat(first.get("principal")).isEqualByComparingTo("0.00");

        Map<String, BigDecimal> second = PaymentAllocator.allocate(new BigDecimal("280"), due, "juros,multa,encargos,principal");
        assertThat(second.get("juros")).isEqualByComparingTo("200.00");
        assertThat(second.get("multa")).isEqualByComparingTo("50.00");
        assertThat(second.get("encargos")).isEqualByComparingTo("30.00");
        assertThat(second.get("principal")).isEqualByComparingTo("0.00");

        Map<String, BigDecimal> full = PaymentAllocator.allocate(new BigDecimal("1280"), due, "juros,multa,encargos,principal");
        assertThat(full.get("principal")).isEqualByComparingTo("1000.00");
    }

    @Test
    void customOrderCanPayPrincipalFirst() {
        Map<String, BigDecimal> due = Map.of(
                "juros", new BigDecimal("200.00"),
                "multa", new BigDecimal("50.00"),
                "encargos", BigDecimal.ZERO,
                "principal", new BigDecimal("500.00")
        );
        Map<String, BigDecimal> allocated = PaymentAllocator.allocate(new BigDecimal("500"), due, "principal,juros,multa,encargos");
        assertThat(allocated.get("principal")).isEqualByComparingTo("500.00");
        assertThat(allocated.get("juros")).isEqualByComparingTo("0.00");
    }
}
