package com.pixfactory.service;

import com.pixfactory.domain.ActivityLog;
import com.pixfactory.domain.AppNotification;
import com.pixfactory.domain.Client;
import com.pixfactory.exception.ConflictException;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.integration.EmailService;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.ActivityLogRepository;
import com.pixfactory.repo.ChargeRepository;
import com.pixfactory.repo.ClientRepository;
import com.pixfactory.repo.ContractRepository;
import com.pixfactory.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeService chargeService;
    private final DtoMapper mapper;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailService emailService;

    public ClientService(
            ClientRepository clientRepository,
            ContractRepository contractRepository,
            ChargeRepository chargeRepository,
            ChargeService chargeService,
            DtoMapper mapper,
            NotificationRepository notificationRepository,
            ActivityLogRepository activityLogRepository,
            EmailService emailService
    ) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.chargeRepository = chargeRepository;
        this.chargeService = chargeService;
        this.mapper = mapper;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.emailService = emailService;
    }

    public List<Map<String, Object>> findAll() {
        return clientRepository.findAll().stream().map(mapper::client).toList();
    }

    public Map<String, Object> find(Long id) {
        return mapper.client(require(id));
    }

    public Map<String, Object> dossier(Long id) {
        Client client = require(id);
        Map<String, Object> map = new java.util.LinkedHashMap<>(mapper.client(client));
        var contracts = contractRepository.findByClientId(id);
        var charges = chargeRepository.findByClientIdOrderByVencimentoAsc(id);
        java.math.BigDecimal emprestado = contracts.stream()
                .map(c -> c.getValorTotal() == null ? java.math.BigDecimal.ZERO : c.getValorTotal())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal recebido = contracts.stream()
                .map(c -> c.getValorPago() == null ? java.math.BigDecimal.ZERO : c.getValorPago())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal aberto = contracts.stream()
                .map(c -> c.getSaldoDevedor() == null ? java.math.BigDecimal.ZERO : c.getSaldoDevedor())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        map.put("contratos", contracts.stream().map(mapper::contract).toList());
        map.put("cobrancas", charges.stream().map(mapper::charge).toList());
        map.put("totalEmprestado", emprestado);
        map.put("totalRecebido", recebido);
        map.put("totalAberto", aberto);
        map.put("score", chargeService.explainScore(id));
        long pagas = charges.stream().filter(c -> c.getStatus() != null && "pago".equals(c.getStatus().getCode())).count();
        long pendentes = charges.stream().filter(c -> c.getStatus() != null && !"pago".equals(c.getStatus().getCode()) && !"cancelado".equals(c.getStatus().getCode())).count();
        java.math.BigDecimal atrasado = charges.stream()
                .filter(c -> c.getStatus() != null && "atrasado".equals(c.getStatus().getCode()))
                .map(c -> c.remaining())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal multa = charges.stream().map(c -> c.getMultaAplicada() == null ? java.math.BigDecimal.ZERO : c.getMultaAplicada()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal mora = charges.stream().map(c -> c.getMoraAplicada() == null ? java.math.BigDecimal.ZERO : c.getMoraAplicada()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        map.put("operacoes", contracts.size());
        map.put("parcelasPagas", pagas);
        map.put("parcelasPendentes", pendentes);
        map.put("totalAtrasado", atrasado);
        map.put("totalMulta", multa);
        map.put("totalMora", mora);
        map.put("garantias", contracts.stream().map(c -> mapper.readList(c.getGarantiasJson())).flatMap(List::stream).toList());
        return map;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String cpf = String.valueOf(body.get("cpf"));
        if (clientRepository.existsByCpf(cpf)) {
            throw new ConflictException("Já existe um cliente com este CPF.");
        }
        Client client = apply(new Client(), body);
        client.setCriadoEm(LocalDate.now());
        client.setStatus(body.get("status") == null ? "ativo" : String.valueOf(body.get("status")));
        client.setHistoricoJson(mapper.writeList(List.of(
                Map.of("tipo", "cadastro", "data", LocalDate.now().toString(), "info", "Cliente cadastrado")
        )));
        Client saved = clientRepository.save(client);
        notify("Novo cliente", saved.getNome() + " foi cadastrado.", "novo-cliente");
        emailService.send(
                saved.getEmail() == null ? "cliente-demo@example.com" : saved.getEmail(),
                "Cadastro no PixFactory",
                "Olá " + saved.getNome() + ", seu cadastro DEMO foi criado."
        );
        return mapper.client(saved);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Client client = require(id);
        if (body.get("cpf") != null) {
            String cpf = String.valueOf(body.get("cpf"));
            clientRepository.findByCpf(cpf)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new ConflictException("Já existe um cliente com este CPF."); });
        }
        Client saved = clientRepository.save(apply(client, body));
        return mapper.client(saved);
    }

    @Transactional
    public void delete(Long id) {
        Client client = require(id);
        chargeRepository.deleteByClientId(id);
        contractRepository.deleteByClientId(id);
        clientRepository.delete(client);
        notify("Cliente removido", client.getNome() + " foi removido.", "cliente");
    }

    private Client require(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    private Client apply(Client client, Map<String, Object> body) {
        if (body.get("nome") != null) client.setNome(String.valueOf(body.get("nome")));
        if (body.get("cpf") != null) client.setCpf(String.valueOf(body.get("cpf")));
        if (body.get("telefone") != null) client.setTelefone(String.valueOf(body.get("telefone")));
        if (body.get("email") != null) client.setEmail(String.valueOf(body.get("email")));
        if (body.get("endereco") != null) client.setEndereco(String.valueOf(body.get("endereco")));
        if (body.get("dataNascimento") != null && !String.valueOf(body.get("dataNascimento")).isBlank()) {
            client.setDataNascimento(LocalDate.parse(String.valueOf(body.get("dataNascimento"))));
        }
        if (body.get("profissao") != null) client.setProfissao(String.valueOf(body.get("profissao")));
        if (body.get("rendaMensal") != null) client.setRendaMensal(new BigDecimal(String.valueOf(body.get("rendaMensal"))));
        if (body.get("banco") != null) client.setBanco(String.valueOf(body.get("banco")));
        if (body.get("classificacao") != null) client.setClassificacao(String.valueOf(body.get("classificacao")));
        if (body.get("status") != null) client.setStatus(String.valueOf(body.get("status")));
        if (body.get("foto") != null) client.setFotoUrl(String.valueOf(body.get("foto")));
        if (body.get("fotoUrl") != null) client.setFotoUrl(String.valueOf(body.get("fotoUrl")));
        if (body.get("observacoes") != null) client.setObservacoes(String.valueOf(body.get("observacoes")));
        if (body.get("indicador") != null) client.setIndicadorJson(mapper.writeValue(body.get("indicador")));
        if (body.get("referencias") != null) client.setReferenciasJson(mapper.writeValue(body.get("referencias")));
        if (body.get("historico") instanceof List<?> list) {
            List<Map<String, Object>> hist = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = (Map<String, Object>) map;
                    hist.add(cast);
                }
            }
            client.setHistoricoJson(mapper.writeList(hist));
        }
        return client;
    }

    private void notify(String title, String body, String type) {
        AppNotification notification = new AppNotification();
        notification.setTitle(title);
        notification.setBody(body);
        notificationRepository.save(notification);
        ActivityLog log = new ActivityLog();
        log.setText(body);
        log.setType(type);
        activityLogRepository.save(log);
    }
}
