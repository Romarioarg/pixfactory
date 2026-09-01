package com.pixfactory.qa;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OptionalFinanceQaTest {
    @Test
    void loanEngineInterestRulesWhenClassExists() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("com.pixfactory.service.LoanEngine");
        } catch (ClassNotFoundException ex) {
            assumeTrue(false, "LoanEngine ainda não está neste branch");
            return;
        }
        Method method = type.getMethod(
                "interestAmount",
                BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class, String.class
        );
        BigDecimal original = (BigDecimal) method.invoke(
                null,
                new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("20"), BigDecimal.ZERO, "principal_original"
        );
        BigDecimal saldo = (BigDecimal) method.invoke(
                null,
                new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("20"), BigDecimal.ZERO, "saldo"
        );
        assertThat(original).isEqualByComparingTo("200.00");
        assertThat(saldo).isEqualByComparingTo("100.00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void paymentAllocatorOrderWhenClassExists() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("com.pixfactory.service.PaymentAllocator");
        } catch (ClassNotFoundException ex) {
            assumeTrue(false, "PaymentAllocator ainda não está neste branch");
            return;
        }
        Method method = type.getMethod("allocate", BigDecimal.class, Map.class, String.class);
        Map<String, BigDecimal> due = Map.of(
                "juros", new BigDecimal("200.00"),
                "multa", new BigDecimal("50.00"),
                "encargos", new BigDecimal("30.00"),
                "principal", new BigDecimal("1000.00")
        );
        Map<String, BigDecimal> applied = (Map<String, BigDecimal>) method.invoke(null, new BigDecimal("200"), due, null);
        assertThat(applied.get("juros")).isEqualByComparingTo("200.00");
        assertThat(applied.get("principal")).isEqualByComparingTo("0.00");
    }
}
