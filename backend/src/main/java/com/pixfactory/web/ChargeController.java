package com.pixfactory.web;

import com.pixfactory.service.ChargeService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/cobrancas", "/api/charges"})
public class ChargeController {
    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String data,
            @RequestParam(required = false) String status
    ) {
        if (data != null && !data.isBlank()) {
            return chargeService.findByDate(LocalDate.parse(data));
        }
        List<Map<String, Object>> all = chargeService.findAll();
        if (status == null || status.isBlank()) {
            return all;
        }
        return all.stream().filter(item -> status.equalsIgnoreCase(String.valueOf(item.get("status")))).toList();
    }

    @GetMapping("/atrasadas")
    public List<Map<String, Object>> overdue() {
        return chargeService.overdue();
    }

    @GetMapping("/follow-up")
    public List<Map<String, Object>> followUp(@RequestParam(required = false) String data) {
        return chargeService.followUps(data == null || data.isBlank() ? LocalDate.now() : LocalDate.parse(data));
    }

    @GetMapping("/juros-periodo")
    public List<Map<String, Object>> interestPeriod() {
        chargeService.refreshOverdueAndPromises();
        return chargeService.interestPeriodItems();
    }

    @GetMapping("/{id:\\d+}")
    public Map<String, Object> get(@PathVariable Long id) {
        return chargeService.find(id);
    }

    @PostMapping("/{id:\\d+}/pagamentos")
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("valor")));
        return chargeService.registerPayment(
                id,
                amount,
                body.get("forma") == null ? (body.get("formaPagamento") == null ? null : String.valueOf(body.get("formaPagamento"))) : String.valueOf(body.get("forma")),
                body.get("excedente") == null ? (body.get("excedenteDestino") == null ? null : String.valueOf(body.get("excedenteDestino"))) : String.valueOf(body.get("excedente"))
        );
    }

    @PostMapping("/{id:\\d+}/promessa")
    public Map<String, Object> promise(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LocalDate date = LocalDate.parse(String.valueOf(body.get("data")));
        BigDecimal amount = body.get("valor") == null ? null : new BigDecimal(String.valueOf(body.get("valor")));
        return chargeService.registerPromise(
                id,
                date,
                amount,
                body.get("observacao") == null ? null : String.valueOf(body.get("observacao")),
                body.get("responsavel") == null ? null : String.valueOf(body.get("responsavel"))
        );
    }

    @PostMapping("/{id:\\d+}/contato")
    public Map<String, Object> contact(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LocalDate followUp = null;
        if (body.get("proximoContato") != null && !String.valueOf(body.get("proximoContato")).isBlank()) {
            followUp = LocalDate.parse(String.valueOf(body.get("proximoContato")));
        }
        return chargeService.registerContact(
                id,
                body.get("canal") == null ? null : String.valueOf(body.get("canal")),
                body.get("resultado") == null ? null : String.valueOf(body.get("resultado")),
                body.get("observacao") == null ? null : String.valueOf(body.get("observacao")),
                followUp,
                body.get("proximaAcao") == null ? null : String.valueOf(body.get("proximaAcao"))
        );
    }

    @PostMapping("/{id:\\d+}/estorno")
    public Map<String, Object> reverse(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String motivo = body == null || body.get("motivo") == null ? null : String.valueOf(body.get("motivo"));
        return chargeService.reversePayment(id, motivo);
    }

    @GetMapping("/{id:\\d+}/recibo")
    public Map<String, Object> receipt(@PathVariable Long id) {
        return chargeService.receipt(id);
    }

    @GetMapping("/{id:\\d+}/extrato")
    public List<Map<String, Object>> ledger(@PathVariable Long id) {
        return chargeService.ledgerForCharge(id);
    }
}
