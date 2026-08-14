package com.pixfactory.service;

import com.pixfactory.domain.*;
import com.pixfactory.exception.ApiException;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.integration.EmailService;
import com.pixfactory.integration.PaymentGateway;
import com.pixfactory.integration.PixService;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {
    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final DtoMapper mapper;
    private final PixService pixService;
    private final PaymentGateway paymentGateway;
    private final EmailService emailService;

    public ContractService(
            ContractRepository contractRepository,
            ClientRepository clientRepository,
            PaymentRepository paymentRepository,
            NotificationRepository notificationRepository,
            ActivityLogRepository activityLogRepository,
            DtoMapper mapper,
            PixService pixService,
            PaymentGateway paymentGateway,
            EmailService emailService
    ) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.mapper = mapper;
        this.pixService = pixService;
        this.paymentGateway = paymentGateway;
        this.emailService = emailService;
    }

    public List<Map<String, Object>> findAll() {
        return contractRepository.findAll().stream().map(mapper::contract).toList();
    }

    public Map<String, Object> find(Long id) {
        return mapper.contract(require(id));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Long clientId = toLong(body.get("clienteId"));
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        Contract contract = new Contract();
        contract.setClient(client);
        apply(contract, body);
        if (contract.getStatus() == null) contract.setStatus(ContractStatus.PENDENTE);
        if (contract.getValorPago() == null) contract.setValorPago(BigDecimal.ZERO);
        if (contract.getParcelasPagas() == null) contract.setParcelasPagas(0);
        appendHistory(contract, "Contrato criado");
        Contract saved = contractRepository.save(contract);
        notify("Novo contrato", "Contrato criado para " + client.getNome() + ".", "contrato");
        emailService.send(
                client.getEmail() == null ? "cliente-demo@example.com" : client.getEmail(),
                "Contrato atualizado",
                "Um contrato DEMO foi criado para " + client.getNome() + "."
        );
        return mapper.contract(saved);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Contract contract = require(id);
        apply(contract, body);
        if (body.get("historicoMessage") != null) {
            appendHistory(contract, String.valueOf(body.get("historicoMessage")));
        }
        return mapper.contract(contractRepository.save(contract));
    }

    @Transactional
    public void delete(Long id) {
        contractRepository.delete(require(id));
    }

    @Transactional
    public Map<String, Object> action(Long id, String action, Map<String, Object> body) {
        Contract contract = require(id);
        BigDecimal amount = body.get("valor") == null ? null : new BigDecimal(String.valueOf(body.get("valor")));
        String message = switch (action) {
            case "pagar" -> applyPayment(contract, amount);
            case "alongar" -> {
                contract.setParcelasTotais(Integer.parseInt(String.valueOf(body.get("parcelasTotais"))));
                contract.setProximoPagamento(LocalDate.now().plusDays(30));
                yield "Prazo alongado para " + contract.getParcelasTotais() + " parcelas";
            }
            case "extra" -> {
                contract.setValorTotal(contract.getValorTotal().add(amount));
                contract.setSaldoDevedor(contract.getSaldoDevedor().add(amount));
                contract.setStatus(ContractStatus.ATIVO);
                yield "Empréstimo adicional de " + amount;
            }
            case "bem" -> {
                BigDecimal saldo = contract.getSaldoDevedor().subtract(amount).max(BigDecimal.ZERO);
                contract.setSaldoDevedor(saldo);
                contract.setValorPago(contract.getValorPago().add(amount));
                if (saldo.signum() == 0) contract.setStatus(ContractStatus.ENCERRADO);
                yield "Pagamento com bem: " + body.getOrDefault("descricao", "bem");
            }
            case "reneg" -> {
                contract.setJuros(new BigDecimal(String.valueOf(body.get("juros"))));
                contract.setStatus(ContractStatus.ATIVO);
                yield "Contrato renegociado";
            }
            case "quitar" -> {
                contract.setSaldoDevedor(BigDecimal.ZERO);
                contract.setValorPago(contract.getValorTotal());
                contract.setStatus(ContractStatus.ENCERRADO);
                contract.setParcelasPagas(contract.getParcelasTotais());
                contract.setProximoPagamento(null);
                yield "Contrato quitado";
            }
            case "acordo" -> {
                contract.setSaldoDevedor(amount);
                contract.setStatus(ContractStatus.ACORDO);
                contract.setProximoPagamento(LocalDate.now().plusDays(15));
                yield "Acordo extrajudicial de " + amount;
            }
            case "transferir" -> {
                Long destId = toLong(body.get("clienteId"));
                Client dest = clientRepository.findById(destId)
                        .orElseThrow(() -> new NotFoundException("Cliente inválido."));
                contract.setClient(dest);
                yield "Contrato transferido para " + dest.getNome();
            }
            case "hold" -> {
                contract.setStatus(ContractStatus.HOLD);
                yield "Contrato colocado em espera";
            }
            case "reativar" -> {
                contract.setStatus(ContractStatus.ATIVO);
                yield "Contrato reativado";
            }
            case "falecimento" -> {
                contract.setStatus(ContractStatus.FALECIMENTO);
                yield "Contrato encerrado por falecimento";
            }
            default -> throw new ApiException(400, "Ação inválida.");
        };
        appendHistory(contract, message);
        contractRepository.save(contract);
        notify("Contrato atualizado", message, "pagamento");
        return mapper.contract(contract);
    }

    @Transactional
    public Map<String, Object> createPayment(Long contractId, String method, BigDecimal amount) {
        require(contractId);
        Payment payment = paymentGateway.charge(contractId, method, amount);
        return mapper.payment(paymentRepository.save(payment));
    }

    @Transactional
    public Map<String, Object> confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));
        pixService.confirm(payment);
        paymentRepository.save(payment);
        if (payment.getContractId() != null && payment.getStatus() == PaymentStatus.PAID) {
            Contract contract = require(payment.getContractId());
            applyPayment(contract, payment.getAmount());
            appendHistory(contract, "Pagamento DEMO " + payment.getTxid() + " confirmado");
            contractRepository.save(contract);
            notify("Pagamento recebido", "Pagamento " + payment.getTxid() + " confirmado (DEMO).", "pagamento");
        }
        return mapper.payment(payment);
    }

    @Transactional
    public Map<String, Object> failPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));
        pixService.fail(payment);
        return mapper.payment(paymentRepository.save(payment));
    }

    @Transactional
    public Map<String, Object> cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));
        pixService.cancel(payment);
        return mapper.payment(paymentRepository.save(payment));
    }

    public List<Map<String, Object>> payments() {
        return paymentRepository.findAll().stream().map(mapper::payment).toList();
    }

    private String applyPayment(Contract contract, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(400, "Valor inválido.");
        }
        BigDecimal pago = contract.getValorPago().add(amount);
        BigDecimal saldo = contract.getSaldoDevedor().subtract(amount).max(BigDecimal.ZERO);
        contract.setValorPago(pago);
        contract.setSaldoDevedor(saldo);
        contract.setParcelasPagas(contract.getParcelasPagas() + 1);
        if (saldo.signum() == 0) {
            contract.setStatus(ContractStatus.ENCERRADO);
            contract.setProximoPagamento(null);
        } else {
            contract.setStatus(ContractStatus.ATIVO);
            contract.setProximoPagamento(LocalDate.now().plusDays(30));
        }
        return "Pagamento de " + amount + " registrado";
    }

    private void apply(Contract contract, Map<String, Object> body) {
        if (body.get("tipo") != null) contract.setTipo(String.valueOf(body.get("tipo")));
        if (body.get("valorTotal") != null) contract.setValorTotal(new BigDecimal(String.valueOf(body.get("valorTotal"))));
        if (body.get("valorPago") != null) contract.setValorPago(new BigDecimal(String.valueOf(body.get("valorPago"))));
        if (body.get("parcelasPagas") != null) contract.setParcelasPagas(Integer.parseInt(String.valueOf(body.get("parcelasPagas"))));
        if (body.get("parcelasTotais") != null) contract.setParcelasTotais(Integer.parseInt(String.valueOf(body.get("parcelasTotais"))));
        if (body.get("proximoPagamento") != null) {
            String value = String.valueOf(body.get("proximoPagamento"));
            contract.setProximoPagamento(value.equals("-") || value.isBlank() ? null : LocalDate.parse(value));
        }
        if (body.get("status") != null) contract.setStatus(ContractStatus.from(String.valueOf(body.get("status"))));
        if (body.get("juros") != null) contract.setJuros(new BigDecimal(String.valueOf(body.get("juros"))));
        if (body.get("multa") != null) contract.setMulta(new BigDecimal(String.valueOf(body.get("multa"))));
        if (body.get("saldoDevedor") != null) contract.setSaldoDevedor(new BigDecimal(String.valueOf(body.get("saldoDevedor"))));
        if (body.get("clienteId") != null && contract.getId() != null) {
            Long destId = toLong(body.get("clienteId"));
            Client dest = clientRepository.findById(destId)
                    .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
            contract.setClient(dest);
        }
    }

    private void appendHistory(Contract contract, String message) {
        List<Map<String, Object>> hist = new ArrayList<>(mapper.readList(contract.getHistoricoJson()));
        hist.add(Map.of("data", LocalDate.now().toString(), "descricao", message));
        contract.setHistoricoJson(mapper.writeList(hist));
    }

    private Contract require(Long id) {
        return contractRepository.findById(id).orElseThrow(() -> new NotFoundException("Contrato não encontrado."));
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

    private Long toLong(Object value) {
        if (value == null) throw new ApiException(400, "Cliente é obrigatório.");
        return Long.parseLong(String.valueOf(value));
    }
}
