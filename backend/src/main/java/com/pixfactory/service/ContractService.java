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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {
    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeService chargeService;
    private final CashService cashService;
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
            ChargeRepository chargeRepository,
            ChargeService chargeService,
            CashService cashService,
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
        this.chargeRepository = chargeRepository;
        this.chargeService = chargeService;
        this.cashService = cashService;
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
        if (contract.getIndicadoPorJson() == null || contract.getIndicadoPorJson().isBlank() || "{}".equals(contract.getIndicadoPorJson())) {
            contract.setIndicadoPorJson(client.getIndicadorJson());
        }
        freezeSnapshot(contract);
        appendHistory(contract, "Contrato criado · liberação");
        Contract saved = contractRepository.save(contract);
        chargeService.ensureSchedule(saved);
        cashService.recordIfOpen("saida", saved.getValorTotal(), "emprestimo", "Liberação do contrato " + saved.getId(), null, saved.getId());
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
        chargeRepository.deleteByContractId(id);
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
                chargeService.addCredit(contract, amount);
                yield "Novo crédito agrupado de " + amount + ". Principal atual " + contract.getValorTotal();
            }
            case "bem" -> {
                chargeService.applyAsset(contract, amount, body.get("descricao") == null ? null : String.valueOf(body.get("descricao")));
                yield "Bem recebido como abatimento: " + body.getOrDefault("descricao", "bem") + " · " + amount;
            }
            case "reneg" -> renegotiate(contract, body);
            case "imprevisto" -> incident(contract, body);
            case "quitar" -> {
                chargeService.settleContract(contract);
                yield "Contrato quitado";
            }
            case "quitar_antecipado" -> {
                var result = chargeService.settleEarly(contract, !"false".equals(String.valueOf(body.getOrDefault("desconto", "true"))));
                yield "Quitação antecipada de " + result.get("valorQuitacao") + " (desconto " + result.get("desconto") + ")";
            }
            case "gerar_juros" -> {
                var next = chargeService.generateNextInterest(contract);
                yield "Próximo juro gerado: " + next.get("valor") + " para " + next.get("vencimento");
            }
            case "amortizar_capital", "pagar_capital" -> {
                var result = chargeService.amortizePrincipal(contract, amount);
                yield "Capital abatido em " + amount + ". Restam " + result.get("capitalRestante") + ". Próximo juro: " + result.get("juroProximo");
            }
            case "nova_operacao", "novo_credito" -> novaOperacao(contract, body, amount);
            case "refinanciar" -> refinanciar(contract, body, amount);
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
                chargeService.cancelOpenCharges(contract);
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
        chargeService.applyContractPayment(contract, amount);
        cashService.recordIfOpen("entrada", amount, "pagamento", "Pagamento contrato " + contract.getId(), null, contract.getId());
        return "Pagamento de " + amount + " registrado";
    }

    private String renegotiate(Contract original, Map<String, Object> body) {
        BigDecimal saldo = original.getSaldoDevedor() == null ? BigDecimal.ZERO : original.getSaldoDevedor();
        if (saldo.signum() <= 0) {
            throw new ApiException(400, "Não há saldo para renegociar.");
        }
        if (original.getStatus() == ContractStatus.RENEGOCIADO || original.getStatus() == ContractStatus.ENCERRADO) {
            throw new ApiException(400, "Este contrato não pode ser renegociado.");
        }
        int round = (original.getRenegociacaoNumero() == null ? 0 : original.getRenegociacaoNumero()) + 1;
        String justificativa = body.get("justificativa") == null ? "Renegociação operacional" : String.valueOf(body.get("justificativa"));
        chargeService.markOpenAsRenegotiated(original);
        original.setJustificativaRenegociacao(justificativa);
        original.setRenegociacaoNumero(round);
        original.setStatus(ContractStatus.RENEGOCIADO);
        contractRepository.save(original);

        Contract neu = new Contract();
        neu.setClient(original.getClient());
        neu.setTipo(original.getTipo() == null ? "Empréstimo" : original.getTipo() + " (R" + round + ")");
        neu.setTipoOperacao(original.getTipoOperacao());
        neu.setValorTotal(body.get("valor") == null ? saldo : new BigDecimal(String.valueOf(body.get("valor"))));
        if (body.get("desconto") != null) {
            BigDecimal desconto = new BigDecimal(String.valueOf(body.get("desconto")));
            neu.setValorTotal(neu.getValorTotal().subtract(desconto).max(BigDecimal.ZERO));
        }
        neu.setValorPago(BigDecimal.ZERO);
        neu.setParcelasPagas(0);
        neu.setParcelasTotais(body.get("parcelasTotais") == null ? Math.max(1, original.getParcelasTotais()) : Integer.parseInt(String.valueOf(body.get("parcelasTotais"))));
        neu.setJuros(body.get("juros") == null ? original.getJuros() : new BigDecimal(String.valueOf(body.get("juros"))));
        neu.setMulta(original.getMulta());
        neu.setSaldoDevedor(neu.getValorTotal());
        neu.setSistemaAmortizacao(String.valueOf(body.getOrDefault("sistema", original.getSistemaAmortizacao() == null ? "price" : original.getSistemaAmortizacao())));
        neu.setModoPagamento(String.valueOf(body.getOrDefault("modo", original.getModoPagamento() == null ? "parcela_cheia" : original.getModoPagamento())));
        neu.setPeriodicidade(original.getPeriodicidade() == null ? "mensal" : original.getPeriodicidade());
        neu.setCarenciaMeses(body.get("carencia") == null ? 0 : Integer.parseInt(String.valueOf(body.get("carencia"))));
        copyFinancialRules(original, neu);
        if (body.get("baseCalculo") != null) neu.setBaseCalculo(String.valueOf(body.get("baseCalculo")));
        if (body.get("periodicidade") != null) neu.setPeriodicidade(String.valueOf(body.get("periodicidade")));
        if (body.get("primeiroVencimento") != null && !String.valueOf(body.get("primeiroVencimento")).isBlank()) {
            neu.setProximoPagamento(LocalDate.parse(String.valueOf(body.get("primeiroVencimento"))));
        } else {
            neu.setProximoPagamento(LocalDate.now().plusDays(30));
        }
        neu.setOriginalContractId(original.getId());
        neu.setRenegociacaoNumero(round);
        neu.setJustificativaRenegociacao(justificativa);
        neu.setStatus(ContractStatus.PENDENTE);
        neu.setHistoricoJson(mapper.writeList(List.of(Map.of(
                "data", LocalDate.now().toString(),
                "descricao", "Origem do contrato " + original.getId() + " · " + justificativa
        ))));
        Contract saved = contractRepository.save(neu);
        freezeSnapshot(saved);
        chargeService.ensureSchedule(saved);
        contractRepository.save(original);
        return "Renegociação criada: contrato " + saved.getId() + " a partir do " + original.getId();
    }

    private String incident(Contract contract, Map<String, Object> body) {
        String tipo = String.valueOf(body.getOrDefault("tipo", "outro")).toLowerCase();
        String obs = body.get("observacao") == null ? "" : String.valueOf(body.get("observacao"));
        return switch (tipo) {
            case "pular_parcela", "feriado" -> {
                int days = body.get("dias") == null ? 7 : Integer.parseInt(String.valueOf(body.get("dias")));
                chargeService.postponeNext(contract, days);
                yield "Parcela adiada em " + days + " dia(s). " + obs;
            }
            case "reajuste_aluguel" -> {
                BigDecimal novo = new BigDecimal(String.valueOf(body.get("valor")));
                contract.setValorTotal(novo);
                yield "Reajuste de aluguel para " + novo + ". " + obs;
            }
            case "perda_renda", "cliente_ausente", "pix_falhou", "pagamento_duplicado", "obito", "outro" ->
                    "Imprevisto registrado: " + tipo + (obs.isBlank() ? "" : " — " + obs);
            default -> "Imprevisto registrado: " + tipo + " — " + obs;
        };
    }

    private String novaOperacao(Contract contract, Map<String, Object> body, BigDecimal amount) {
        String modo = String.valueOf(body.getOrDefault("modoOperacao", body.getOrDefault("modo", "separar"))).toLowerCase();
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(400, "Informe o valor do novo crédito.");
        }
        return switch (modo) {
            case "agrupar", "adicionar", "2" -> {
                chargeService.addCredit(contract, amount);
                yield "Novo crédito agrupado de " + amount + ". Principal " + contract.getValorTotal();
            }
            case "refinanciar", "3" -> refinanciar(contract, body, amount);
            case "quitar_e_novo", "4" -> {
                BigDecimal saldo = contract.getSaldoDevedor() == null ? BigDecimal.ZERO : contract.getSaldoDevedor();
                chargeService.settleContract(contract);
                Contract neu = spawnFrom(contract, amount, body, "Nova operação após quitação do contrato " + contract.getId(), true);
                yield "Operação " + contract.getId() + " quitada (saldo " + saldo + "). Nova operação " + neu.getId() + " de " + amount;
            }
            default -> {
                Contract neu = spawnFrom(contract, amount, body, "Crédito separado a partir do contrato " + contract.getId(), true);
                yield "Nova operação separada " + neu.getId() + " de " + amount + ". Contrato " + contract.getId() + " permanece.";
            }
        };
    }

    private String refinanciar(Contract original, Map<String, Object> body, BigDecimal novoCredito) {
        if (novoCredito == null || novoCredito.signum() <= 0) {
            throw new ApiException(400, "Informe o valor do refinanciamento.");
        }
        BigDecimal saldo = original.getSaldoDevedor() == null ? BigDecimal.ZERO : original.getSaldoDevedor();
        if (novoCredito.compareTo(saldo) < 0) {
            throw new ApiException(400, "O novo crédito precisa cobrir o saldo de " + saldo + ".");
        }
        BigDecimal liquido = novoCredito.subtract(saldo);
        int round = (original.getRenegociacaoNumero() == null ? 0 : original.getRenegociacaoNumero()) + 1;
        chargeService.markOpenAsRenegotiated(original);
        original.setStatus(ContractStatus.RENEGOCIADO);
        original.setRenegociacaoNumero(round);
        original.setJustificativaRenegociacao("Refinanciamento");
        contractRepository.save(original);
        Contract neu = spawnFrom(original, novoCredito, body,
                "Refinanciamento do contrato " + original.getId() + " · quitação " + saldo + " · líquido entregue " + liquido, false);
        neu.setTipo((original.getTipo() == null ? "Empréstimo" : original.getTipo()) + " (RF" + round + ")");
        freezeSnapshot(neu);
        contractRepository.save(neu);
        cashService.recordIfOpen("saida", liquido, "emprestimo", "Líquido do refinanciamento " + neu.getId(), null, neu.getId());
        appendHistory(original, "Refinanciado no contrato " + neu.getId() + ". Saldo " + saldo + " quitado. Líquido " + liquido + ".");
        contractRepository.save(original);
        return "Refinanciamento: novo contrato " + neu.getId() + " de " + novoCredito + ". Quitação anterior " + saldo + ". Líquido " + liquido;
    }

    private Contract spawnFrom(Contract original, BigDecimal valor, Map<String, Object> body, String historico, boolean liberarCaixa) {
        Contract neu = new Contract();
        neu.setClient(original.getClient());
        neu.setTipo(original.getTipo() == null ? "Empréstimo" : original.getTipo());
        neu.setTipoOperacao(original.getTipoOperacao());
        neu.setValorTotal(valor);
        neu.setValorPago(BigDecimal.ZERO);
        neu.setParcelasPagas(0);
        neu.setParcelasTotais(body.get("parcelasTotais") == null ? Math.max(1, original.getParcelasTotais() == null ? 1 : original.getParcelasTotais()) : Integer.parseInt(String.valueOf(body.get("parcelasTotais"))));
        neu.setJuros(body.get("juros") == null ? original.getJuros() : new BigDecimal(String.valueOf(body.get("juros"))));
        neu.setMulta(original.getMulta());
        neu.setSaldoDevedor(valor);
        neu.setSistemaAmortizacao(String.valueOf(body.getOrDefault("sistemaAmortizacao", body.getOrDefault("sistema", original.getSistemaAmortizacao() == null ? "price" : original.getSistemaAmortizacao()))));
        neu.setModoPagamento(String.valueOf(body.getOrDefault("modoPagamento", original.getModoPagamento() == null ? "parcela_cheia" : original.getModoPagamento())));
        copyFinancialRules(original, neu);
        if (body.get("periodicidade") != null) neu.setPeriodicidade(String.valueOf(body.get("periodicidade")));
        neu.setProximoPagamento(LocalDate.now().plusDays(30));
        neu.setStatus(ContractStatus.PENDENTE);
        neu.setOriginalContractId(original.getId());
        neu.setHistoricoJson(mapper.writeList(List.of(Map.of("data", LocalDate.now().toString(), "descricao", historico))));
        freezeSnapshot(neu);
        Contract saved = contractRepository.save(neu);
        chargeService.ensureSchedule(saved);
        if (liberarCaixa) {
            cashService.recordIfOpen("saida", valor, "emprestimo", "Liberação do contrato " + saved.getId(), null, saved.getId());
        }
        return saved;
    }

    private void copyFinancialRules(Contract from, Contract to) {
        if (to.getBaseCalculo() == null || to.getBaseCalculo().isBlank()) {
            to.setBaseCalculo(from.getBaseCalculo() == null ? "saldo" : from.getBaseCalculo());
        }
        if (to.getJurosFixo() == null) to.setJurosFixo(from.getJurosFixo());
        if (to.getOrdemPagamento() == null) to.setOrdemPagamento(from.getOrdemPagamento());
        to.setPeriodicidade(from.getPeriodicidade());
        to.setPeriodicidadeDias(from.getPeriodicidadeDias());
        to.setCarenciaMeses(from.getCarenciaMeses());
        to.setAvalistaJson(from.getAvalistaJson());
        to.setGarantiasJson(from.getGarantiasJson());
        to.setIndicadoPorJson(from.getIndicadoPorJson());
    }

    private void freezeSnapshot(Contract contract) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("taxa", contract.getJuros());
        snap.put("baseCalculo", contract.getBaseCalculo());
        snap.put("jurosFixo", contract.getJurosFixo());
        snap.put("sistema", contract.getSistemaAmortizacao());
        snap.put("modo", contract.getModoPagamento());
        snap.put("periodicidade", contract.getPeriodicidade());
        snap.put("periodicidadeDias", contract.getPeriodicidadeDias());
        snap.put("multa", contract.getMulta());
        snap.put("ordemPagamento", contract.getOrdemPagamento());
        snap.put("carencia", contract.getCarenciaMeses());
        snap.put("parcelas", contract.getParcelasTotais());
        snap.put("congeladoEm", LocalDate.now().toString());
        contract.setRegrasSnapshotJson(mapper.writeMap(snap));
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
        if (body.get("tipoOperacao") != null) contract.setTipoOperacao(String.valueOf(body.get("tipoOperacao")));
        if (body.get("sistemaAmortizacao") != null || body.get("sistema") != null) {
            contract.setSistemaAmortizacao(String.valueOf(body.getOrDefault("sistemaAmortizacao", body.get("sistema"))));
        }
        if (body.get("modoPagamento") != null || body.get("modo") != null) {
            contract.setModoPagamento(String.valueOf(body.getOrDefault("modoPagamento", body.get("modo"))));
        }
        if (body.get("periodicidade") != null) contract.setPeriodicidade(String.valueOf(body.get("periodicidade")));
        if (body.get("carenciaMeses") != null || body.get("carencia") != null) {
            contract.setCarenciaMeses(Integer.parseInt(String.valueOf(body.getOrDefault("carenciaMeses", body.get("carencia")))));
        }
        if (body.get("baseCalculo") != null || body.get("base") != null) {
            contract.setBaseCalculo(String.valueOf(body.getOrDefault("baseCalculo", body.get("base"))));
        }
        if (body.get("jurosFixo") != null) contract.setJurosFixo(new BigDecimal(String.valueOf(body.get("jurosFixo"))));
        if (body.get("ordemPagamento") != null) contract.setOrdemPagamento(String.valueOf(body.get("ordemPagamento")));
        if (body.get("clausulas") != null) contract.setClausulas(String.valueOf(body.get("clausulas")));
        if (body.get("periodicidadeDias") != null) contract.setPeriodicidadeDias(Integer.parseInt(String.valueOf(body.get("periodicidadeDias"))));
        if (body.get("garantias") != null) contract.setGarantiasJson(mapper.writeValue(body.get("garantias")));
        if (body.get("avalista") != null) contract.setAvalistaJson(mapper.writeValue(body.get("avalista")));
        if (body.get("indicadoPor") != null) contract.setIndicadoPorJson(mapper.writeValue(body.get("indicadoPor")));
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
