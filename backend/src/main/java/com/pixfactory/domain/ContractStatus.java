package com.pixfactory.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractStatus {
    ATIVO("ativo"),
    ATRASADO("atrasado"),
    PENDENTE("pendente"),
    ENCERRADO("encerrado"),
    RENEGOCIADO("renegociado"),
    ACORDO("acordo"),
    HOLD("hold"),
    FALECIMENTO("falecimento");

    private final String code;

    ContractStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ContractStatus from(String value) {
        if (value == null) {
            return PENDENTE;
        }
        for (ContractStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de contrato inválido: " + value);
    }
}
