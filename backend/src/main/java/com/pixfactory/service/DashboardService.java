package com.pixfactory.service;

import com.pixfactory.domain.Charge;
import com.pixfactory.domain.ChargeStatus;
import com.pixfactory.domain.Contract;
import com.pixfactory.domain.ContractStatus;
import com.pixfactory.domain.Launch;
import com.pixfactory.repo.ChargeRepository;
import com.pixfactory.repo.ClientRepository;
import com.pixfactory.repo.ContractRepository;
import com.pixfactory.repo.LaunchRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final LaunchRepository launchRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeService chargeService;

    public DashboardService(
            ClientRepository clientRepository,
            ContractRepository contractRepository,
            LaunchRepository launchRepository,
            ChargeRepository chargeRepository,
            ChargeService chargeService
    ) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.launchRepository = launchRepository;
        this.chargeRepository = chargeRepository;
        this.chargeService = chargeService;
    }

    public Map<String, Object> metrics() {
        chargeService.refreshOverdueAndPromises();
        var clients = clientRepository.findAll();
        var contracts = contractRepository.findAll();
        var charges = chargeRepository.findAll();
        LocalDate today = LocalDate.now();
        String thisMonth = today.toString().substring(0, 7);
        String lastMonth = today.minusMonths(1).toString().substring(0, 7);
        var ativos = contracts.stream().filter(c -> c.getStatus() == ContractStatus.ATIVO
                || c.getStatus() == ContractStatus.ATRASADO
                || c.getStatus() == ContractStatus.PENDENTE).toList();
        var atrasados = contracts.stream().filter(c -> c.getStatus() == ContractStatus.ATRASADO).toList();
        BigDecimal emprestado = sum(contracts, Contract::getValorTotal);
        BigDecimal atraso = charges.stream()
                .filter(c -> c.getStatus() == ChargeStatus.ATRASADO)
                .map(Charge::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucro = sum(contracts, Contract::getValorPago).multiply(new BigDecimal("0.2"));
        long novos = clients.stream().filter(c -> c.getCriadoEm() != null && c.getCriadoEm().toString().startsWith(thisMonth)).count();
        long novosAnterior = clients.stream().filter(c -> c.getCriadoEm() != null && c.getCriadoEm().toString().startsWith(lastMonth)).count();
        long cobrancasPendentes = charges.stream().filter(c -> c.getStatus() == ChargeStatus.PENDENTE || c.getStatus() == ChargeStatus.PARCIAL).count();
        long cobrancasAtrasadas = charges.stream().filter(c -> c.getStatus() == ChargeStatus.ATRASADO).count();
        long vencendoHoje = charges.stream().filter(c -> today.equals(c.getVencimento()) && c.getStatus() != ChargeStatus.PAGO && c.getStatus() != ChargeStatus.CANCELADO).count();
        BigDecimal aReceber = charges.stream()
                .filter(c -> c.getStatus() == ChargeStatus.PENDENTE || c.getStatus() == ChargeStatus.ATRASADO || c.getStatus() == ChargeStatus.PARCIAL)
                .map(Charge::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal capitalCarteira = sum(ativos, Contract::getSaldoDevedor);
        long promisesToday = charges.stream().filter(c -> "pendente".equals(c.getPromiseStatus()) && today.equals(c.getPromiseDate())).count();
        long followUpsHoje = charges.stream().filter(c -> today.equals(c.getNextFollowUp())).count();
        BigDecimal multaMora = charges.stream()
                .map(c -> nz(c.getMultaAplicada()).add(nz(c.getMoraAplicada())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal aVencer = aReceber.subtract(atraso).max(BigDecimal.ZERO);
        BigDecimal jurosRecebidos = charges.stream()
                .filter(c -> c.getStatus() == ChargeStatus.PAGO)
                .map(c -> nz(c.getValorJuros()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pagamentosHoje = charges.stream()
                .filter(c -> c.getLastPaidAt() != null && today.equals(c.getLastPaidAt().atZone(java.time.ZoneOffset.UTC).toLocalDate()))
                .count();
        long emprestimosQuitados = contracts.stream().filter(c -> c.getStatus() == ContractStatus.ENCERRADO).count();
        long clientesInadimplentes = charges.stream()
                .filter(c -> c.getStatus() == ChargeStatus.ATRASADO && c.getClient() != null)
                .map(c -> c.getClient().getId())
                .distinct()
                .count();

        List<String> alertas = new ArrayList<>();
        if (vencendoHoje > 0) {
            alertas.add("Você possui parcelas vencendo hoje.");
        }
        if (cobrancasAtrasadas > 0) {
            alertas.add("Há " + cobrancasAtrasadas + " parcela(s) em atraso.");
        }
        if (promisesToday > 0) {
            alertas.add("Há " + promisesToday + " promessa(s) de pagamento para hoje.");
        }
        if (followUpsHoje > 0) {
            alertas.add("Há " + followUpsHoje + " follow-up(s) de cobrança para hoje.");
        }
        List<Map<String, Object>> jurosDoPeriodo = chargeService.interestPeriodItems();
        if (jurosDoPeriodo.size() == 1) {
            Map<String, Object> item = jurosDoPeriodo.get(0);
            alertas.add("Hoje cabe o juro de " + item.get("clienteNome") + " (R$ " + item.get("valor") + ").");
        } else if (jurosDoPeriodo.size() > 1) {
            alertas.add("Há " + jurosDoPeriodo.size() + " juro(s) do período para cobrar ou gerar.");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clientesAtivos", clients.stream().filter(c -> !"encerrado".equals(c.getStatus())).count());
        map.put("contratosVigentes", ativos.size());
        map.put("novosNoMes", novos);
        map.put("pendencias", cobrancasAtrasadas + vencendoHoje);
        map.put("totalEmprestado", emprestado);
        map.put("totalEmAtraso", atraso);
        map.put("lucroGerado", lucro);
        map.put("emDia", clients.stream().filter(c -> "ativo".equals(c.getStatus())).count());
        map.put("atrasados", clients.stream().filter(c -> "atrasado".equals(c.getStatus())).count());
        map.put("quitados", clients.stream().filter(c -> "encerrado".equals(c.getStatus())).count());
        map.put("receitaRecebida", sum(contracts, Contract::getValorPago));
        map.put("aReceber", aReceber);
        map.put("capitalCarteira", capitalCarteira);
        map.put("cobrancasPendentes", cobrancasPendentes);
        map.put("cobrancasAtrasadas", cobrancasAtrasadas);
        map.put("vencendoHoje", vencendoHoje);
        map.put("promessasHoje", promisesToday);
        map.put("followUpsHoje", followUpsHoje);
        map.put("multaMoraAplicada", multaMora);
        map.put("tendenciaNovos", percentChange(novos, novosAnterior));
        map.put("proximosVencimentos", chargeService.upcoming(8));
        map.put("jurosDoPeriodo", jurosDoPeriodo);
        map.put("carteiraTotal", emprestado);
        map.put("carteiraAberto", capitalCarteira);
        map.put("carteiraVencido", atraso);
        map.put("carteiraAVencer", aVencer);
        map.put("jurosRecebidos", jurosRecebidos);
        map.put("pagamentosHoje", pagamentosHoje);
        map.put("emprestimosQuitados", emprestimosQuitados);
        map.put("clientesInadimplentes", clientesInadimplentes);
        map.put("alertas", alertas);
        return map;
    }

    public Map<String, Object> reports() {
        Map<String, Object> map = new LinkedHashMap<>(metrics());
        List<Launch> launches = launchRepository.findAll();
        BigDecimal despesas = launches.stream()
                .filter(l -> "despesa".equalsIgnoreCase(l.getTipo()))
                .map(Launch::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receitas = launches.stream()
                .filter(l -> "receita".equalsIgnoreCase(l.getTipo()))
                .map(Launch::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("despesas", despesas);
        map.put("receitas", receitas);
        map.put("saldoLancamentos", receitas.subtract(despesas));
        map.put("series", List.of(
                Map.of("label", "Emprestado", "value", map.get("totalEmprestado")),
                Map.of("label", "Em atraso", "value", map.get("totalEmAtraso")),
                Map.of("label", "Recebido", "value", map.get("receitaRecebida")),
                Map.of("label", "Receitas", "value", receitas),
                Map.of("label", "Despesas", "value", despesas)
        ));
        map.put("insight", ((Number) map.get("pendencias")).longValue() > 0
                ? "Há " + map.get("pendencias") + " contrato(s) em atraso. Priorize cobrança via agenda e WhatsApp."
                : "Nenhum contrato em atraso. Boa hora para prospectar novos clientes.");
        return map;
    }

    private BigDecimal sum(List<Contract> contracts, java.util.function.Function<Contract, BigDecimal> getter) {
        return contracts.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int percentChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round(((current - previous) * 100.0) / previous);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
