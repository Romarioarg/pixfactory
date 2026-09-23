package com.pixfactory.service;

import com.pixfactory.domain.Client;
import com.pixfactory.domain.Contract;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.ChargeRepository;
import com.pixfactory.repo.ClientRepository;
import com.pixfactory.repo.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchService {
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final ChargeRepository chargeRepository;
    private final DtoMapper mapper;

    public SearchService(
            ClientRepository clientRepository,
            ContractRepository contractRepository,
            ChargeRepository chargeRepository,
            DtoMapper mapper
    ) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.chargeRepository = chargeRepository;
        this.mapper = mapper;
    }

    public Map<String, Object> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        if (q.length() < 2) {
            result.put("clientes", List.of());
            result.put("contratos", List.of());
            result.put("cobrancas", List.of());
            result.put("dica", "Digite pelo menos 2 caracteres: nome, CPF, telefone ou número do contrato.");
            return result;
        }
        List<Map<String, Object>> clients = new ArrayList<>();
        for (Client client : clientRepository.findAll()) {
            String nome = client.getNome() == null ? "" : client.getNome();
            String blob = (nome + " " + nz(client.getCpf()) + " " + nz(client.getTelefone()) + " " + nz(client.getEmail())).toLowerCase(Locale.ROOT);
            String d = digits(query);
            boolean byText = blob.contains(q);
            boolean byDigits = d.length() >= 3 && (digits(client.getCpf()).contains(d) || digits(client.getTelefone()).contains(d));
            if (byText || byDigits) {
                Map<String, Object> item = mapper.client(client);
                item.put("href", "cliente.html?id=" + client.getId());
                clients.add(item);
            }
        }
        List<Map<String, Object>> contracts = new ArrayList<>();
        for (Contract contract : contractRepository.findAll()) {
            String id = String.valueOf(contract.getId());
            String nome = contract.getClient() == null || contract.getClient().getNome() == null ? "" : contract.getClient().getNome().toLowerCase(Locale.ROOT);
            String tipo = contract.getTipo() == null ? "" : contract.getTipo().toLowerCase(Locale.ROOT);
            if (id.contains(q) || nome.contains(q) || tipo.contains(q)) {
                Map<String, Object> item = mapper.contract(contract);
                item.put("href", "contratos.html");
                contracts.add(item);
            }
        }
        List<Map<String, Object>> charges = chargeRepository.findAll().stream()
                .filter(c -> String.valueOf(c.getId()).contains(q)
                        || (c.getClient() != null && c.getClient().getNome() != null && c.getClient().getNome().toLowerCase(Locale.ROOT).contains(q)))
                .limit(8)
                .map(c -> {
                    Map<String, Object> item = mapper.charge(c);
                    item.put("href", "agenda.html");
                    return item;
                })
                .toList();
        result.put("clientes", clients.stream().limit(8).toList());
        result.put("contratos", contracts.stream().limit(8).toList());
        result.put("cobrancas", charges);
        return result;
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
