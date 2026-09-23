package com.pixfactory.service;

import com.pixfactory.domain.Charge;
import com.pixfactory.domain.ChargeStatus;
import com.pixfactory.domain.Contract;
import com.pixfactory.domain.ContractStatus;
import com.pixfactory.repo.ChargeRepository;
import com.pixfactory.repo.ContractRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssistantService {
    private final DashboardService dashboardService;
    private final ChargeRepository chargeRepository;
    private final ContractRepository contractRepository;
    private final CashService cashService;

    public AssistantService(
            DashboardService dashboardService,
            ChargeRepository chargeRepository,
            ContractRepository contractRepository,
            CashService cashService
    ) {
        this.dashboardService = dashboardService;
        this.chargeRepository = chargeRepository;
        this.contractRepository = contractRepository;
        this.cashService = cashService;
    }

    public Map<String, Object> ask(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Map<String, Object> metrics = dashboardService.metrics();
        List<String> suggestions = List.of(
                "Quanto vou receber hoje?",
                "Quem está atrasado?",
                "Quanto tenho em caixa?",
                "Qual cliente deve mais?",
                "Como funciona só juros?",
                "Como funciona multa e mora?",
                "Como quitar antecipado?"
        );
        String answer;
        if (contains(q, "hoje", "receber", "venc")) {
            long hoje = ((Number) metrics.getOrDefault("vencendoHoje", 0)).longValue();
            answer = "Hoje vencem " + hoje + " parcela(s). Total a receber em aberto: " + money(metrics.get("aReceber"))
                    + ". Em atraso: " + money(metrics.get("totalEmAtraso")) + ".";
        } else if (contains(q, "atras", "inadimpl")) {
            answer = rankingAtraso();
        } else if (contains(q, "caixa", "dinheiro disponível", "saldo do dia")) {
            Map<String, Object> cash = cashService.today();
            if (Boolean.TRUE.equals(cash.get("aberto"))) {
                answer = "Caixa aberto. Abertura " + money(cash.get("opening")) + ", esperado " + money(cash.get("expected"))
                        + ". Entradas " + money(cash.get("entradas")) + ", saídas " + money(cash.get("saidas")) + ".";
            } else {
                answer = "Nenhum caixa aberto hoje. Abra o caixa do dia antes de registrar dinheiro físico.";
            }
        } else if (contains(q, "deve mais", "maior dívida", "maior saldo")) {
            answer = maiorDivida();
        } else if (contains(q, "só juros", "so juros", "apenas juros", "rotativo", "até quitar", "ate quitar")) {
            answer = "Cobrança aberta: não coloca 6 parcelas. O cliente paga só o juro (1000 a 20% = 200) até devolver o capital, em 2 meses ou em 2 anos. No contrato use Gerar próximo juro. O americano é diferente: capital cai na última parcela.";
        } else if (contains(q, "price", "parcela fixa", "parcela cheia")) {
            answer = "Price: parcela fixa. Parcela cheia (juros simples): o juro do valor original é rateado. SAC: amortização constante, parcela cai com o tempo. Aluguel: valor fixo, sem amortizar empréstimo.";
        } else if (contains(q, "reneg")) {
            answer = "Renegociação não apaga o contrato antigo. As parcelas abertas ficam como renegociadas e nasce um novo contrato ligado ao original, com histórico.";
        } else if (contains(q, "multa", "mora", "atraso diário")) {
            answer = "Multa é percentual único após o vencimento (fora a carência). Mora é percentual ao dia sobre o valor base. Os dois entram no saldo da parcela e aparecem no recibo. Configure em Configurações.";
        } else if (contains(q, "estorno", "estornar")) {
            answer = "Estorno zera o valor pago da parcela, reabre o saldo e lança saída no caixa se ele estiver aberto. Use quando o Pix caiu errado ou o valor foi digitado duas vezes.";
        } else if (contains(q, "recibo")) {
            answer = "Todo pagamento gera extrato. Abra o recibo da parcela para imprimir ou mandar no WhatsApp. É recibo operacional DEMO, não documento fiscal.";
        } else if (contains(q, "quitar", "quitação", "antecipad")) {
            answer = "Quitação antecipada fecha as parcelas em aberto com desconto configurável. O contrato antigo fica encerrado e o desconto entra no extrato.";
        } else if (contains(q, "busca", "encontrar cliente", "ctrl+k")) {
            answer = "Use a busca da sidebar ou Ctrl+K. Procura nome, CPF, telefone e número de contrato.";
        } else if (contains(q, "emprest", "carteira", "capital")) {
            answer = "Capital emprestado: " + money(metrics.get("totalEmprestado"))
                    + ". Carteira em aberto: " + money(metrics.get("capitalCarteira"))
                    + ". Já recebido: " + money(metrics.get("receitaRecebida")) + ".";
        } else if (contains(q, "novo", "mês", "mes")) {
            answer = "Novos clientes neste mês: " + metrics.get("novosNoMes") + ".";
        } else if (q.isBlank()) {
            answer = "Pergunte sobre o dia, atrasos, caixa, dívidas ou como calcular juros. Eu uso os dados do PixFactory — não invento taxa nem recuso cliente.";
        } else {
            answer = "Ainda não entendi. Posso ajudar com: receber hoje, atrasados, caixa, maior dívida, Price, só juros e renegociação. "
                    + "Pendências atuais: " + metrics.get("pendencias") + ".";
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pergunta", question);
        map.put("resposta", answer);
        map.put("sugestoes", suggestions);
        map.put("fonte", "assistente-operacional");
        map.put("aviso", "Indicador operacional. Não é decisão automática de crédito.");
        return map;
    }

    private String rankingAtraso() {
        List<Charge> late = chargeRepository.findAll().stream()
                .filter(c -> c.getStatus() == ChargeStatus.ATRASADO)
                .sorted(Comparator.comparing(Charge::remaining).reversed())
                .limit(5)
                .toList();
        if (late.isEmpty()) {
            return "Nenhuma parcela em atraso no momento.";
        }
        StringBuilder sb = new StringBuilder("Maiores atrasos agora: ");
        for (Charge charge : late) {
            String nome = charge.getClient() == null ? "Cliente" : charge.getClient().getNome();
            sb.append(nome).append(" ").append(money(charge.remaining())).append("; ");
        }
        return sb.toString();
    }

    private String maiorDivida() {
        return contractRepository.findAll().stream()
                .filter(c -> c.getStatus() != ContractStatus.ENCERRADO && c.getStatus() != ContractStatus.RENEGOCIADO && c.getStatus() != ContractStatus.FALECIMENTO)
                .max(Comparator.comparing(c -> nz(c.getSaldoDevedor())))
                .map(c -> {
                    String nome = c.getClient() == null ? "Cliente" : c.getClient().getNome();
                    return nome + " tem o maior saldo em aberto: " + money(c.getSaldoDevedor()) + " no contrato " + c.getId() + ".";
                })
                .orElse("Não há saldo em aberto.");
    }

    private boolean contains(String q, String... words) {
        for (String word : words) {
            if (q.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String money(Object value) {
        BigDecimal n = value instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(value == null ? "0" : value));
        return "R$ " + n.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
