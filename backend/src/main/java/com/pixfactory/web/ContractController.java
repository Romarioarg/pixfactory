package com.pixfactory.web;

import com.pixfactory.service.ContractService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/contracts", "/api/contratos"})
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return contractService.findAll();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return contractService.find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return contractService.create(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return contractService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contractService.delete(id);
    }

    @PostMapping("/{id}/actions/{action}")
    public Map<String, Object> action(@PathVariable Long id, @PathVariable String action, @RequestBody(required = false) Map<String, Object> body) {
        return contractService.action(id, action, body == null ? Map.of() : body);
    }

    @PostMapping("/{id}/payments")
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String method = String.valueOf(body.getOrDefault("method", "PIX"));
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("valor")));
        return contractService.createPayment(id, method, amount);
    }
}
