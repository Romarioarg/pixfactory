package com.pixfactory.service;

import com.pixfactory.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras financeiras no backend. O frontend só exibe o resultado.
 */
public class LoanEngine {
    public static final RoundingMode MONEY = RoundingMode.HALF_UP;

    public record Installment(
            int numero,
            BigDecimal principal,
            BigDecimal juros,
            BigDecimal parcela,
            BigDecimal saldo,
            String tipo,
            LocalDate vencimento
    ) {}

    public static Map<String, Object> simulate(Map<String, Object> body) {
        BigDecimal principal = decimal(body.get("valor"));
        BigDecimal taxaPct = decimal(body.getOrDefault("juros", body.getOrDefault("taxa", 0)));
        int n = integer(body.getOrDefault("parcelas", body.getOrDefault("parcelasTotais", 1)));
        String sistema = str(body.get("sistema"), "price").toLowerCase();
        String modo = str(body.get("modo"), "parcela_cheia").toLowerCase();
        String tipo = str(body.get("tipo"), str(body.get("tipoOperacao"), "emprestimo")).toLowerCase();
        String periodicidade = str(body.get("periodicidade"), "mensal").toLowerCase();
        int carencia = integer(body.getOrDefault("carencia", 0));
        LocalDate first = body.get("primeiroVencimento") == null || String.valueOf(body.get("primeiroVencimento")).isBlank()
                ? LocalDate.now().plusMonths(1)
                : LocalDate.parse(String.valueOf(body.get("primeiroVencimento")));

        if (principal.signum() <= 0) {
            throw new ApiException(400, "Valor precisa ser maior que zero.");
        }
        String baseCalculo = str(body.get("baseCalculo"), str(body.get("base"), "saldo")).toLowerCase();
        BigDecimal jurosFixo = decimal(body.getOrDefault("jurosFixo", body.get("juroFixo")));
        BigDecimal saldoInformado = decimal(body.getOrDefault("saldo", body.get("principalRestante")));
        boolean openInterest = isOpenInterest(modo, sistema, tipo);
        if (!openInterest && n < 1) {
            throw new ApiException(400, "Quantidade de parcelas inválida.");
        }
        if (openInterest && n < 1) {
            n = 1;
        }
        if (taxaPct.signum() < 0) {
            throw new ApiException(400, "Taxa de juros inválida.");
        }

        if (tipo.contains("aluguel")) {
            sistema = "aluguel";
        } else if (openInterest) {
            sistema = "so_juros_aberto";
        } else if ("so_juros".equals(modo) || "somente_juros".equals(modo) || "apenas_juros".equals(modo)
                || "americano".equals(sistema) || "americano".equals(modo) || "bullet".equals(sistema)) {
            sistema = "so_juros";
        } else if ("composto".equals(sistema) || "price_composto".equals(sistema)) {
            sistema = "price";
        } else if ("sacre".equals(sistema)) {
            sistema = "sac";
        }

        BigDecimal rate = taxaPct.divide(BigDecimal.valueOf(100), 10, MONEY);
        BigDecimal remaining = saldoInformado.signum() > 0 ? saldoInformado : principal;
        BigDecimal juroAberto = interestAmount(principal, remaining, taxaPct, jurosFixo, baseCalculo);
        List<Installment> rows = switch (sistema) {
            case "sac" -> sac(principal, rate, n, first, periodicidade, carencia);
            case "simples" -> simples(principal, rate, n, first, periodicidade, carencia);
            case "so_juros" -> interestOnly(principal, rate, n, first, periodicidade, carencia);
            case "so_juros_aberto" -> interestUntilPrincipal(principal, juroAberto, n, first, periodicidade);
            case "aluguel" -> rent(principal, n, first, periodicidade);
            default -> price(principal, rate, n, first, periodicidade, carencia);
        };

        BigDecimal total = rows.stream().map(Installment::parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jurosTotal = rows.stream().map(Installment::juros).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sistema", sistema);
        result.put("modo", modo);
        result.put("tipo", tipo);
        result.put("periodicidade", periodicidade);
        result.put("principal", principal.setScale(2, MONEY));
        result.put("taxaPercentual", taxaPct);
        result.put("parcelas", n);
        result.put("carencia", carencia);
        result.put("parcelaInicial", rows.isEmpty() ? BigDecimal.ZERO : rows.get(0).parcela());
        result.put("totalPagar", total);
        result.put("totalJuros", jurosTotal);
        result.put("cronograma", rows.stream().map(LoanEngine::toMap).toList());
        result.put("prazoAberto", openInterest || "so_juros_aberto".equals(sistema));
        result.put("baseCalculo", baseCalculo);
        result.put("jurosFixo", jurosFixo.setScale(2, MONEY));
        result.put("explicacao", explain(sistema, modo, tipo, baseCalculo));
        return result;
    }

    public static BigDecimal interestAmount(
            BigDecimal originalPrincipal,
            BigDecimal remainingPrincipal,
            BigDecimal taxaPct,
            BigDecimal jurosFixo,
            String baseCalculo
    ) {
        String base = baseCalculo == null ? "saldo" : baseCalculo.toLowerCase().trim();
        if ("valor_fixo".equals(base) || "fixo".equals(base) || "valor".equals(base)) {
            return money(jurosFixo == null ? BigDecimal.ZERO : jurosFixo);
        }
        BigDecimal taxa = taxaPct == null ? BigDecimal.ZERO : taxaPct;
        BigDecimal origem = ("principal_original".equals(base) || "original".equals(base) || "principal".equals(base))
                ? (originalPrincipal == null ? BigDecimal.ZERO : originalPrincipal)
                : (remainingPrincipal == null ? BigDecimal.ZERO : remainingPrincipal);
        return origem.multiply(taxa).divide(BigDecimal.valueOf(100), 2, MONEY);
    }

    public static List<Installment> scheduleFromContract(
            BigDecimal principal,
            BigDecimal taxaPct,
            int n,
            String sistema,
            String modo,
            String tipo,
            String periodicidade,
            int carencia,
            LocalDate first
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valor", principal);
        body.put("juros", taxaPct);
        body.put("parcelas", n);
        body.put("sistema", sistema);
        body.put("modo", modo);
        body.put("tipo", tipo);
        body.put("periodicidade", periodicidade);
        body.put("carencia", carencia);
        body.put("primeiroVencimento", first == null ? null : first.toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cronograma = (List<Map<String, Object>>) simulate(body).get("cronograma");
        List<Installment> rows = new ArrayList<>();
        for (Map<String, Object> row : cronograma) {
            rows.add(new Installment(
                    ((Number) row.get("numero")).intValue(),
                    (BigDecimal) row.get("principal"),
                    (BigDecimal) row.get("juros"),
                    (BigDecimal) row.get("parcela"),
                    (BigDecimal) row.get("saldo"),
                    String.valueOf(row.get("tipo")),
                    row.get("vencimento") == null || String.valueOf(row.get("vencimento")).isBlank()
                            ? null
                            : LocalDate.parse(String.valueOf(row.get("vencimento")))
            ));
        }
        return rows;
    }

    private static List<Installment> price(BigDecimal p, BigDecimal i, int n, LocalDate first, String period, int grace) {
        List<Installment> rows = new ArrayList<>();
        BigDecimal saldo = p;
        BigDecimal parcela = i.signum() == 0
                ? p.divide(BigDecimal.valueOf(n), 2, MONEY)
                : p.multiply(i).multiply(pow(BigDecimal.ONE.add(i), n))
                .divide(pow(BigDecimal.ONE.add(i), n).subtract(BigDecimal.ONE), 2, MONEY);
        for (int k = 1; k <= n; k++) {
            LocalDate due = shift(first, k - 1, period);
            if (k <= grace) {
                BigDecimal juros = money(saldo.multiply(i));
                rows.add(new Installment(k, BigDecimal.ZERO, juros, juros, saldo, "carencia", due));
                continue;
            }
            BigDecimal juros = money(saldo.multiply(i));
            BigDecimal amort = k == n ? saldo : money(parcela.subtract(juros).max(BigDecimal.ZERO));
            if (amort.compareTo(saldo) > 0) {
                amort = saldo;
            }
            BigDecimal valor = money(amort.add(juros));
            saldo = money(saldo.subtract(amort).max(BigDecimal.ZERO));
            rows.add(new Installment(k, amort, juros, valor, saldo, "price", due));
        }
        return rows;
    }

    private static List<Installment> sac(BigDecimal p, BigDecimal i, int n, LocalDate first, String period, int grace) {
        List<Installment> rows = new ArrayList<>();
        BigDecimal amort = p.divide(BigDecimal.valueOf(Math.max(1, n - grace)), 2, MONEY);
        BigDecimal saldo = p;
        for (int k = 1; k <= n; k++) {
            LocalDate due = shift(first, k - 1, period);
            BigDecimal juros = money(saldo.multiply(i));
            if (k <= grace) {
                rows.add(new Installment(k, BigDecimal.ZERO, juros, juros, saldo, "carencia", due));
                continue;
            }
            BigDecimal principal = k == n ? saldo : amort.min(saldo);
            BigDecimal valor = money(principal.add(juros));
            saldo = money(saldo.subtract(principal).max(BigDecimal.ZERO));
            rows.add(new Installment(k, principal, juros, valor, saldo, "sac", due));
        }
        return rows;
    }

    private static List<Installment> simples(BigDecimal p, BigDecimal i, int n, LocalDate first, String period, int grace) {
        List<Installment> rows = new ArrayList<>();
        BigDecimal jurosTotal = money(p.multiply(i).multiply(BigDecimal.valueOf(n)));
        BigDecimal amort = p.divide(BigDecimal.valueOf(Math.max(1, n - grace)), 2, MONEY);
        BigDecimal jurosParcela = jurosTotal.divide(BigDecimal.valueOf(n), 2, MONEY);
        BigDecimal saldo = p;
        for (int k = 1; k <= n; k++) {
            LocalDate due = shift(first, k - 1, period);
            if (k <= grace) {
                rows.add(new Installment(k, BigDecimal.ZERO, jurosParcela, jurosParcela, saldo, "carencia", due));
                continue;
            }
            BigDecimal principal = k == n ? saldo : amort.min(saldo);
            BigDecimal valor = money(principal.add(jurosParcela));
            saldo = money(saldo.subtract(principal).max(BigDecimal.ZERO));
            rows.add(new Installment(k, principal, jurosParcela, valor, saldo, "simples", due));
        }
        return rows;
    }

    private static List<Installment> interestOnly(BigDecimal p, BigDecimal i, int n, LocalDate first, String period, int grace) {
        List<Installment> rows = new ArrayList<>();
        BigDecimal juros = money(p.multiply(i));
        for (int k = 1; k <= n; k++) {
            LocalDate due = shift(first, k - 1, period);
            if (k < n) {
                rows.add(new Installment(k, BigDecimal.ZERO, juros, juros, p, k <= grace ? "carencia" : "so_juros", due));
            } else {
                BigDecimal valor = money(p.add(juros));
                rows.add(new Installment(k, p, juros, valor, BigDecimal.ZERO, "balloon", due));
            }
        }
        return rows;
    }

    private static List<Installment> interestUntilPrincipal(BigDecimal p, BigDecimal jurosValor, int n, LocalDate first, String period) {
        List<Installment> rows = new ArrayList<>();
        BigDecimal juros = money(jurosValor);
        int months = Math.max(1, n);
        for (int k = 1; k <= months; k++) {
            rows.add(new Installment(k, BigDecimal.ZERO, juros, juros, p, "so_juros", shift(first, k - 1, period)));
        }
        rows.add(new Installment(months + 1, p, BigDecimal.ZERO, p, BigDecimal.ZERO, "principal", null));
        return rows;
    }

    private static List<Installment> rent(BigDecimal rent, int n, LocalDate first, String period) {
        List<Installment> rows = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            rows.add(new Installment(k, BigDecimal.ZERO, BigDecimal.ZERO, money(rent), BigDecimal.ZERO, "aluguel", shift(first, k - 1, period)));
        }
        return rows;
    }

    private static Map<String, Object> toMap(Installment row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("numero", row.numero());
        map.put("principal", row.principal());
        map.put("juros", row.juros());
        map.put("parcela", row.parcela());
        map.put("saldo", row.saldo());
        map.put("tipo", row.tipo());
        map.put("vencimento", row.vencimento() == null ? null : row.vencimento().toString());
        return map;
    }

    private static String explain(String sistema, String modo, String tipo, String base) {
        if ("aluguel".equals(sistema) || tipo.contains("aluguel")) {
            return "Aluguel: valor fixo por período, sem amortizar um principal emprestado.";
        }
        if ("so_juros_aberto".equals(sistema) || isOpenInterest(modo, sistema, tipo)) {
            String regra = "saldo".equals(base) ? "juros sobre o saldo em aberto"
                    : ("valor_fixo".equals(base) ? "juro fixo em reais"
                    : "juros sobre o principal original");
            return "Cobrança aberta até quitar (" + regra + "). Não existe '6 parcelas'. O capital fica em aberto até a quitação.";
        }
        if ("so_juros".equals(sistema) || "so_juros".equals(modo) || "americano".equals(sistema)) {
            return "Sistema americano / só juros + balloon: nas parcelas do meio paga apenas o juro. O valor emprestado entra na última parcela.";
        }
        return switch (sistema) {
            case "sac" -> "SAC: amortização constante. As parcelas começam maiores e diminuem.";
            case "simples" -> "Juros simples: o juro incide sobre o valor original e é rateado nas parcelas.";
            default -> "Price (composto): parcela fixa. No início paga mais juro; no fim amortiza mais o principal.";
        };
    }

    private static boolean isOpenInterest(String modo, String sistema, String tipo) {
        String m = modo == null ? "" : modo.toLowerCase();
        String s = sistema == null ? "" : sistema.toLowerCase();
        String t = tipo == null ? "" : tipo.toLowerCase();
        return "juros_rotativo".equals(m) || "so_juros_aberto".equals(m) || "so_juros_ate_quitar".equals(m)
                || "so_juros_aberto".equals(s) || "juros_rotativo".equals(s)
                || t.contains("rotativo");
    }

    private static LocalDate shift(LocalDate first, int index, String period) {
        return switch (period) {
            case "semanal" -> first.plusWeeks(index);
            case "quinzenal" -> first.plusDays(index * 15L);
            case "diario" -> first.plusDays(index);
            case "anual" -> first.plusYears(index);
            default -> first.plusMonths(index);
        };
    }

    private static BigDecimal pow(BigDecimal base, int n) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < n; i++) {
            result = result.multiply(base);
        }
        return result;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, MONEY);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static int integer(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value).split("\\.")[0]);
    }

    private static String str(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }
}
