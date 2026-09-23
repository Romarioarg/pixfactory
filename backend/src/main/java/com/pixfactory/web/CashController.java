package com.pixfactory.web;

import com.pixfactory.domain.User;
import com.pixfactory.service.CashService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caixa")
public class CashController {
    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @GetMapping
    public List<Map<String, Object>> history() {
        return cashService.history();
    }

    @GetMapping("/hoje")
    public Map<String, Object> today() {
        return cashService.today();
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> open(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        BigDecimal opening = new BigDecimal(String.valueOf(body.getOrDefault("saldoInicial", 0)));
        String operator = user == null ? "operador" : user.getName();
        return cashService.open(opening, operator);
    }

    @PostMapping("/fechar")
    public Map<String, Object> close(@RequestBody Map<String, Object> body) {
        BigDecimal informed = new BigDecimal(String.valueOf(body.get("saldoInformado")));
        String notes = body.get("justificativa") == null ? null : String.valueOf(body.get("justificativa"));
        return cashService.close(informed, notes);
    }

    @PostMapping("/movimentos")
    public Map<String, Object> movement(@RequestBody Map<String, Object> body) {
        return cashService.addMovement(
                String.valueOf(body.getOrDefault("tipo", "entrada")),
                new BigDecimal(String.valueOf(body.get("valor"))),
                body.get("origem") == null ? "manual" : String.valueOf(body.get("origem")),
                body.get("descricao") == null ? "" : String.valueOf(body.get("descricao")),
                null,
                null
        );
    }
}
