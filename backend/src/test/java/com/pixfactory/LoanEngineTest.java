package com.pixfactory;

import com.pixfactory.service.LoanEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEngineTest {
    @Test
    void interestOnOriginalThousandAtTwentyPercentIsTwoHundred() {
        BigDecimal juros = LoanEngine.interestAmount(
                new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("20"), BigDecimal.ZERO, "principal_original");
        assertThat(juros).isEqualByComparingTo("200.00");
    }

    @Test
    void interestOnRemainingBalanceAfterHalfPayoff() {
        BigDecimal juros = LoanEngine.interestAmount(
                new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("20"), BigDecimal.ZERO, "saldo");
        assertThat(juros).isEqualByComparingTo("100.00");
    }

    @Test
    void interestOnOriginalStaysTwoHundredWhenBalanceDrops() {
        BigDecimal juros = LoanEngine.interestAmount(
                new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("20"), BigDecimal.ZERO, "principal_original");
        assertThat(juros).isEqualByComparingTo("200.00");
    }

    @Test
    void fixedInterestIgnoresBalance() {
        BigDecimal juros = LoanEngine.interestAmount(
                new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("200"), "valor_fixo");
        assertThat(juros).isEqualByComparingTo("200.00");
    }

    @Test
    void openInterestSimulationUsesBaseCalculo() {
        Map<String, Object> original = LoanEngine.simulate(Map.of(
                "valor", 1000, "juros", 20, "parcelas", 1, "modo", "juros_rotativo",
                "baseCalculo", "principal_original", "saldo", 500));
        assertThat((BigDecimal) original.get("parcelaInicial")).isEqualByComparingTo("200.00");

        Map<String, Object> saldo = LoanEngine.simulate(Map.of(
                "valor", 1000, "juros", 20, "parcelas", 1, "modo", "juros_rotativo",
                "baseCalculo", "saldo", "saldo", 500));
        assertThat((BigDecimal) saldo.get("parcelaInicial")).isEqualByComparingTo("100.00");
    }
}
