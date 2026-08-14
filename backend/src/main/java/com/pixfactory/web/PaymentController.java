package com.pixfactory.web;

import com.pixfactory.service.ContractService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final ContractService contractService;

    public PaymentController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return contractService.payments();
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        return contractService.confirmPayment(id);
    }

    @PostMapping("/{id}/fail")
    public Map<String, Object> fail(@PathVariable Long id) {
        return contractService.failPayment(id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Long id) {
        return contractService.cancelPayment(id);
    }
}
