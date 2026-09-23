package com.pixfactory.service;

import com.pixfactory.domain.*;
import com.pixfactory.exception.ApiException;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.ActivityLogRepository;
import com.pixfactory.repo.AppSettingsRepository;
import com.pixfactory.repo.ChargeRepository;
import com.pixfactory.repo.ContractRepository;
import com.pixfactory.repo.LedgerEntryRepository;
import com.pixfactory.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChargeService {
    private static final List<ChargeStatus> OPEN = List.of(
            ChargeStatus.PENDENTE, ChargeStatus.ATRASADO, ChargeStatus.PARCIAL
    );

    private final ChargeRepository chargeRepository;
    private final ContractRepository contractRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final DtoMapper mapper;
    private final CashService cashService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AppSettingsRepository settingsRepository;

    public ChargeService(
            ChargeRepository chargeRepository,
            ContractRepository contractRepository,
            NotificationRepository notificationRepository,
            ActivityLogRepository activityLogRepository,
            DtoMapper mapper,
            CashService cashService,
            LedgerEntryRepository ledgerEntryRepository,
            AppSettingsRepository settingsRepository
    ) {
        this.chargeRepository = chargeRepository;
        this.contractRepository = contractRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.mapper = mapper;
        this.cashService = cashService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.settingsRepository = settingsRepository;
    }

    public List<Map<String, Object>> findAll() {
        refreshOverdueAndPromises();
        return chargeRepository.findAll().stream()
                .sorted(Comparator.comparing(Charge::getVencimento, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Charge::getNumero, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(mapper::charge)
                .toList();
    }

    public Map<String, Object> find(Long id) {
        refreshOverdueAndPromises();
        return mapper.charge(require(id));
    }

    public List<Map<String, Object>> findByDate(LocalDate date) {
        refreshOverdueAndPromises();
        return chargeRepository.findByVencimento(date).stream().map(mapper::charge).toList();
    }

    public List<Map<String, Object>> overdue() {
        refreshOverdueAndPromises();
        return chargeRepository.findByStatusInOrderByVencimentoAsc(List.of(ChargeStatus.ATRASADO, ChargeStatus.PARCIAL))
                .stream()
                .filter(c -> c.getStatus() == ChargeStatus.ATRASADO || c.remaining().signum() > 0)
                .map(mapper::charge)
                .toList();
    }

    public List<Map<String, Object>> followUps(LocalDate date) {
        refreshOverdueAndPromises();
        LocalDate target = date == null ? LocalDate.now() : date;
        return chargeRepository.findAll().stream()
                .filter(c -> target.equals(c.getNextFollowUp()))
                .sorted(Comparator.comparing(Charge::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(mapper::charge)
                .toList();
    }

    @Transactional
    public void ensureSchedulesForAllContracts() {
        for (Contract contract : contractRepository.findAll()) {
            ensureSchedule(contract);
        }
    }

    @Transactional
    public void ensureSchedule(Contract contract) {
        if (contract == null || contract.getId() == null) {
            return;
        }
        if (chargeRepository.countByContractId(contract.getId()) > 0) {
            return;
        }
        generateSchedule(contract);
    }

    @Transactional
    public Map<String, Object> registerPayment(Long chargeId, BigDecimal amount) {
        return registerPayment(chargeId, amount, null, null);
    }

    @Transactional
    public Map<String, Object> registerPayment(Long chargeId, BigDecimal amount, String forma, String excedenteDestino) {
        Charge charge = require(chargeId);
        if (charge.getStatus() == ChargeStatus.PAGO || charge.getStatus() == ChargeStatus.CANCELADO) {
            throw new ApiException(400, "Cobrança já quitada ou cancelada.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(400, "Valor inválido.");
        }
        BigDecimal remaining = charge.remaining();
        BigDecimal applied = amount.min(remaining);
        BigDecimal extra = amount.subtract(remaining);
        if (extra.signum() > 0 && (excedenteDestino == null || excedenteDestino.isBlank())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("codigo", "excedente");
            details.put("saldoParcela", remaining);
            details.put("excedente", extra);
            details.put("opcoes", List.of("amortizar_principal", "antecipar", "quitar_proxima", "credito"));
            throw new ApiException(409, "Pagamento maior que a parcela. Informe o destino do excedente.", details);
        }
        if (ledgerEntryRepository.existsByChargeIdAndTipoAndValorAndCreatedAtAfter(
                chargeId, "pagamento", applied, Instant.now().minusSeconds(8))) {
            throw new ApiException(409, "Pagamento duplicado ignorado. Aguarde e confira o extrato.");
        }
        applyToCharge(charge, applied);
        charge.setLastPaidAt(Instant.now());
        if (forma != null && !forma.isBlank()) {
            charge.setFormaPagamento(forma);
        }
        chargeRepository.save(charge);
        ledger("pagamento", applied, charge, "Recebimento da parcela " + charge.getNumero()
                + (forma == null || forma.isBlank() ? "" : " · " + forma));
        syncContractFromCharges(charge.getContract());
        if (extra.signum() > 0) {
            applyExcess(charge.getContract(), extra, excedenteDestino);
        }
        if ("principal".equals(charge.getTipoParcela()) && isOpenInterestContract(charge.getContract())) {
            recalculateFutureInterest(charge.getContract());
        }
        if ("pendente".equals(charge.getPromiseStatus()) && charge.getStatus() == ChargeStatus.PAGO) {
            charge.setPromiseStatus("cumprida");
            chargeRepository.save(charge);
        }
        notify("Pagamento recebido", "Parcela " + charge.getNumero() + " — " + charge.getClient().getNome() + ".", "pagamento");
        cashService.recordIfOpen("entrada", amount, "pagamento", "Parcela " + charge.getNumero() + " · " + charge.getClient().getNome(), charge.getId(), charge.getContract().getId());
        return mapper.charge(charge);
    }

    @Transactional
    public void addCredit(Contract contract, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(400, "Valor do novo crédito inválido.");
        }
        ensureSchedule(contract);
        Charge capital = principalCharge(contract);
        if (capital == null) {
            capital = new Charge();
            capital.setClient(contract.getClient());
            capital.setContract(contract);
            capital.setNumero(chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId()).size() + 1);
            capital.setValor(amount);
            capital.setValorBase(amount);
            capital.setValorPrincipal(amount);
            capital.setValorJuros(BigDecimal.ZERO);
            capital.setTipoParcela("principal");
            capital.setVencimento(null);
            capital.setValorPago(BigDecimal.ZERO);
            capital.setStatus(ChargeStatus.PENDENTE);
        } else {
            capital.setValor(nz(capital.getValor()).add(amount));
            capital.setValorBase(capital.getValor());
            capital.setValorPrincipal(nz(capital.getValorPrincipal()).add(amount));
            if (capital.getStatus() == ChargeStatus.PAGO || capital.getStatus() == ChargeStatus.CANCELADO) {
                capital.setStatus(ChargeStatus.PENDENTE);
            }
        }
        chargeRepository.save(capital);
        contract.setValorTotal(nz(contract.getValorTotal()).add(amount));
        contract.setStatus(ContractStatus.ATIVO);
        if (isOpenInterestContract(contract)) {
            recalculateFutureInterest(contract);
        } else {
            syncContractFromCharges(contract);
        }
    }

    @Transactional
    public void applyAsset(Contract contract, BigDecimal amount, String descricao) {
        applyContractPayment(contract, amount);
        ledger("bem", amount, contract, descricao == null || descricao.isBlank() ? "Bem recebido como abatimento" : descricao);
    }

    @Transactional
    public void applyContractPayment(Contract contract, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(400, "Valor inválido.");
        }
        ensureSchedule(contract);
        BigDecimal saldo = nz(contract.getSaldoDevedor());
        if (amount.compareTo(saldo) > 0) {
            throw new ApiException(400, "Não é possível registrar pagamento maior que o saldo do contrato.");
        }
        BigDecimal left = amount;
        List<Charge> open = openCharges(contract.getId());
        for (Charge charge : open) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal slice = left.min(charge.remaining());
            applyToCharge(charge, slice);
            chargeRepository.save(charge);
            left = left.subtract(slice);
        }
        syncContractFromCharges(contract);
        if (isOpenInterestContract(contract)) {
            recalculateFutureInterest(contract);
        }
    }

    @Transactional
    public void settleContract(Contract contract) {
        ensureSchedule(contract);
        for (Charge charge : chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId())) {
            if (charge.getStatus() == ChargeStatus.CANCELADO) {
                continue;
            }
            charge.setValorPago(nz(charge.getValor()));
            charge.setStatus(ChargeStatus.PAGO);
            if ("pendente".equals(charge.getPromiseStatus())) {
                charge.setPromiseStatus("cumprida");
            }
            chargeRepository.save(charge);
        }
        syncContractFromCharges(contract);
        contract.setSaldoDevedor(BigDecimal.ZERO);
        contract.setValorPago(nz(contract.getValorTotal()));
        contract.setStatus(ContractStatus.ENCERRADO);
        contract.setParcelasPagas(contract.getParcelasTotais());
        contract.setProximoPagamento(null);
        contractRepository.save(contract);
    }

    @Transactional
    public void markOpenAsRenegotiated(Contract contract) {
        for (Charge charge : openCharges(contract.getId())) {
            charge.setStatus(ChargeStatus.RENEGOCIADA);
            charge.setNotes((charge.getNotes() == null ? "" : charge.getNotes() + " | ") + "Parcela preservada após renegociação.");
            chargeRepository.save(charge);
        }
        syncContractFromCharges(contract);
        contract.setStatus(ContractStatus.RENEGOCIADO);
        contractRepository.save(contract);
    }

    @Transactional
    public void postponeNext(Contract contract, int days) {
        Charge next = openCharges(contract.getId()).stream()
                .min(Comparator.comparing(Charge::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (next == null) {
            throw new ApiException(400, "Não há parcela em aberto para adiar.");
        }
        next.setVencimento(next.getVencimento().plusDays(days));
        chargeRepository.save(next);
        syncContractFromCharges(contract);
    }

    @Transactional
    public void cancelOpenCharges(Contract contract) {
        for (Charge charge : openCharges(contract.getId())) {
            charge.setStatus(ChargeStatus.CANCELADO);
            chargeRepository.save(charge);
        }
    }

    @Transactional
    public Map<String, Object> registerPromise(Long chargeId, LocalDate date, BigDecimal amount) {
        return registerPromise(chargeId, date, amount, null, null);
    }

    @Transactional
    public Map<String, Object> registerPromise(Long chargeId, LocalDate date, BigDecimal amount, String notes, String owner) {
        Charge charge = require(chargeId);
        if (date == null) {
            throw new ApiException(400, "Data da promessa é obrigatória.");
        }
        BigDecimal promised = amount == null || amount.signum() <= 0 ? charge.remaining() : amount;
        charge.setPromiseDate(date);
        charge.setPromiseAmount(promised);
        charge.setPromiseStatus("pendente");
        if (notes != null && !notes.isBlank()) {
            charge.setPromiseNote(notes);
        }
        if (owner != null && !owner.isBlank()) {
            charge.setPromiseOwner(owner);
        }
        chargeRepository.save(charge);
        notify("Promessa de pagamento", charge.getClient().getNome() + " prometeu pagar " + promised + " em " + date + ".", "cobranca");
        return mapper.charge(charge);
    }

    @Transactional
    public Map<String, Object> reversePayment(Long chargeId, String reason) {
        Charge charge = require(chargeId);
        BigDecimal paid = nz(charge.getValorPago());
        if (paid.signum() <= 0) {
            throw new ApiException(400, "Não há pagamento para estornar nesta parcela.");
        }
        String motivo = reason == null || reason.isBlank() ? "Estorno operacional" : reason;
        charge.setValorPago(BigDecimal.ZERO);
        charge.setPagoJuros(BigDecimal.ZERO);
        charge.setPagoMulta(BigDecimal.ZERO);
        charge.setPagoEncargos(BigDecimal.ZERO);
        charge.setPagoPrincipal(BigDecimal.ZERO);
        if (charge.getVencimento() != null && charge.getVencimento().isBefore(LocalDate.now())) {
            charge.setStatus(ChargeStatus.ATRASADO);
        } else {
            charge.setStatus(ChargeStatus.PENDENTE);
        }
        charge.setNotes((charge.getNotes() == null ? "" : charge.getNotes() + " | ") + "Estorno: " + motivo);
        chargeRepository.save(charge);
        syncContractFromCharges(charge.getContract());
        ledger("estorno", paid, charge, motivo);
        cashService.recordIfOpen("saida", paid, "estorno", "Estorno parcela " + charge.getNumero(), charge.getId(), charge.getContract().getId());
        notify("Estorno", "Parcela " + charge.getNumero() + " estornada. " + motivo, "pagamento");
        return mapper.charge(charge);
    }

    public Map<String, Object> receipt(Long chargeId) {
        Charge charge = require(chargeId);
        AppSettings settings = settings();
        Map<String, Object> map = new java.util.LinkedHashMap<>(mapper.charge(charge));
        map.put("empresa", settings.getCompanyName());
        map.put("reciboId", "PF-" + charge.getId());
        map.put("clienteNome", charge.getClient() == null ? "" : charge.getClient().getNome());
        map.put("clienteCpf", charge.getClient() == null ? "" : charge.getClient().getCpf());
        map.put("contratoId", charge.getContract() == null ? null : String.valueOf(charge.getContract().getId()));
        map.put("emitidoEm", LocalDate.now().toString());
        map.put("historico", ledgerEntryRepository.findByChargeIdOrderByCreatedAtDesc(chargeId).stream().map(this::ledgerMap).toList());
        map.put("aviso", "Recibo operacional DEMO. Não substitui documento fiscal.");
        return map;
    }

    public List<Map<String, Object>> ledgerForCharge(Long chargeId) {
        require(chargeId);
        return ledgerEntryRepository.findByChargeIdOrderByCreatedAtDesc(chargeId).stream().map(this::ledgerMap).toList();
    }

    @Transactional
    public Map<String, Object> settleEarly(Contract contract, boolean applyDiscount) {
        ensureSchedule(contract);
        refreshOverdueAndPromises();
        BigDecimal remaining = openCharges(contract.getId()).stream().map(Charge::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (remaining.signum() <= 0) {
            throw new ApiException(400, "Não há saldo em aberto para quitar antecipadamente.");
        }
        AppSettings settings = settings();
        BigDecimal discountPct = applyDiscount ? nz(settings.getEarlyPayoffDiscountPercent()) : BigDecimal.ZERO;
        BigDecimal discount = remaining.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal due = remaining.subtract(discount).max(BigDecimal.ZERO);
        settleContract(contract);
        ledger("quitacao", due, contract, "Quitação antecipada. Desconto " + discount);
        cashService.recordIfOpen("entrada", due, "quitacao", "Quitação antecipada contrato " + contract.getId(), null, contract.getId());
        return Map.of(
                "contrato", mapper.contract(contract),
                "saldoOriginal", remaining,
                "desconto", discount,
                "valorQuitacao", due
        );
    }

    @Transactional
    public Map<String, Object> registerContact(Long chargeId, String channel, String result, String notes, LocalDate nextFollowUp) {
        return registerContact(chargeId, channel, result, notes, nextFollowUp, null);
    }

    @Transactional
    public Map<String, Object> registerContact(Long chargeId, String channel, String result, String notes, LocalDate nextFollowUp, String proximaAcao) {
        Charge charge = require(chargeId);
        charge.setLastChannel(channel == null || channel.isBlank() ? "whatsapp" : channel);
        charge.setLastResult(result == null ? "" : result);
        charge.setLastContactAt(Instant.now());
        if (notes != null && !notes.isBlank()) {
            charge.setNotes(notes);
        }
        if (nextFollowUp != null) {
            charge.setNextFollowUp(nextFollowUp);
        } else if (charge.getNextFollowUp() == null && charge.getStatus() != ChargeStatus.PAGO) {
            charge.setNextFollowUp(LocalDate.now().plusDays(3));
        }
        if (proximaAcao != null && !proximaAcao.isBlank()) {
            charge.setProximaAcao(proximaAcao);
        } else if (result != null && !result.isBlank() && List.of("cobrar hoje", "cobrar amanha", "ligar", "aguardar promessa", "renegociar", "revisar garantia").contains(result.toLowerCase())) {
            charge.setProximaAcao(result);
        }
        chargeRepository.save(charge);
        notify("Cobrança registrada", "Contato com " + charge.getClient().getNome() + " via " + charge.getLastChannel() + ".", "cobranca");
        return mapper.charge(charge);
    }

    @Transactional
    public void refreshOverdueAndPromises() {
        rollForwardOpenInterest();
        LocalDate today = LocalDate.now();
        for (Charge charge : chargeRepository.findAll()) {
            boolean dirty = false;
            if (charge.getStatus() == ChargeStatus.PENDENTE
                    && charge.getVencimento() != null
                    && charge.getVencimento().isBefore(today)) {
                charge.setStatus(ChargeStatus.ATRASADO);
                dirty = true;
            }
            if (charge.getStatus() == ChargeStatus.PARCIAL
                    && charge.getVencimento() != null
                    && charge.getVencimento().isBefore(today)
                    && charge.remaining().signum() > 0) {
                charge.setStatus(ChargeStatus.ATRASADO);
                dirty = true;
            }
            if ("pendente".equals(charge.getPromiseStatus())
                    && charge.getPromiseDate() != null
                    && charge.getPromiseDate().isBefore(today)
                    && charge.getStatus() != ChargeStatus.PAGO) {
                charge.setPromiseStatus("quebrada");
                dirty = true;
            }
            if (dirty || applyLateCharges(charge, today)) {
                chargeRepository.save(charge);
            }
        }
    }

    public List<Map<String, Object>> upcoming(int limit) {
        refreshOverdueAndPromises();
        return chargeRepository.findByStatusInOrderByVencimentoAsc(OPEN).stream()
                .limit(limit)
                .map(mapper::charge)
                .toList();
    }

    public String explainScore(Long clientId) {
        List<Charge> charges = chargeRepository.findByClientIdOrderByVencimentoAsc(clientId);
        long late = charges.stream().filter(c -> c.getStatus() == ChargeStatus.ATRASADO).count();
        long paid = charges.stream().filter(c -> c.getStatus() == ChargeStatus.PAGO).count();
        long broken = charges.stream().filter(c -> "quebrada".equals(c.getPromiseStatus())).count();
        if (charges.isEmpty()) {
            return "Novo — ainda sem histórico de parcelas.";
        }
        if (late >= 3) {
            return "Em observação — " + late + " parcelas em atraso.";
        }
        if (late >= 1) {
            return "Atenção — há parcela em atraso. Acompanhar cobrança.";
        }
        if (broken > 0) {
            return "Em observação — promessa de pagamento não cumprida.";
        }
        if (paid >= 3) {
            return "Bom histórico de pagamento.";
        }
        return "Cliente em dia, histórico ainda curto.";
    }

    @Transactional
    public Map<String, Object> generateNextInterest(Contract contract) {
        if (!isOpenInterestContract(contract)) {
            throw new ApiException(400, "Só contratos de juros até quitar geram o próximo juro.");
        }
        ensureSchedule(contract);
        if (remainingCapital(contract).signum() <= 0) {
            throw new ApiException(400, "O capital já foi quitado. Não há próximo juro.");
        }
        List<Charge> charges = chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId());
        LocalDate nextDue = nextInterestDueDate(charges, contract);
        boolean exists = charges.stream().anyMatch(c -> "so_juros".equals(c.getTipoParcela()) && nextDue.equals(c.getVencimento()));
        if (exists) {
            throw new ApiException(400, "Já existe cobrança de juros para " + nextDue + ".");
        }
        BigDecimal juros = periodInterest(contract);
        int numero = charges.stream().map(c -> c.getNumero() == null ? 0 : c.getNumero()).max(Integer::compareTo).orElse(0) + 1;
        Charge charge = new Charge();
        charge.setClient(contract.getClient());
        charge.setContract(contract);
        charge.setNumero(numero);
        charge.setValor(juros);
        charge.setValorBase(juros);
        charge.setValorPrincipal(BigDecimal.ZERO);
        charge.setValorJuros(juros);
        charge.setTipoParcela("so_juros");
        charge.setVencimento(nextDue);
        charge.setValorPago(BigDecimal.ZERO);
        charge.setStatus(ChargeStatus.PENDENTE);
        chargeRepository.save(charge);
        contract.setParcelasTotais((contract.getParcelasTotais() == null ? 0 : contract.getParcelasTotais()) + 1);
        syncContractFromCharges(contract);
        return mapper.charge(charge);
    }

    @Transactional
    public Map<String, Object> amortizePrincipal(Contract contract, BigDecimal amount) {
        if (!isOpenInterestContract(contract)) {
            throw new ApiException(400, "Abater capital vale para contratos de juros até quitar.");
        }
        Charge capital = principalCharge(contract);
        if (capital == null) {
            throw new ApiException(400, "Este contrato não tem capital em aberto.");
        }
        Map<String, Object> paid = registerPayment(capital.getId(), amount);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("capitalRestante", remainingCapital(contract));
        result.put("juroProximo", periodInterest(contract));
        result.put("cobranca", paid);
        return result;
    }

    public List<Map<String, Object>> interestPeriodItems() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Charge charge : chargeRepository.findAll()) {
            if (!"so_juros".equals(charge.getTipoParcela())) {
                continue;
            }
            if (charge.getStatus() == ChargeStatus.PAGO
                    || charge.getStatus() == ChargeStatus.CANCELADO
                    || charge.getStatus() == ChargeStatus.RENEGOCIADA) {
                continue;
            }
            if (charge.getVencimento() == null || charge.getVencimento().isAfter(today)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>(mapper.charge(charge));
            row.put("acao", "cobrar");
            row.put("rotulo", today.equals(charge.getVencimento()) ? "vence hoje" : "em atraso");
            items.add(row);
        }
        for (Contract contract : contractRepository.findAll()) {
            if (!shouldGenerateNextInterest(contract, today)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("contratoId", String.valueOf(contract.getId()));
            row.put("clienteNome", contract.getClient() == null ? "Cliente" : contract.getClient().getNome());
            row.put("telefone", contract.getClient() == null ? null : contract.getClient().getTelefone());
            row.put("valor", periodInterest(contract));
            row.put("saldo", periodInterest(contract));
            row.put("vencimento", nextInterestDueDate(
                    chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId()), contract).toString());
            row.put("status", "pendente");
            row.put("acao", "gerar");
            row.put("rotulo", "gerar juro do período");
            items.add(row);
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.getOrDefault("vencimento", ""))));
        return items;
    }

    private boolean isOpenInterestContract(Contract contract) {
        String modo = contract.getModoPagamento() == null ? "" : contract.getModoPagamento().toLowerCase();
        String sistema = contract.getSistemaAmortizacao() == null ? "" : contract.getSistemaAmortizacao().toLowerCase();
        String tipo = contract.getTipoOperacao() == null ? "" : contract.getTipoOperacao().toLowerCase();
        return modo.contains("rotativo") || modo.contains("aberto") || sistema.contains("aberto") || tipo.contains("rotativo");
    }

    private boolean isClosedForInterest(Contract contract) {
        ContractStatus status = contract.getStatus();
        return status == ContractStatus.ENCERRADO
                || status == ContractStatus.FALECIMENTO
                || status == ContractStatus.RENEGOCIADO;
    }

    private void rollForwardOpenInterest() {
        LocalDate today = LocalDate.now();
        for (Contract contract : contractRepository.findAll()) {
            if (!isOpenInterestContract(contract) || isClosedForInterest(contract)) {
                continue;
            }
            ensureSchedule(contract);
            int guard = 0;
            while (guard++ < 36 && shouldGenerateNextInterest(contract, today)) {
                try {
                    generateNextInterest(contract);
                } catch (ApiException ignored) {
                    break;
                }
            }
        }
    }

    private boolean shouldGenerateNextInterest(Contract contract, LocalDate today) {
        if (!isOpenInterestContract(contract) || isClosedForInterest(contract)) {
            return false;
        }
        if (remainingCapital(contract).signum() <= 0) {
            return false;
        }
        List<Charge> charges = chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId());
        if (charges.isEmpty()) {
            return false;
        }
        LocalDate nextDue = nextInterestDueDate(charges, contract);
        if (nextDue.isAfter(today)) {
            return false;
        }
        return charges.stream().noneMatch(c -> "so_juros".equals(c.getTipoParcela()) && nextDue.equals(c.getVencimento()));
    }

    private LocalDate nextInterestDueDate(List<Charge> charges, Contract contract) {
        Charge lastInterest = charges.stream()
                .filter(c -> "so_juros".equals(c.getTipoParcela()) && c.getVencimento() != null)
                .filter(c -> c.getStatus() != ChargeStatus.CANCELADO)
                .max(Comparator.comparing(Charge::getVencimento))
                .orElse(null);
        if (lastInterest == null || lastInterest.getVencimento() == null) {
            return LocalDate.now();
        }
        return shiftPeriod(contract, lastInterest.getVencimento());
    }

    private Charge principalCharge(Contract contract) {
        return chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId()).stream()
                .filter(c -> "principal".equals(c.getTipoParcela()))
                .filter(c -> c.getStatus() != ChargeStatus.CANCELADO && c.getStatus() != ChargeStatus.RENEGOCIADA)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal remainingCapital(Contract contract) {
        Charge capital = principalCharge(contract);
        if (capital == null || capital.getStatus() == ChargeStatus.PAGO) {
            return BigDecimal.ZERO;
        }
        return capital.remaining();
    }

    private BigDecimal periodInterest(Contract contract) {
        BigDecimal original = nz(contract.getValorTotal());
        Charge capital = principalCharge(contract);
        if (capital != null && nz(capital.getValor()).signum() > 0) {
            original = nz(capital.getValor());
        }
        return LoanEngine.interestAmount(
                original,
                remainingCapital(contract),
                nz(contract.getJuros()),
                nz(contract.getJurosFixo()),
                contract.getBaseCalculo()
        );
    }

    private void applyExcess(Contract contract, BigDecimal extra, String dest) {
        String d = dest == null ? "" : dest.toLowerCase().trim();
        switch (d) {
            case "amortizar_principal", "principal" -> {
                Charge capital = principalCharge(contract);
                if (capital != null && capital.remaining().signum() > 0) {
                    BigDecimal slice = extra.min(capital.remaining());
                    applyToCharge(capital, slice);
                    capital.setLastPaidAt(Instant.now());
                    chargeRepository.save(capital);
                    extra = extra.subtract(slice);
                    if (isOpenInterestContract(contract)) {
                        recalculateFutureInterest(contract);
                    }
                }
                if (extra.signum() > 0) {
                    contract.setCredito(nz(contract.getCredito()).add(extra));
                }
            }
            case "antecipar", "quitar_proxima", "proxima" -> applyContractPayment(contract, extra);
            case "credito" -> contract.setCredito(nz(contract.getCredito()).add(extra));
            default -> throw new ApiException(400, "Destino do excedente inválido.");
        }
        syncContractFromCharges(contract);
        contractRepository.save(contract);
    }

    private void recalculateFutureInterest(Contract contract) {
        if (!isOpenInterestContract(contract)) {
            return;
        }
        LocalDate today = LocalDate.now();
        BigDecimal capitalLeft = remainingCapital(contract);
        BigDecimal newJuro = periodInterest(contract);
        for (Charge charge : chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId())) {
            if (!"so_juros".equals(charge.getTipoParcela())) {
                continue;
            }
            if (charge.getStatus() == ChargeStatus.PAGO
                    || charge.getStatus() == ChargeStatus.CANCELADO
                    || charge.getStatus() == ChargeStatus.RENEGOCIADA) {
                continue;
            }
            if (nz(charge.getValorPago()).signum() > 0) {
                continue;
            }
            if (charge.getVencimento() != null && charge.getVencimento().isBefore(today)) {
                continue;
            }
            if (capitalLeft.signum() <= 0) {
                charge.setStatus(ChargeStatus.CANCELADO);
            } else {
                charge.setValor(newJuro);
                charge.setValorBase(newJuro);
                charge.setValorJuros(newJuro);
            }
            chargeRepository.save(charge);
        }
        syncContractFromCharges(contract);
    }

    private LocalDate shiftPeriod(Contract contract, LocalDate from) {
        String p = contract.getPeriodicidade() == null ? "mensal" : contract.getPeriodicidade();
        if ("personalizada".equalsIgnoreCase(p) || "outra".equalsIgnoreCase(p)) {
            int days = contract.getPeriodicidadeDias() == null || contract.getPeriodicidadeDias() < 1 ? 30 : contract.getPeriodicidadeDias();
            return from.plusDays(days);
        }
        return shiftPeriod(from, p);
    }

    private LocalDate shiftPeriod(LocalDate from, String period) {
        String p = period == null ? "mensal" : period;
        return switch (p) {
            case "semanal" -> from.plusWeeks(1);
            case "quinzenal" -> from.plusDays(15);
            case "diario" -> from.plusDays(1);
            case "anual" -> from.plusYears(1);
            default -> from.plusMonths(1);
        };
    }

    private void generateSchedule(Contract contract) {
        int total = Math.max(1, contract.getParcelasTotais() == null ? 1 : contract.getParcelasTotais());
        int paid = Math.min(contract.getParcelasPagas() == null ? 0 : contract.getParcelasPagas(), total);
        LocalDate firstOpen = contract.getProximoPagamento() != null ? contract.getProximoPagamento() : LocalDate.now();
        String period = contract.getPeriodicidade() == null || contract.getPeriodicidade().isBlank() ? "mensal" : contract.getPeriodicidade();
        LocalDate firstDue = switch (period) {
            case "semanal" -> firstOpen.minusWeeks(Math.max(paid, 0));
            case "quinzenal" -> firstOpen.minusDays(Math.max(paid, 0) * 15L);
            case "diario" -> firstOpen.minusDays(Math.max(paid, 0));
            case "anual" -> firstOpen.minusYears(Math.max(paid, 0));
            default -> firstOpen.minusMonths(Math.max(paid, 0));
        };
        LocalDate today = LocalDate.now();
        String sistema = contract.getSistemaAmortizacao();
        List<LoanEngine.Installment> rows;
        if (sistema == null || sistema.isBlank()) {
            rows = equalSplit(nz(contract.getValorTotal()), total, firstDue, period);
        } else {
            rows = LoanEngine.scheduleFromContract(
                    nz(contract.getValorTotal()),
                    nz(contract.getJuros()),
                    total,
                    sistema,
                    contract.getModoPagamento(),
                    contract.getTipoOperacao(),
                    period,
                    contract.getCarenciaMeses() == null ? 0 : contract.getCarenciaMeses(),
                    firstDue
            );
        }
        for (LoanEngine.Installment row : rows) {
            Charge charge = new Charge();
            charge.setClient(contract.getClient());
            charge.setContract(contract);
            charge.setNumero(row.numero());
            charge.setValor(row.parcela());
            charge.setValorBase(row.parcela());
            charge.setValorPrincipal(row.principal());
            charge.setValorJuros(row.juros());
            charge.setTipoParcela(row.tipo());
            charge.setVencimento(row.vencimento());
            if (row.numero() <= paid || contract.getStatus() == ContractStatus.ENCERRADO) {
                charge.setValorPago(charge.getValor());
                charge.setStatus(ChargeStatus.PAGO);
            } else if (charge.getVencimento() != null && charge.getVencimento().isBefore(today)) {
                charge.setValorPago(BigDecimal.ZERO);
                charge.setStatus(ChargeStatus.ATRASADO);
            } else {
                charge.setValorPago(BigDecimal.ZERO);
                charge.setStatus(ChargeStatus.PENDENTE);
            }
            chargeRepository.save(charge);
        }
        syncContractFromCharges(contract);
    }

    private List<LoanEngine.Installment> equalSplit(BigDecimal valorTotal, int total, LocalDate firstDue, String period) {
        BigDecimal parcela = valorTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        List<LoanEngine.Installment> rows = new ArrayList<>();
        BigDecimal saldo = valorTotal;
        for (int i = 1; i <= total; i++) {
            BigDecimal value = i == total ? saldo : parcela.min(saldo);
            saldo = saldo.subtract(value).max(BigDecimal.ZERO);
            LocalDate due = switch (period) {
                case "semanal" -> firstDue.plusWeeks(i - 1L);
                case "quinzenal" -> firstDue.plusDays((i - 1L) * 15);
                case "diario" -> firstDue.plusDays(i - 1L);
                case "anual" -> firstDue.plusYears(i - 1L);
                default -> firstDue.plusMonths(i - 1L);
            };
            rows.add(new LoanEngine.Installment(i, value, BigDecimal.ZERO, value, saldo, "normal", due));
        }
        return rows;
    }

    private void applyToCharge(Charge charge, BigDecimal amount) {
        Map<String, BigDecimal> due = new LinkedHashMap<>();
        due.put("juros", nz(charge.getValorJuros()).subtract(nz(charge.getPagoJuros())).max(BigDecimal.ZERO));
        due.put("multa", nz(charge.getMultaAplicada()).subtract(nz(charge.getPagoMulta())).max(BigDecimal.ZERO));
        due.put("encargos", nz(charge.getMoraAplicada()).subtract(nz(charge.getPagoEncargos())).max(BigDecimal.ZERO));
        BigDecimal principalDue = nz(charge.getValorPrincipal());
        if (principalDue.signum() <= 0) {
            principalDue = nz(charge.getValor())
                    .subtract(nz(charge.getValorJuros()))
                    .subtract(nz(charge.getMultaAplicada()))
                    .subtract(nz(charge.getMoraAplicada()))
                    .max(BigDecimal.ZERO);
        }
        due.put("principal", principalDue.subtract(nz(charge.getPagoPrincipal())).max(BigDecimal.ZERO));
        String order = charge.getContract() == null ? PaymentAllocator.DEFAULT_ORDER : charge.getContract().getOrdemPagamento();
        Map<String, BigDecimal> allocated = PaymentAllocator.allocate(amount, due, order);
        charge.setPagoJuros(nz(charge.getPagoJuros()).add(allocated.get("juros")));
        charge.setPagoMulta(nz(charge.getPagoMulta()).add(allocated.get("multa")));
        charge.setPagoEncargos(nz(charge.getPagoEncargos()).add(allocated.get("encargos")));
        charge.setPagoPrincipal(nz(charge.getPagoPrincipal()).add(allocated.get("principal")));
        BigDecimal paid = nz(charge.getValorPago()).add(amount);
        charge.setValorPago(paid);
        if (paid.compareTo(nz(charge.getValor())) >= 0) {
            charge.setValorPago(nz(charge.getValor()));
            charge.setStatus(ChargeStatus.PAGO);
        } else {
            charge.setStatus(charge.getVencimento() != null && charge.getVencimento().isBefore(LocalDate.now())
                    ? ChargeStatus.ATRASADO
                    : ChargeStatus.PARCIAL);
        }
    }

    private void syncContractFromCharges(Contract contract) {
        List<Charge> charges = chargeRepository.findByContractIdOrderByNumeroAsc(contract.getId());
        BigDecimal paid = charges.stream().map(c -> nz(c.getValorPago())).reduce(BigDecimal.ZERO, BigDecimal::add);
        int paidCount = (int) charges.stream().filter(c -> c.getStatus() == ChargeStatus.PAGO).count();
        BigDecimal remaining = charges.stream()
                .filter(c -> c.getStatus() != ChargeStatus.CANCELADO && c.getStatus() != ChargeStatus.RENEGOCIADA)
                .map(Charge::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        contract.setValorPago(paid);
        contract.setParcelasPagas(paidCount);
        contract.setSaldoDevedor(remaining);
        Charge next = charges.stream()
                .filter(c -> OPEN.contains(c.getStatus()))
                .min(Comparator.comparing(Charge::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        contract.setProximoPagamento(next == null ? null : next.getVencimento());
        if (remaining.signum() == 0) {
            if (contract.getStatus() != ContractStatus.RENEGOCIADO && contract.getStatus() != ContractStatus.FALECIMENTO) {
                contract.setStatus(ContractStatus.ENCERRADO);
            }
        } else if (charges.stream().anyMatch(c -> c.getStatus() == ChargeStatus.ATRASADO)) {
            contract.setStatus(ContractStatus.ATRASADO);
        } else if (contract.getStatus() == ContractStatus.ENCERRADO || contract.getStatus() == ContractStatus.FALECIMENTO) {
            // keep terminal statuses unless remaining exists
        } else if (paidCount > 0) {
            contract.setStatus(ContractStatus.ATIVO);
        }
        contractRepository.save(contract);
    }

    private List<Charge> openCharges(Long contractId) {
        return chargeRepository.findByContractIdOrderByNumeroAsc(contractId).stream()
                .filter(c -> OPEN.contains(c.getStatus()))
                .toList();
    }

    private Charge require(Long id) {
        return chargeRepository.findById(id).orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
    }

    private boolean applyLateCharges(Charge charge, LocalDate today) {
        if (charge.getStatus() != ChargeStatus.ATRASADO && charge.getStatus() != ChargeStatus.PARCIAL) {
            return false;
        }
        if (charge.getStatus() == ChargeStatus.PAGO || charge.getStatus() == ChargeStatus.CANCELADO) {
            return false;
        }
        if (charge.getVencimento() == null || !charge.getVencimento().isBefore(today)) {
            return false;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(charge.getVencimento(), today);
        AppSettings settings = settings();
        int grace = settings.getGraceDays() == null ? 0 : settings.getGraceDays();
        if (days <= grace) {
            return false;
        }
        BigDecimal base = nz(charge.getValorBase()).signum() == 0 ? nz(charge.getValor()) : nz(charge.getValorBase());
        if (base.signum() <= 0) {
            return false;
        }
        BigDecimal multa = base.multiply(nz(settings.getLateFeePercent())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal mora = base.multiply(nz(settings.getMoraPercentPerDay())).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(days - grace)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newValor = base.add(multa).add(mora);
        boolean changed = nz(charge.getMultaAplicada()).compareTo(multa) != 0
                || nz(charge.getMoraAplicada()).compareTo(mora) != 0
                || nz(charge.getValor()).compareTo(newValor) != 0;
        if (!changed) {
            return false;
        }
        boolean firstMulta = nz(charge.getMultaAplicada()).signum() == 0 && multa.signum() > 0;
        charge.setValorBase(base);
        charge.setMultaAplicada(multa);
        charge.setMoraAplicada(mora);
        charge.setValor(newValor);
        if (firstMulta) {
            ledger("multa", multa, charge, "Multa de atraso após " + days + " dia(s).");
        }
        return true;
    }

    private void ledger(String tipo, BigDecimal valor, Charge charge, String motivo) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTipo(tipo);
        entry.setValor(valor);
        entry.setMotivo(motivo);
        if (charge != null) {
            entry.setChargeId(charge.getId());
            entry.setContractId(charge.getContract() == null ? null : charge.getContract().getId());
        }
        ledgerEntryRepository.save(entry);
    }

    private void ledger(String tipo, BigDecimal valor, Contract contract, String motivo) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTipo(tipo);
        entry.setValor(valor);
        entry.setMotivo(motivo);
        entry.setContractId(contract.getId());
        ledgerEntryRepository.save(entry);
    }

    private Map<String, Object> ledgerMap(LedgerEntry entry) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", String.valueOf(entry.getId()));
        map.put("tipo", entry.getTipo());
        map.put("valor", entry.getValor());
        map.put("motivo", entry.getMotivo());
        map.put("createdAt", entry.getCreatedAt());
        return map;
    }

    private AppSettings settings() {
        return settingsRepository.findById(1L).orElseGet(AppSettings::new);
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

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
