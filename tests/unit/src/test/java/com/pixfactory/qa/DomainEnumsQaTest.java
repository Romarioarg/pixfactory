package com.pixfactory.qa;

import com.pixfactory.domain.AccountStatus;
import com.pixfactory.domain.PaymentStatus;
import com.pixfactory.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEnumsQaTest {
    @Test
    void accountStatusDefaultsToActive() {
        assertThat(AccountStatus.from(null)).isEqualTo(AccountStatus.ATIVO);
        assertThat(AccountStatus.from("inativo")).isEqualTo(AccountStatus.INATIVO);
        assertThat(AccountStatus.ATIVO.getCode()).isEqualTo("ativo");
    }

    @Test
    void accountStatusRejectsUnknown() {
        assertThatThrownBy(() -> AccountStatus.from("bloqueado"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paymentStatusHasDemoLifecycle() {
        assertThat(PaymentStatus.valueOf("PENDING")).isEqualTo(PaymentStatus.PENDING);
        assertThat(PaymentStatus.PAID.name()).isEqualTo("PAID");
        assertThat(PaymentStatus.FAILED).isNotEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void rolesAreAdminAndUser() {
        assertThat(Role.values()).containsExactly(Role.ADMIN, Role.USER);
    }
}
