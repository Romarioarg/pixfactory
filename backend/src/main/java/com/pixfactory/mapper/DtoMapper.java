package com.pixfactory.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixfactory.domain.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        map.put("indicador", readMap(client.getIndicadorJson()));
        map.put("referencias", readList(client.getReferenciasJson()));
        map.put("observacoes", client.getObservacoes());
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
        map.put("tipoOperacao", contract.getTipoOperacao() == null ? "emprestimo" : contract.getTipoOperacao());
        map.put("sistemaAmortizacao", contract.getSistemaAmortizacao() == null ? "price" : contract.getSistemaAmortizacao());
        map.put("modoPagamento", contract.getModoPagamento() == null ? "parcela_cheia" : contract.getModoPagamento());
        map.put("periodicidade", contract.getPeriodicidade() == null ? "mensal" : contract.getPeriodicidade());
        map.put("carenciaMeses", contract.getCarenciaMeses() == null ? 0 : contract.getCarenciaMeses());
        map.put("originalContractId", contract.getOriginalContractId() == null ? null : String.valueOf(contract.getOriginalContractId()));
        map.put("renegociacaoNumero", contract.getRenegociacaoNumero() == null ? 0 : contract.getRenegociacaoNumero());
        map.put("justificativaRenegociacao", contract.getJustificativaRenegociacao());
        map.put("baseCalculo", contract.getBaseCalculo() == null || contract.getBaseCalculo().isBlank() ? "saldo" : contract.getBaseCalculo());
        map.put("jurosFixo", contract.getJurosFixo());
        map.put("ordemPagamento", contract.getOrdemPagamento() == null ? "juros,multa,encargos,principal" : contract.getOrdemPagamento());
        map.put("credito", contract.getCredito());
        map.put("clausulas", contract.getClausulas());
        map.put("regrasSnapshot", readMap(contract.getRegrasSnapshotJson()));
        map.put("garantias", readList(contract.getGarantiasJson()));
        map.put("avalista", readMap(contract.getAvalistaJson()));
        map.put("indicadoPor", readMap(contract.getIndicadoPorJson()));
        map.put("periodicidadeDias", contract.getPeriodicidadeDias() == null ? 0 : contract.getPeriodicidadeDias());
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

    public Map<String, Object> charge(Charge charge) {
        Map<String, Object> map = new LinkedHashMap<>();
        java.math.BigDecimal valor = charge.getValor() == null ? java.math.BigDecimal.ZERO : charge.getValor();
        java.math.BigDecimal pago = charge.getValorPago() == null ? java.math.BigDecimal.ZERO : charge.getValorPago();
        map.put("id", String.valueOf(charge.getId()));
        map.put("clienteId", charge.getClient() == null ? null : String.valueOf(charge.getClient().getId()));
        map.put("clienteNome", charge.getClient() == null ? null : charge.getClient().getNome());
        map.put("telefone", charge.getClient() == null ? null : charge.getClient().getTelefone());
        map.put("contratoId", charge.getContract() == null ? null : String.valueOf(charge.getContract().getId()));
        map.put("numero", charge.getNumero());
        map.put("valor", valor);
        map.put("valorPago", pago);
        map.put("saldo", charge.remaining());
        map.put("vencimento", charge.getVencimento() == null ? null : charge.getVencimento().toString());
        map.put("status", charge.getStatus() == null ? "pendente" : charge.getStatus().getCode());
        map.put("promessaData", charge.getPromiseDate() == null ? null : charge.getPromiseDate().toString());
        map.put("promessaValor", charge.getPromiseAmount());
        map.put("promessaStatus", charge.getPromiseStatus());
        map.put("ultimoCanal", charge.getLastChannel());
        map.put("ultimoResultado", charge.getLastResult());
        map.put("ultimoContato", charge.getLastContactAt());
        map.put("observacao", charge.getNotes());
        map.put("valorPrincipal", charge.getValorPrincipal());
        map.put("valorJuros", charge.getValorJuros());
        map.put("tipoParcela", charge.getTipoParcela());
        map.put("valorBase", charge.getValorBase());
        map.put("multaAplicada", charge.getMultaAplicada());
        map.put("moraAplicada", charge.getMoraAplicada());
        map.put("lastPaidAt", charge.getLastPaidAt());
        map.put("proximoFollowUp", charge.getNextFollowUp() == null ? null : charge.getNextFollowUp().toString());
        map.put("formaPagamento", charge.getFormaPagamento());
        map.put("proximaAcao", charge.getProximaAcao());
        map.put("promessaObs", charge.getPromiseNote());
        map.put("promessaResponsavel", charge.getPromiseOwner());
        map.put("pagoJuros", charge.getPagoJuros());
        map.put("pagoMulta", charge.getPagoMulta());
        map.put("pagoEncargos", charge.getPagoEncargos());
        map.put("pagoPrincipal", charge.getPagoPrincipal());
        int dias = 0;
        LocalDate today = LocalDate.now();
        if (charge.getVencimento() != null
                && charge.getStatus() != ChargeStatus.PAGO
                && charge.getStatus() != ChargeStatus.CANCELADO) {
            dias = (int) Math.max(0, ChronoUnit.DAYS.between(charge.getVencimento(), today));
        }
        map.put("diasAtraso", dias);
        String operacional = charge.getStatus() == null ? "pendente" : charge.getStatus().getCode();
        if (charge.getStatus() == ChargeStatus.PENDENTE && charge.getVencimento() != null) {
            if (today.equals(charge.getVencimento())) operacional = "vencendo_hoje";
            else if (charge.getVencimento().isAfter(today)) operacional = "a_vencer";
        }
        map.put("statusOperacional", operacional);
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

    public String writeMap(Map<String, ?> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Map<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank() || "[]".equals(json.trim())) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return "[]";
        }
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
