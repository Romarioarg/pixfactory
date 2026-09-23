package com.pixfactory.web;

import com.pixfactory.domain.User;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.*;
import com.pixfactory.service.CatalogService;
import com.pixfactory.service.CashService;
import com.pixfactory.service.ChargeService;
import com.pixfactory.service.ClientService;
import com.pixfactory.service.ContractService;
import com.pixfactory.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DashboardService dashboardService;
    private final CatalogService catalogService;
    private final ClientService clientService;
    private final ContractService contractService;
    private final ChargeService chargeService;
    private final CashService cashService;
    private final DtoMapper mapper;
    private final LaunchRepository launchRepository;
    private final PayableRepository payableRepository;
    private final ReceivableRepository receivableRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final BankConnectionRepository bankConnectionRepository;
    private final SuggestionRepository suggestionRepository;

    public ApiController(
            DashboardService dashboardService,
            CatalogService catalogService,
            ClientService clientService,
            ContractService contractService,
            ChargeService chargeService,
            CashService cashService,
            DtoMapper mapper,
            LaunchRepository launchRepository,
            PayableRepository payableRepository,
            ReceivableRepository receivableRepository,
            BudgetRepository budgetRepository,
            AccountRepository accountRepository,
            BankConnectionRepository bankConnectionRepository,
            SuggestionRepository suggestionRepository
    ) {
        this.dashboardService = dashboardService;
        this.catalogService = catalogService;
        this.clientService = clientService;
        this.contractService = contractService;
        this.chargeService = chargeService;
        this.cashService = cashService;
        this.mapper = mapper;
        this.launchRepository = launchRepository;
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.budgetRepository = budgetRepository;
        this.accountRepository = accountRepository;
        this.bankConnectionRepository = bankConnectionRepository;
        this.suggestionRepository = suggestionRepository;
    }

    @GetMapping("/bootstrap")
    public Map<String, Object> bootstrap(@AuthenticationPrincipal User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", mapper.user(user));
        data.put("clients", clientService.findAll());
        data.put("contracts", contractService.findAll());
        data.put("charges", chargeService.findAll());
        data.put("cobrancas", data.get("charges"));
        data.put("launches", catalogService.launches());
        data.put("payables", catalogService.payables());
        data.put("receivables", catalogService.receivables());
        data.put("budgets", catalogService.budgets());
        data.put("accounts", catalogService.accounts());
        data.put("banks", catalogService.banks());
        data.put("notifications", catalogService.notifications());
        data.put("suggestions", catalogService.suggestions());
        data.put("appointments", catalogService.appointments());
        data.put("settings", catalogService.settings());
        data.put("activity", catalogService.activity());
        data.put("payments", contractService.payments());
        data.put("emails", catalogService.emails());
        data.put("metrics", dashboardService.metrics());
        data.put("caixa", cashService.today());
        return data;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>(dashboardService.metrics());
        data.put("activity", catalogService.activity());
        return data;
    }

    @GetMapping("/reports")
    public Map<String, Object> reports() {
        return dashboardService.reports();
    }

    @GetMapping("/launches")
    public Object launches() { return stringifyIds(catalogService.launches()); }
    @PostMapping("/launches")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createLaunch(@RequestBody Map<String, Object> body) { return catalogService.createLaunch(body); }
    @DeleteMapping("/launches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLaunch(@PathVariable Long id) { catalogService.deleteLaunch(id); }

    @GetMapping("/expenses")
    public Object expenses() { return stringifyIds(catalogService.payables()); }
    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createExpense(@RequestBody Map<String, Object> body) { return catalogService.createPayable(body); }
    @PutMapping("/expenses/{id}")
    public Object updateExpense(@PathVariable Long id, @RequestBody Map<String, Object> body) { return catalogService.updatePayable(id, body); }
    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id) { catalogService.deletePayable(id); }

    @GetMapping("/payables")
    public Object payables() { return stringifyIds(catalogService.payables()); }
    @PostMapping("/payables")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createPayable(@RequestBody Map<String, Object> body) { return catalogService.createPayable(body); }
    @PutMapping("/payables/{id}")
    public Object updatePayable(@PathVariable Long id, @RequestBody Map<String, Object> body) { return catalogService.updatePayable(id, body); }
    @DeleteMapping("/payables/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayable(@PathVariable Long id) { catalogService.deletePayable(id); }

    @GetMapping("/receivables")
    public Object receivables() { return stringifyIds(catalogService.receivables()); }
    @PostMapping("/receivables")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createReceivable(@RequestBody Map<String, Object> body) { return catalogService.createReceivable(body); }
    @PutMapping("/receivables/{id}")
    public Object updateReceivable(@PathVariable Long id, @RequestBody Map<String, Object> body) { return catalogService.updateReceivable(id, body); }
    @DeleteMapping("/receivables/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReceivable(@PathVariable Long id) { catalogService.deleteReceivable(id); }

    @GetMapping("/budgets")
    public Object budgets() { return stringifyIds(catalogService.budgets()); }
    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createBudget(@RequestBody Map<String, Object> body) { return catalogService.createBudget(body); }
    @PutMapping("/budgets/{id}")
    public Object updateBudget(@PathVariable Long id, @RequestBody Map<String, Object> body) { return catalogService.updateBudget(id, body); }
    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable Long id) { catalogService.deleteBudget(id); }

    @GetMapping("/accounts")
    public Object accounts() { return stringifyIds(catalogService.accounts()); }
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createAccount(@RequestBody Map<String, Object> body) { return catalogService.createAccount(body); }

    @GetMapping("/banks")
    public Object banks() { return stringifyIds(catalogService.banks()); }
    @PostMapping("/banks")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createBank(@RequestBody Map<String, Object> body) { return catalogService.createBank(body); }
    @DeleteMapping("/banks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBank(@PathVariable Long id) { catalogService.deleteBank(id); }

    @GetMapping("/open-finance/institutions")
    public List<Map<String, Object>> institutions() { return catalogService.openFinanceInstitutions(); }
    @GetMapping("/open-finance/snapshot/{code}")
    public Map<String, Object> snapshot(@PathVariable String code) { return catalogService.openFinanceSnapshot(code); }

    @GetMapping("/notifications")
    public List<Map<String, Object>> notifications() { return catalogService.notifications(); }
    @PutMapping("/notifications/{id}")
    public Map<String, Object> updateNotification(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return catalogService.updateNotification(id, body);
    }
    @DeleteMapping("/notifications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable Long id) { catalogService.deleteNotification(id); }
    @PostMapping("/notifications/read-all")
    public Map<String, String> readAll() {
        catalogService.markAllRead();
        return Map.of("message", "ok");
    }
    @DeleteMapping("/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearNotifications() { catalogService.clearNotifications(); }

    @GetMapping("/appointments")
    public List<Map<String, Object>> appointments() { return catalogService.appointments(); }
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAppointment(@RequestBody Map<String, Object> body) { return catalogService.createAppointment(body); }
    @PutMapping("/appointments/{id}")
    public Map<String, Object> updateAppointment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return catalogService.updateAppointment(id, body);
    }
    @DeleteMapping("/appointments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAppointment(@PathVariable Long id) { catalogService.deleteAppointment(id); }

    @GetMapping("/settings")
    public Map<String, Object> settings() { return catalogService.settings(); }
    @PutMapping("/settings")
    public Map<String, Object> saveSettings(@RequestBody Map<String, Object> body) { return catalogService.saveSettings(body); }

    @PostMapping("/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    public Object suggestion(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        return catalogService.createSuggestion(user.getId(), String.valueOf(body.get("text")));
    }

    @GetMapping("/emails")
    public List<Map<String, Object>> emails() { return catalogService.emails(); }

    private List<?> stringifyIds(List<?> items) {
        return items;
    }
}
