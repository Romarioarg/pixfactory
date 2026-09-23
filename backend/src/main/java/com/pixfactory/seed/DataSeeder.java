package com.pixfactory.seed;

import com.pixfactory.domain.*;
import com.pixfactory.mapper.DtoMapper;
import com.pixfactory.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final LaunchRepository launchRepository;
    private final PayableRepository payableRepository;
    private final ReceivableRepository receivableRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final BankConnectionRepository bankConnectionRepository;
    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppSettingsRepository settingsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;

    @Value("${pixfactory.seed:true}")
    private boolean seedEnabled;

    public DataSeeder(
            UserRepository userRepository,
            ClientRepository clientRepository,
            ContractRepository contractRepository,
            LaunchRepository launchRepository,
            PayableRepository payableRepository,
            ReceivableRepository receivableRepository,
            BudgetRepository budgetRepository,
            AccountRepository accountRepository,
            BankConnectionRepository bankConnectionRepository,
            NotificationRepository notificationRepository,
            AppointmentRepository appointmentRepository,
            AppSettingsRepository settingsRepository,
            ActivityLogRepository activityLogRepository,
            PasswordEncoder passwordEncoder,
            DtoMapper mapper
    ) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.launchRepository = launchRepository;
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.budgetRepository = budgetRepository;
        this.accountRepository = accountRepository;
        this.bankConnectionRepository = bankConnectionRepository;
        this.notificationRepository = notificationRepository;
        this.appointmentRepository = appointmentRepository;
        this.settingsRepository = settingsRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate plus5 = today.plusDays(5);
        LocalDate plus10 = today.plusDays(10);
        LocalDate plus20 = today.plusDays(20);
        LocalDate minus5 = today.minusDays(5);
        LocalDate minus15 = today.minusDays(15);

        User admin = user("Anderson Demo", "admin@pixfactory.app", "11999990000", "Admin@123", Role.ADMIN, "premium");
        user("Usuário Demo", "demo@pixfactory.app", "11988887777", "Demo@123", Role.USER, "gratuito");

        Client c1 = client("João da Silva", "529.982.247-25", "11987654321", "joao.silva@example.com",
                "Rua das Flores, 123 - Centro, SP", "1988-03-12", "Autônomo", "4500", "Nubank Demo", "Médio", "atrasado",
                "https://randomuser.me/api/portraits/men/1.jpg", minus15);
        Client c2 = client("Maria Oliveira", "390.533.447-05", "11998877665", "maria.oliveira@example.com",
                "Av. Paulista, 1500 - Bela Vista, SP", "1992-07-22", "Analista", "7200", "Itaú Demo", "Bom pagador", "ativo",
                "https://randomuser.me/api/portraits/women/2.jpg", today.minusDays(40));
        Client c3 = client("Pedro Souza", "111.444.777-35", "11912345678", "pedro.souza@example.com",
                "Rua Augusta, 200 - Consolação, SP", "1985-01-09", "Motorista", "3200", "Banco Demo", "Médio", "pendente",
                "https://randomuser.me/api/portraits/men/32.jpg", today.minusDays(8));
        Client c4 = client("Ana Costa", "853.513.468-93", "11966660004", "ana.costa@example.com",
                "Rua Harmonia, 50 - Vila Madalena, SP", "1995-11-02", "Designer", "5100", "Inter Demo", "Bom pagador", "encerrado",
                "https://randomuser.me/api/portraits/women/44.jpg", today.minusDays(90));
        client("Carlos Ferreira", "248.438.034-80", "11955551111", "carlos.ferreira@example.com",
                "Rua Vergueiro, 900 - Liberdade, SP", "1979-05-18", "Comerciante", "8000", "Bradesco Demo", "Bom pagador", "ativo",
                "https://randomuser.me/api/portraits/men/55.jpg", today);

        contract(c1, "Empréstimo", "1500", "250", 1, 6, minus5, ContractStatus.ATRASADO, "2.5", "20", "1250");
        contract(c2, "Financiamento", "5000", "1250", 3, 12, plus5, ContractStatus.ATIVO, "1.8", "15", "3750");
        contract(c3, "Serviço", "800", "0", 0, 3, plus10, ContractStatus.PENDENTE, "0", "0", "800");
        contract(c4, "Empréstimo", "2000", "2000", 6, 6, null, ContractStatus.ENCERRADO, "2", "0", "0");

        Launch l1 = new Launch();
        l1.setTipo("despesa"); l1.setDescricao("Aluguel do escritório"); l1.setValor(new BigDecimal("1200")); l1.setData(plus10); l1.setConta("Caixa");
        Launch l2 = new Launch();
        l2.setTipo("receita"); l2.setDescricao("Recebimento de parcela"); l2.setValor(new BigDecimal("416.67")); l2.setData(minus5); l2.setConta("Nubank Demo");
        launchRepository.saveAll(List.of(l1, l2));

        Payable p1 = new Payable(); p1.setDescricao("Aluguel"); p1.setValor(new BigDecimal("1200")); p1.setVencimento(plus10); p1.setStatus("pendente");
        Payable p2 = new Payable(); p2.setDescricao("Internet"); p2.setValor(new BigDecimal("150")); p2.setVencimento(plus20); p2.setStatus("pendente");
        payableRepository.saveAll(List.of(p1, p2));

        Receivable r1 = new Receivable(); r1.setDescricao("Parcela Maria Oliveira"); r1.setValor(new BigDecimal("416.67")); r1.setVencimento(plus5); r1.setStatus("pendente");
        Receivable r2 = new Receivable(); r2.setDescricao("Freelance"); r2.setValor(new BigDecimal("800")); r2.setVencimento(plus20); r2.setStatus("pendente");
        receivableRepository.saveAll(List.of(r1, r2));

        Budget b1 = new Budget(); b1.setCategoria("Operacional"); b1.setLimite(new BigDecimal("3000")); b1.setGasto(new BigDecimal("1350"));
        Budget b2 = new Budget(); b2.setCategoria("Marketing"); b2.setLimite(new BigDecimal("800")); b2.setGasto(new BigDecimal("220"));
        budgetRepository.saveAll(List.of(b1, b2));

        Account a1 = new Account(); a1.setNome("Caixa"); a1.setSaldo(new BigDecimal("2400"));
        Account a2 = new Account(); a2.setNome("Nubank Demo"); a2.setSaldo(new BigDecimal("1850"));
        accountRepository.saveAll(List.of(a1, a2));

        BankConnection bank = new BankConnection();
        bank.setNome("Nubank Demo"); bank.setAgencia("0001"); bank.setConta("12345-6"); bank.setConnected(true);
        bank.setSaldo(new BigDecimal("8450.00")); bank.setInstitutionCode("NUBANK-DEMO");
        bankConnectionRepository.save(bank);

        notify("Parcela em atraso", "João da Silva está com parcela vencida.", minus5, false);
        notify("Novo cliente", "Carlos Ferreira foi cadastrado hoje.", today, false);
        notify("Pagamento recebido", "Maria Oliveira pagou uma parcela.", minus15, true);

        Appointment ap = new Appointment();
        ap.setTitle("Cobrança João da Silva");
        ap.setNotes("Ligar e oferecer Pix Demo.");
        ap.setDate(minus5);
        ap.setClientId(c1.getId());
        appointmentRepository.save(ap);

        settingsRepository.save(new AppSettings());

        activity("Parcela de João da Silva venceu", "atraso", minus5);
        activity("Maria Oliveira pagou parcela", "pagamento", minus15);
        activity("Novo cliente cadastrado: Carlos Ferreira", "novo-cliente", today);
        activity("Seed DEMO aplicado por " + admin.getEmail(), "sistema", today);
    }

    private User user(String name, String email, String phone, String password, Role role, String plan) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setPlan(plan);
        user.setStatus(AccountStatus.ATIVO);
        user.setAddress("São Paulo, SP (DEMO)");
        return userRepository.save(user);
    }

    private Client client(String nome, String cpf, String telefone, String email, String endereco, String nascimento,
                          String profissao, String renda, String banco, String classificacao, String status, String foto, LocalDate criado) {
        Client client = new Client();
        client.setNome(nome);
        client.setCpf(cpf);
        client.setTelefone(telefone);
        client.setEmail(email);
        client.setEndereco(endereco);
        client.setDataNascimento(LocalDate.parse(nascimento));
        client.setProfissao(profissao);
        client.setRendaMensal(new BigDecimal(renda));
        client.setBanco(banco);
        client.setClassificacao(classificacao);
        client.setStatus(status);
        client.setFotoUrl(foto);
        client.setCriadoEm(criado);
        client.setHistoricoJson(mapper.writeList(List.of(Map.of("tipo", "cadastro", "data", criado.toString(), "info", "Cliente cadastrado"))));
        return clientRepository.save(client);
    }

    private void contract(Client client, String tipo, String total, String pago, int pagas, int totais, LocalDate proximo,
                          ContractStatus status, String juros, String multa, String saldo) {
        Contract contract = new Contract();
        contract.setClient(client);
        contract.setTipo(tipo);
        contract.setValorTotal(new BigDecimal(total));
        contract.setValorPago(new BigDecimal(pago));
        contract.setParcelasPagas(pagas);
        contract.setParcelasTotais(totais);
        contract.setProximoPagamento(proximo);
        contract.setStatus(status);
        contract.setJuros(new BigDecimal(juros));
        contract.setMulta(new BigDecimal(multa));
        contract.setSaldoDevedor(new BigDecimal(saldo));
        contract.setHistoricoJson(mapper.writeList(List.of(Map.of("data", LocalDate.now().minusDays(10).toString(), "descricao", "Contrato criado (seed DEMO)"))));
        contractRepository.save(contract);
    }

    private void notify(String title, String body, LocalDate date, boolean read) {
        AppNotification n = new AppNotification();
        n.setTitle(title);
        n.setBody(body);
        n.setCreatedAt(date);
        n.setReadFlag(read);
        notificationRepository.save(n);
    }

    private void activity(String text, String type, LocalDate date) {
        ActivityLog log = new ActivityLog();
        log.setText(text);
        log.setType(type);
        log.setDate(date);
        activityLogRepository.save(log);
    }
}
