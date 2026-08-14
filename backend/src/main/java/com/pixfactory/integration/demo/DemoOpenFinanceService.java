package com.pixfactory.integration.demo;

import com.pixfactory.integration.OpenFinanceService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class DemoOpenFinanceService implements OpenFinanceService {

    @Override
    public List<Map<String, Object>> institutions() {
        return List.of(
                Map.of("code", "DEMOBANK", "name", "Banco Demo", "demo", true),
                Map.of("code", "NUBANK-DEMO", "name", "Nubank Demo", "demo", true),
                Map.of("code", "ITAU-DEMO", "name", "Itaú Demo", "demo", true)
        );
    }

    @Override
    public Map<String, Object> snapshot(String institutionCode) {
        String name = institutions().stream()
                .filter(i -> institutionCode.equals(i.get("code")))
                .map(i -> String.valueOf(i.get("name")))
                .findFirst()
                .orElse("Instituição Demo");
        return Map.of(
                "demo", true,
                "provider", "DEMO",
                "institution", name,
                "institutionCode", institutionCode,
                "account", Map.of(
                        "type", "CACC",
                        "agency", "0001",
                        "number", "12345-6",
                        "balance", new BigDecimal("8450.00")
                ),
                "transactions", List.of(
                        Map.of("date", LocalDate.now().minusDays(2).toString(), "description", "Pix recebido (demo)", "amount", 416.67),
                        Map.of("date", LocalDate.now().minusDays(5).toString(), "description", "Tarifa conta (demo)", "amount", -12.90)
                )
        );
    }
}
