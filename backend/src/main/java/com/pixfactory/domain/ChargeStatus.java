package com.pixfactory.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChargeStatus {
    PENDENTE("pendente"),
    PAGO("pago"),
    ATRASADO("atrasado"),
    PARCIAL("parcial"),
    CANCELADO("cancelado"),
    RENEGOCIADA("renegociada");

    private final String code;

    ChargeStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ChargeStatus from(String value) {
        if (value == null) {
            return PENDENTE;
        }
        for (ChargeStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de cobrança inválido: " + value);
    }
}
