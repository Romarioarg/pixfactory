package com.pixfactory.qa;

import com.pixfactory.exception.ApiException;
import com.pixfactory.exception.ConflictException;
import com.pixfactory.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionQaTest {
    @Test
    void conflictIsHttp409() {
        ConflictException ex = new ConflictException("Já existe um cliente com este CPF.");
        assertThat(ex.getStatus()).isEqualTo(409);
        assertThat(ex.getMessage()).contains("CPF");
    }

    @Test
    void notFoundIsHttp404() {
        NotFoundException ex = new NotFoundException("Cliente não encontrado.");
        assertThat(ex.getStatus()).isEqualTo(404);
    }

    @Test
    void genericApiExceptionKeepsStatus() {
        ApiException ex = new ApiException(401, "E-mail ou senha incorretos.");
        assertThat(ex.getStatus()).isEqualTo(401);
        assertThat(ex.getMessage()).contains("senha");
    }
}
