package com.pixfactory.service;

import com.pixfactory.domain.*;
import com.pixfactory.exception.NotFoundException;
import com.pixfactory.integration.OpenFinanceService;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class CatalogService {
    private final LaunchRepository launchRepository;
    private final PayableRepository payableRepository;
    private final ReceivableRepository receivableRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final BankConnectionRepository bankConnectionRepository;
    private final NotificationRepository notificationRepository;
    private final SuggestionRepository suggestionRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppSettingsRepository settingsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final OpenFinanceService openFinanceService;
    private final DtoMapper mapper;

    public CatalogService(
            LaunchRepository launchRepository,
            PayableRepository payableRepository,
            ReceivableRepository receivableRepository,
            BudgetRepository budgetRepository,
            AccountRepository accountRepository,
            BankConnectionRepository bankConnectionRepository,
            NotificationRepository notificationRepository,
            SuggestionRepository suggestionRepository,
            AppointmentRepository appointmentRepository,
            AppSettingsRepository settingsRepository,
            ActivityLogRepository activityLogRepository,
            EmailMessageRepository emailMessageRepository,
            OpenFinanceService openFinanceService,
            DtoMapper mapper
    ) {
        this.launchRepository = launchRepository;
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.budgetRepository = budgetRepository;
        this.accountRepository = accountRepository;
        this.bankConnectionRepository = bankConnectionRepository;
        this.notificationRepository = notificationRepository;
        this.suggestionRepository = suggestionRepository;
        this.appointmentRepository = appointmentRepository;
        this.settingsRepository = settingsRepository;
        this.activityLogRepository = activityLogRepository;
        this.emailMessageRepository = emailMessageRepository;
        this.openFinanceService = openFinanceService;
        this.mapper = mapper;
    }

    public List<Launch> launches() { return launchRepository.findAll(); }
    public Launch createLaunch(Map<String, Object> body) {
        Launch item = new Launch();
        item.setTipo(String.valueOf(body.getOrDefault("tipo", "despesa")));
        item.setDescricao(String.valueOf(body.get("descricao")));
        item.setValor(decimal(body.get("valor")));
        item.setData(date(body.get("data")));
        item.setConta(String.valueOf(body.getOrDefault("conta", "Caixa")));
        return launchRepository.save(item);
    }
    public void deleteLaunch(Long id) { launchRepository.deleteById(id); }

    public List<Payable> payables() { return payableRepository.findAll(); }
    public Payable createPayable(Map<String, Object> body) {
        Payable item = new Payable();
        item.setDescricao(String.valueOf(body.get("descricao")));
        item.setValor(decimal(body.get("valor")));
        item.setVencimento(date(body.get("vencimento")));
        item.setStatus(String.valueOf(body.getOrDefault("status", "pendente")));
        return payableRepository.save(item);
    }
    public Payable updatePayable(Long id, Map<String, Object> body) {
        Payable item = payableRepository.findById(id).orElseThrow(() -> new NotFoundException("Conta a pagar não encontrada."));
        if (body.get("status") != null) item.setStatus(String.valueOf(body.get("status")));
        if (body.get("descricao") != null) item.setDescricao(String.valueOf(body.get("descricao")));
        if (body.get("valor") != null) item.setValor(decimal(body.get("valor")));
        return payableRepository.save(item);
    }
    public void deletePayable(Long id) { payableRepository.deleteById(id); }

    public List<Receivable> receivables() { return receivableRepository.findAll(); }
    public Receivable createReceivable(Map<String, Object> body) {
        Receivable item = new Receivable();
        item.setDescricao(String.valueOf(body.get("descricao")));
        item.setValor(decimal(body.get("valor")));
        item.setVencimento(date(body.get("vencimento")));
        item.setStatus(String.valueOf(body.getOrDefault("status", "pendente")));
        return receivableRepository.save(item);
    }
    public Receivable updateReceivable(Long id, Map<String, Object> body) {
        Receivable item = receivableRepository.findById(id).orElseThrow(() -> new NotFoundException("Recebível não encontrado."));
        if (body.get("status") != null) item.setStatus(String.valueOf(body.get("status")));
        if (body.get("descricao") != null) item.setDescricao(String.valueOf(body.get("descricao")));
        return receivableRepository.save(item);
    }
    public void deleteReceivable(Long id) { receivableRepository.deleteById(id); }

    public List<Budget> budgets() { return budgetRepository.findAll(); }
    public Budget createBudget(Map<String, Object> body) {
        Budget item = new Budget();
        item.setCategoria(String.valueOf(body.get("categoria")));
        item.setLimite(decimal(body.get("limite")));
        item.setGasto(decimal(body.getOrDefault("gasto", 0)));
        return budgetRepository.save(item);
    }
    public Budget updateBudget(Long id, Map<String, Object> body) {
        Budget item = budgetRepository.findById(id).orElseThrow(() -> new NotFoundException("Limite não encontrado."));
        if (body.get("categoria") != null) item.setCategoria(String.valueOf(body.get("categoria")));
        if (body.get("limite") != null) item.setLimite(decimal(body.get("limite")));
        if (body.get("gasto") != null) item.setGasto(decimal(body.get("gasto")));
        return budgetRepository.save(item);
    }
    public void deleteBudget(Long id) { budgetRepository.deleteById(id); }

    public List<Account> accounts() { return accountRepository.findAll(); }
    public Account createAccount(Map<String, Object> body) {
        Account item = new Account();
        item.setTipo(String.valueOf(body.getOrDefault("tipo", "conta")));
        item.setNome(String.valueOf(body.get("nome")));
        item.setSaldo(decimal(body.getOrDefault("saldo", 0)));
        return accountRepository.save(item);
    }

    public List<BankConnection> banks() { return bankConnectionRepository.findAll(); }
    public BankConnection createBank(Map<String, Object> body) {
        BankConnection item = new BankConnection();
        item.setNome(String.valueOf(body.get("nome")));
        item.setAgencia(String.valueOf(body.getOrDefault("agencia", "0001")));
        item.setConta(String.valueOf(body.getOrDefault("conta", "00000-0")));
        item.setConnected(true);
        item.setInstitutionCode(String.valueOf(body.getOrDefault("institutionCode", "DEMOBANK")));
        Map<String, Object> snapshot = openFinanceService.snapshot(item.getInstitutionCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) snapshot.get("account");
        if (account != null && account.get("balance") != null) {
            item.setSaldo(decimal(account.get("balance")));
        }
        notify("Open Finance DEMO", "Conta " + item.getNome() + " conectada (simulação).");
        return bankConnectionRepository.save(item);
    }
    public void deleteBank(Long id) { bankConnectionRepository.deleteById(id); }
    public List<Map<String, Object>> openFinanceInstitutions() { return openFinanceService.institutions(); }
    public Map<String, Object> openFinanceSnapshot(String code) { return openFinanceService.snapshot(code); }

    public List<Map<String, Object>> notifications() {
        return notificationRepository.findAll().stream().map(mapper::notification).toList();
    }
    public Map<String, Object> updateNotification(Long id, Map<String, Object> body) {
        AppNotification item = notificationRepository.findById(id).orElseThrow(() -> new NotFoundException("Notificação não encontrada."));
        if (body.get("read") != null) item.setReadFlag(Boolean.parseBoolean(String.valueOf(body.get("read"))));
        return mapper.notification(notificationRepository.save(item));
    }
    public void deleteNotification(Long id) { notificationRepository.deleteById(id); }
    @Transactional
    public void markAllRead() {
        notificationRepository.findAll().forEach(n -> n.setReadFlag(true));
    }
    @Transactional
    public void clearNotifications() { notificationRepository.deleteAll(); }

    public Suggestion createSuggestion(Long userId, String text) {
        Suggestion item = new Suggestion();
        item.setUserId(userId);
        item.setText(text);
        return suggestionRepository.save(item);
    }
    public List<Suggestion> suggestions() { return suggestionRepository.findAll(); }

    public List<Map<String, Object>> appointments() {
        return appointmentRepository.findAll().stream().map(mapper::appointment).toList();
    }
    public Map<String, Object> createAppointment(Map<String, Object> body) {
        Appointment item = new Appointment();
        item.setTitle(String.valueOf(body.get("title")));
        item.setNotes(body.get("notes") == null ? "" : String.valueOf(body.get("notes")));
        item.setDate(date(body.get("date")));
        if (body.get("clientId") != null) item.setClientId(Long.parseLong(String.valueOf(body.get("clientId"))));
        if (body.get("contractId") != null) item.setContractId(Long.parseLong(String.valueOf(body.get("contractId"))));
        item.setStatus(String.valueOf(body.getOrDefault("status", "agendado")));
        notify("Novo agendamento", item.getTitle() + " em " + item.getDate() + ".");
        return mapper.appointment(appointmentRepository.save(item));
    }
    public Map<String, Object> updateAppointment(Long id, Map<String, Object> body) {
        Appointment item = appointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
        if (body.get("title") != null) item.setTitle(String.valueOf(body.get("title")));
        if (body.get("notes") != null) item.setNotes(String.valueOf(body.get("notes")));
        if (body.get("date") != null) item.setDate(date(body.get("date")));
        if (body.get("status") != null) item.setStatus(String.valueOf(body.get("status")));
        return mapper.appointment(appointmentRepository.save(item));
    }
    public void deleteAppointment(Long id) { appointmentRepository.deleteById(id); }

    public Map<String, Object> settings() {
        AppSettings settings = settingsRepository.findById(1L).orElseGet(() -> settingsRepository.save(new AppSettings()));
        return settingsMap(settings);
    }

    public Map<String, Object> saveSettings(Map<String, Object> body) {
        AppSettings settings = settingsRepository.findById(1L).orElseGet(AppSettings::new);
        settings.setId(1L);
        if (body.get("companyName") != null) settings.setCompanyName(String.valueOf(body.get("companyName")));
        if (body.get("slogan") != null) settings.setSlogan(String.valueOf(body.get("slogan")));
        if (body.get("defaultInterestRate") != null) settings.setDefaultInterestRate(decimal(body.get("defaultInterestRate")));
        if (body.get("emailNotifications") != null) settings.setEmailNotifications(Boolean.parseBoolean(String.valueOf(body.get("emailNotifications"))));
        if (body.get("paymentReminders") != null) settings.setPaymentReminders(Boolean.parseBoolean(String.valueOf(body.get("paymentReminders"))));
        if (body.get("hideValues") != null) settings.setHideValues(Boolean.parseBoolean(String.valueOf(body.get("hideValues"))));
        if (body.get("theme") != null) settings.setTheme(String.valueOf(body.get("theme")));
        if (body.get("lateFeePercent") != null) settings.setLateFeePercent(decimal(body.get("lateFeePercent")));
        if (body.get("moraPercentPerDay") != null) settings.setMoraPercentPerDay(decimal(body.get("moraPercentPerDay")));
        if (body.get("earlyPayoffDiscountPercent") != null) settings.setEarlyPayoffDiscountPercent(decimal(body.get("earlyPayoffDiscountPercent")));
        if (body.get("graceDays") != null) settings.setGraceDays(Integer.parseInt(String.valueOf(body.get("graceDays"))));
        if (body.get("cobrancaEscada") != null) settings.setCobrancaEscada(String.valueOf(body.get("cobrancaEscada")));
        return settingsMap(settingsRepository.save(settings));
    }

    public List<ActivityLog> activity() { return activityLogRepository.findTop20ByOrderByIdDesc(); }
    public List<Map<String, Object>> emails() {
        return emailMessageRepository.findAll().stream().map(mapper::email).toList();
    }

    public Map<String, Object> withId(Object entity, Function<Object, Long> idFn, Map<String, Object> extra) {
        Map<String, Object> map = extra == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extra);
        map.put("id", String.valueOf(idFn.apply(entity)));
        return map;
    }

    private Map<String, Object> settingsMap(AppSettings settings) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("companyName", settings.getCompanyName());
        map.put("slogan", settings.getSlogan());
        map.put("defaultInterestRate", settings.getDefaultInterestRate());
        map.put("referralReward", settings.getReferralReward());
        map.put("emailNotifications", settings.isEmailNotifications());
        map.put("paymentReminders", settings.isPaymentReminders());
        map.put("theme", settings.getTheme());
        map.put("hideValues", settings.isHideValues());
        map.put("lateFeePercent", settings.getLateFeePercent());
        map.put("moraPercentPerDay", settings.getMoraPercentPerDay());
        map.put("earlyPayoffDiscountPercent", settings.getEarlyPayoffDiscountPercent());
        map.put("graceDays", settings.getGraceDays());
        map.put("cobrancaEscada", settings.getCobrancaEscada());
        return map;
    }

    private void notify(String title, String body) {
        AppNotification notification = new AppNotification();
        notification.setTitle(title);
        notification.setBody(body);
        notificationRepository.save(notification);
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }

    private LocalDate date(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return LocalDate.now();
        return LocalDate.parse(String.valueOf(value));
    }
}
