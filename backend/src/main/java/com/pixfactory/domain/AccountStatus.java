package com.pixfactory.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {
    ATIVO("ativo"),
    INATIVO("inativo");

    private final String code;

    AccountStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AccountStatus from(String value) {
        if (value == null) {
            return ATIVO;
        }
        for (AccountStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de conta inválido: " + value);
    }
}
