package com.pixfactory.qa;

import com.pixfactory.domain.ContractStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractStatusQaTest {
    @Test
    void mapsPortugueseCodes() {
        assertThat(ContractStatus.from("atrasado")).isEqualTo(ContractStatus.ATRASADO);
        assertThat(ContractStatus.from("ENCERRADO")).isEqualTo(ContractStatus.ENCERRADO);
        assertThat(ContractStatus.from(null)).isEqualTo(ContractStatus.PENDENTE);
        assertThat(ContractStatus.ATIVO.getCode()).isEqualTo("ativo");
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> ContractStatus.from("liquidado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }
}
