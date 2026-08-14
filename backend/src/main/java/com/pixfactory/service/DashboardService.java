package com.pixfactory.service;

import com.pixfactory.domain.Contract;
import com.pixfactory.domain.ContractStatus;
import com.pixfactory.domain.Launch;
import com.pixfactory.repo.ClientRepository;
import com.pixfactory.repo.ContractRepository;
import com.pixfactory.repo.LaunchRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final LaunchRepository launchRepository;

    public DashboardService(ClientRepository clientRepository, ContractRepository contractRepository, LaunchRepository launchRepository) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.launchRepository = launchRepository;
    }

    public Map<String, Object> metrics() {
        var clients = clientRepository.findAll();
        var contracts = contractRepository.findAll();
        String thisMonth = LocalDate.now().toString().substring(0, 7);
        var ativos = contracts.stream().filter(c -> c.getStatus() == ContractStatus.ATIVO
                || c.getStatus() == ContractStatus.ATRASADO
                || c.getStatus() == ContractStatus.PENDENTE).toList();
        var atrasados = contracts.stream().filter(c -> c.getStatus() == ContractStatus.ATRASADO).toList();
        BigDecimal emprestado = sum(contracts, Contract::getValorTotal);
        BigDecimal atraso = sum(atrasados, Contract::getSaldoDevedor);
        BigDecimal lucro = sum(contracts, Contract::getValorPago).multiply(new BigDecimal("0.2"));
        long novos = clients.stream().filter(c -> c.getCriadoEm() != null && c.getCriadoEm().toString().startsWith(thisMonth)).count();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clientesAtivos", clients.stream().filter(c -> !"encerrado".equals(c.getStatus())).count());
        map.put("contratosVigentes", ativos.size());
        map.put("novosNoMes", novos);
        map.put("pendencias", atrasados.size());
        map.put("totalEmprestado", emprestado);
        map.put("totalEmAtraso", atraso);
        map.put("lucroGerado", lucro);
        map.put("emDia", clients.stream().filter(c -> "ativo".equals(c.getStatus())).count());
        map.put("atrasados", clients.stream().filter(c -> "atrasado".equals(c.getStatus())).count());
        map.put("quitados", clients.stream().filter(c -> "encerrado".equals(c.getStatus())).count());
        map.put("receitaRecebida", sum(contracts, Contract::getValorPago));
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
}
