package com.pixfactory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aplica um pagamento na ordem configurada do contrato.
 * Padrão: juros vencidos → multa → encargos → principal.
 */
public final class PaymentAllocator {
    public static final String DEFAULT_ORDER = "juros,multa,encargos,principal";

    private PaymentAllocator() {
    }

    public static Map<String, BigDecimal> allocate(BigDecimal amount, Map<String, BigDecimal> dueByComponent, String order) {
        BigDecimal leftover = nz(amount).setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> applied = new LinkedHashMap<>();
        applied.put("juros", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        applied.put("multa", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        applied.put("encargos", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        applied.put("principal", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        String sequence = order == null || order.isBlank() ? DEFAULT_ORDER : order;
        for (String raw : sequence.split(",")) {
            String part = normalize(raw);
            if (!applied.containsKey(part)) {
                continue;
            }
            BigDecimal due = nz(dueByComponent.get(part)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (due.signum() <= 0 || leftover.signum() <= 0) {
                continue;
            }
            BigDecimal take = leftover.min(due);
            applied.put(part, applied.get(part).add(take));
            leftover = leftover.subtract(take);
        }
        if (leftover.signum() > 0) {
            applied.put("principal", applied.get("principal").add(leftover));
        }
        return applied;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String part = raw.trim().toLowerCase();
        return switch (part) {
            case "juro", "juros_vencidos", "interest" -> "juros";
            case "mora", "encargo", "charges" -> "encargos";
            case "capital", "amortizacao" -> "principal";
            case "fine", "penalty" -> "multa";
            default -> part;
        };
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
