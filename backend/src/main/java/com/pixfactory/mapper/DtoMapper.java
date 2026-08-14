package com.pixfactory.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixfactory.domain.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DtoMapper {
    private final ObjectMapper objectMapper;

    public DtoMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> user(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(user.getId()));
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole().name());
        map.put("plan", user.getPlan());
        map.put("status", user.getStatus().getCode());
        map.put("address", user.getAddress());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    public Map<String, Object> client(Client client) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(client.getId()));
        map.put("nome", client.getNome());
        map.put("foto", client.getFotoUrl());
        map.put("cpf", client.getCpf());
        map.put("telefone", client.getTelefone());
        map.put("email", client.getEmail());
        map.put("endereco", client.getEndereco());
        map.put("dataNascimento", client.getDataNascimento());
        map.put("profissao", client.getProfissao());
        map.put("rendaMensal", client.getRendaMensal());
        map.put("banco", client.getBanco());
        map.put("classificacao", client.getClassificacao());
        map.put("status", client.getStatus());
        map.put("criadoEm", client.getCriadoEm());
        map.put("historico", readList(client.getHistoricoJson()));
        return map;
    }

    public Map<String, Object> contract(Contract contract) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(contract.getId()));
        map.put("clienteId", contract.getClient() == null ? null : String.valueOf(contract.getClient().getId()));
        map.put("tipo", contract.getTipo());
        map.put("valorTotal", contract.getValorTotal());
        map.put("valorPago", contract.getValorPago());
        map.put("parcelasPagas", contract.getParcelasPagas());
        map.put("parcelasTotais", contract.getParcelasTotais());
        map.put("proximoPagamento", contract.getProximoPagamento() == null ? "-" : contract.getProximoPagamento().toString());
        map.put("status", contract.getStatus().getCode());
        map.put("juros", contract.getJuros());
        map.put("multa", contract.getMulta());
        map.put("saldoDevedor", contract.getSaldoDevedor());
        map.put("jurosPendentes", contract.getJurosPendentes());
        map.put("multaPendente", contract.getMultaPendente());
        map.put("historico", readList(contract.getHistoricoJson()));
        return map;
    }

    public Map<String, Object> notification(AppNotification notification) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(notification.getId()));
        map.put("title", notification.getTitle());
        map.put("body", notification.getBody());
        map.put("read", notification.isReadFlag());
        map.put("createdAt", notification.getCreatedAt());
        return map;
    }

    public Map<String, Object> payment(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(payment.getId()));
        map.put("contractId", payment.getContractId() == null ? null : String.valueOf(payment.getContractId()));
        map.put("method", payment.getMethod());
        map.put("txid", payment.getTxid());
        map.put("amount", payment.getAmount());
        map.put("status", payment.getStatus().name());
        map.put("qrPayload", payment.getQrPayload());
        map.put("copyPaste", payment.getCopyPaste());
        map.put("provider", payment.getProvider());
        map.put("demo", true);
        map.put("createdAt", payment.getCreatedAt());
        map.put("updatedAt", payment.getUpdatedAt());
        return map;
    }

    public Map<String, Object> appointment(Appointment appointment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(appointment.getId()));
        map.put("title", appointment.getTitle());
        map.put("notes", appointment.getNotes());
        map.put("date", appointment.getDate());
        map.put("clientId", appointment.getClientId() == null ? null : String.valueOf(appointment.getClientId()));
        map.put("contractId", appointment.getContractId() == null ? null : String.valueOf(appointment.getContractId()));
        map.put("status", appointment.getStatus());
        return map;
    }

    public Map<String, Object> email(EmailMessage email) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", String.valueOf(email.getId()));
        map.put("recipient", email.getRecipient());
        map.put("subject", email.getSubject());
        map.put("body", email.getBody());
        map.put("status", email.getStatus());
        map.put("provider", email.getProvider());
        map.put("demo", true);
        map.put("createdAt", email.getCreatedAt());
        return map;
    }

    public String writeList(List<?> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public List<Map<String, Object>> readList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
