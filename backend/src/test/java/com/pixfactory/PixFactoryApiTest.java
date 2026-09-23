package com.pixfactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixfactory.repo.ClientRepository;
import com.pixfactory.repo.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PixFactoryApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ClientRepository clientRepository;
    @Autowired PaymentRepository paymentRepository;

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void loginSuccessAndMe() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@pixfactory.app"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "demo@pixfactory.app", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotListUsers() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void clientCrud() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        MvcResult created = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Cliente Teste",
                                "cpf", "191.000.000-00",
                                "telefone", "11977776666",
                                "email", "cliente.teste@example.com",
                                "endereco", "Rua Demo, 1",
                                "classificacao", "Médio"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/clients/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Cliente Teste Editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente Teste Editado"));

        mockMvc.perform(get("/api/clients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/clients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/clients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateCpfIsConflict() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        String cpf = clientRepository.findAll().get(0).getCpf();
        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Duplicado",
                                "cpf", cpf,
                                "telefone", "11911112222",
                                "email", "dup@example.com",
                                "endereco", "Rua X",
                                "classificacao", "Médio"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void dashboardUsesDatabase() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientesAtivos").isNumber())
                .andExpect(jsonPath("$.totalEmprestado").isNumber());
    }

    @Test
    void pixDemoCreatesAndConfirmsPayment() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        MvcResult contracts = mockMvc.perform(get("/api/contracts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String contractId = objectMapper.readTree(contracts.getResponse().getContentAsString()).get(0).get("id").asText();

        MvcResult paymentResult = mockMvc.perform(post("/api/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("method", "PIX", "valor", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("DEMO"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.txid").exists())
                .andReturn();
        String paymentId = objectMapper.readTree(paymentResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        assertThat(paymentRepository.findById(Long.valueOf(paymentId)).orElseThrow().getStatus().name()).isEqualTo("PAID");
    }

    @Test
    void openFinanceDemo() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/open-finance/institutions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].demo").value(true));
        mockMvc.perform(get("/api/open-finance/snapshot/DEMOBANK").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.balance").value(8450.00));
    }

    @Test
    void appointmentCrud() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Reunião DEMO",
                                "date", "2026-08-20",
                                "notes", "Cobrança"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(put("/api/appointments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Reunião DEMO 2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reunião DEMO 2"));
        mockMvc.perform(delete("/api/appointments/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void reportsHaveSeries() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.insight").isString());
    }

    @Test
    void unknownContractIs404() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(get("/api/contracts/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cobrancasAreGeneratedAndPartialPaymentWorks() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        mockMvc.perform(get("/api/cobrancas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].valor").exists());

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").exists());

        MvcResult overdue = mockMvc.perform(get("/api/cobrancas/atrasadas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var list = objectMapper.readTree(overdue.getResponse().getContentAsString());
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThan(0);
        String chargeId = list.get(0).get("id").asText();
        double saldo = list.get(0).get("saldo").asDouble();
        double partial = Math.max(1, Math.floor(saldo / 2));

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/pagamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", saldo + 50))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("excedente"));

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/pagamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", partial))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorPago").isNumber());
    }

    @Test
    void simulationPriceAndInterestOnly() throws Exception {
        String token = login("demo@pixfactory.app", "Demo@123");
        mockMvc.perform(post("/api/simulacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valor", 1000, "juros", 2.5, "parcelas", 4, "sistema", "price"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sistema").value("price"))
                .andExpect(jsonPath("$.cronograma").isArray())
                .andExpect(jsonPath("$.parcelaInicial").isNumber());

        mockMvc.perform(post("/api/simulacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valor", 1000, "juros", 10, "parcelas", 3, "modo", "so_juros"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronograma[0].tipo").value("so_juros"))
                .andExpect(jsonPath("$.cronograma[2].tipo").value("balloon"));

        mockMvc.perform(post("/api/simulacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valor", 1000, "juros", 20, "parcelas", 3, "modo", "juros_rotativo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sistema").value("so_juros_aberto"))
                .andExpect(jsonPath("$.cronograma[0].parcela").value(200))
                .andExpect(jsonPath("$.cronograma[0].tipo").value("so_juros"))
                .andExpect(jsonPath("$.cronograma[3].tipo").value("principal"))
                .andExpect(jsonPath("$.cronograma[3].parcela").value(1000))
                .andExpect(jsonPath("$.prazoAberto").value(true));
    }

    @Test
    void searchReceiptReverseDuplicateAndEarlyPayoff() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        mockMvc.perform(get("/api/busca").param("q", "a").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes").isArray());

        MvcResult createdClient = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Cliente Operacional",
                                "cpf", "191.222.333-44",
                                "telefone", "11988887777",
                                "email", "ops.teste@example.com",
                                "endereco", "Rua Operacional, 10",
                                "classificacao", "Médio"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String clientId = objectMapper.readTree(createdClient.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/busca").param("q", "Operacional").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes[?(@.nome=='Cliente Operacional')]").isNotEmpty());

        Map<String, Object> contractBody = new java.util.LinkedHashMap<>();
        contractBody.put("clienteId", clientId);
        contractBody.put("tipo", "Empréstimo");
        contractBody.put("valorTotal", 200);
        contractBody.put("valorPago", 0);
        contractBody.put("parcelasPagas", 0);
        contractBody.put("parcelasTotais", 2);
        contractBody.put("proximoPagamento", "2026-09-15");
        contractBody.put("status", "pendente");
        contractBody.put("juros", 0);
        contractBody.put("multa", 0);
        contractBody.put("saldoDevedor", 200);
        contractBody.put("sistemaAmortizacao", "price");
        contractBody.put("modoPagamento", "parcela_cheia");
        contractBody.put("periodicidade", "mensal");
        MvcResult createdContract = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractBody)))
                .andExpect(status().isCreated())
                .andReturn();
        String contractId = objectMapper.readTree(createdContract.getResponse().getContentAsString()).get("id").asText();

        MvcResult chargesResult = mockMvc.perform(get("/api/cobrancas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var charges = objectMapper.readTree(chargesResult.getResponse().getContentAsString());
        String chargeId = null;
        for (var node : charges) {
            if (contractId.equals(node.get("contratoId").asText())) {
                chargeId = node.get("id").asText();
                break;
            }
        }
        assertThat(chargeId).isNotNull();

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/pagamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorPago").value(50));

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/pagamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 50))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/cobrancas/" + chargeId + "/recibo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reciboId").value("PF-" + chargeId))
                .andExpect(jsonPath("$.historico").isArray());

        mockMvc.perform(get("/api/cobrancas/" + chargeId + "/extrato").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("pagamento"));

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/estorno")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivo", "Teste de estorno"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorPago").value(0));

        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/contato")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "canal", "whatsapp",
                                "resultado", "mensagem enviada",
                                "proximoContato", "2026-08-20"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoFollowUp").value("2026-08-20"));

        mockMvc.perform(get("/api/cobrancas/follow-up").param("data", "2026-08-20").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chargeId));

        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lateFeePercent", 2,
                                "moraPercentPerDay", 0.033,
                                "earlyPayoffDiscountPercent", 5,
                                "graceDays", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateFeePercent").exists())
                .andExpect(jsonPath("$.earlyPayoffDiscountPercent").exists());

        mockMvc.perform(post("/api/contracts/" + contractId + "/actions/quitar_antecipado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("desconto", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("encerrado"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multaMoraAplicada").exists())
                .andExpect(jsonPath("$.followUpsHoje").isNumber());
    }

    @Test
    void cashOpenAndAssistant() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        mockMvc.perform(post("/api/caixa/abrir")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("saldoInicial", 5000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aberto").value(true));

        mockMvc.perform(post("/api/assistente")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pergunta", "Quanto tenho em caixa?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").isString());
    }

    @Test
    void openInterestAmortizeAndRollForward() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");

        MvcResult joao = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "João Giro",
                                "cpf", "512.888.777-66",
                                "telefone", "11970001111",
                                "email", "joao.giro@example.com",
                                "endereco", "Rua Giro, 100",
                                "classificacao", "Médio"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String joaoId = objectMapper.readTree(joao.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> joaoContract = new java.util.LinkedHashMap<>();
        joaoContract.put("clienteId", joaoId);
        joaoContract.put("tipo", "Empréstimo");
        joaoContract.put("tipoOperacao", "rotativo");
        joaoContract.put("valorTotal", 1000);
        joaoContract.put("valorPago", 0);
        joaoContract.put("parcelasPagas", 0);
        joaoContract.put("parcelasTotais", 1);
        joaoContract.put("proximoPagamento", "2026-10-15");
        joaoContract.put("status", "ativo");
        joaoContract.put("juros", 20);
        joaoContract.put("multa", 0);
        joaoContract.put("saldoDevedor", 1000);
        joaoContract.put("sistemaAmortizacao", "so_juros_aberto");
        joaoContract.put("modoPagamento", "juros_rotativo");
        joaoContract.put("periodicidade", "mensal");
        MvcResult createdJoao = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joaoContract)))
                .andExpect(status().isCreated())
                .andReturn();
        String joaoContractId = objectMapper.readTree(createdJoao.getResponse().getContentAsString()).get("id").asText();

        var before = objectMapper.readTree(mockMvc.perform(get("/api/cobrancas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        var firstInterest = findCharge(before, joaoContractId, "so_juros");
        var principal = findCharge(before, joaoContractId, "principal");
        assertThat(firstInterest).isNotNull();
        assertThat(principal).isNotNull();
        assertThat(firstInterest.get("valor").asDouble()).isEqualTo(200.0);

        mockMvc.perform(post("/api/contracts/" + joaoContractId + "/actions/amortizar_capital")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 300))))
                .andExpect(status().isOk());

        var afterPay = objectMapper.readTree(mockMvc.perform(get("/api/cobrancas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        var updatedInterest = findCharge(afterPay, joaoContractId, "so_juros");
        var updatedPrincipal = findCharge(afterPay, joaoContractId, "principal");
        assertThat(updatedInterest.get("valor").asDouble()).isEqualTo(140.0);
        assertThat(updatedPrincipal.get("valorPago").asDouble()).isEqualTo(300.0);
        assertThat(updatedPrincipal.get("saldo").asDouble()).isEqualTo(700.0);

        mockMvc.perform(post("/api/contracts/" + joaoContractId + "/actions/gerar_juros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        var afterGenerate = objectMapper.readTree(mockMvc.perform(get("/api/cobrancas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        int interestCount = 0;
        boolean foundNext = false;
        for (var node : afterGenerate) {
            if (joaoContractId.equals(node.get("contratoId").asText()) && "so_juros".equals(node.get("tipoParcela").asText())) {
                interestCount++;
                assertThat(node.get("valor").asDouble()).isEqualTo(140.0);
                if ("2026-11-15".equals(node.get("vencimento").asText())) {
                    foundNext = true;
                }
            }
        }
        assertThat(interestCount).isGreaterThanOrEqualTo(2);
        assertThat(foundNext).isTrue();

        MvcResult maria = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Maria Juro",
                                "cpf", "612.444.333-22",
                                "telefone", "11970002222",
                                "email", "maria.juro@example.com",
                                "endereco", "Rua Juro, 20",
                                "classificacao", "Alto"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String mariaId = objectMapper.readTree(maria.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> mariaContract = new java.util.LinkedHashMap<>();
        mariaContract.put("clienteId", mariaId);
        mariaContract.put("tipo", "Empréstimo");
        mariaContract.put("tipoOperacao", "rotativo");
        mariaContract.put("valorTotal", 1000);
        mariaContract.put("valorPago", 0);
        mariaContract.put("parcelasPagas", 0);
        mariaContract.put("parcelasTotais", 1);
        mariaContract.put("proximoPagamento", "2026-06-15");
        mariaContract.put("status", "ativo");
        mariaContract.put("juros", 20);
        mariaContract.put("multa", 0);
        mariaContract.put("saldoDevedor", 1000);
        mariaContract.put("sistemaAmortizacao", "so_juros_aberto");
        mariaContract.put("modoPagamento", "juros_rotativo");
        mariaContract.put("periodicidade", "mensal");
        MvcResult createdMaria = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mariaContract)))
                .andExpect(status().isCreated())
                .andReturn();
        String mariaContractId = objectMapper.readTree(createdMaria.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurosDoPeriodo").isArray())
                .andExpect(jsonPath("$.jurosDoPeriodo[?(@.clienteNome=='Maria Juro')]").isNotEmpty());

        var rolled = objectMapper.readTree(mockMvc.perform(get("/api/cobrancas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        int mariaInterest = 0;
        for (var node : rolled) {
            if (mariaContractId.equals(node.get("contratoId").asText()) && "so_juros".equals(node.get("tipoParcela").asText())) {
                mariaInterest++;
            }
        }
        assertThat(mariaInterest).isGreaterThanOrEqualTo(2);
    }

    @Test
    void professionalLendingCases() throws Exception {
        String token = login("admin@pixfactory.app", "Admin@123");
        MvcResult clientRes = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Carla Crédito",
                                "cpf", "701.222.333-00",
                                "telefone", "11970003333",
                                "email", "carla.credito@example.com",
                                "endereco", "Rua Crédito, 1",
                                "classificacao", "Médio",
                                "indicador", Map.of("nome", "Pedro Indicador", "telefone", "11970004444", "relacao", "amigo")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String clientId = objectMapper.readTree(clientRes.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/clients/" + clientId + "/dossie").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicador.nome").value("Pedro Indicador"))
                .andExpect(jsonPath("$.operacoes").isNumber());

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("clienteId", clientId);
        body.put("tipo", "Empréstimo");
        body.put("tipoOperacao", "rotativo");
        body.put("valorTotal", 1000);
        body.put("parcelasTotais", 1);
        body.put("proximoPagamento", "2026-10-15");
        body.put("status", "ativo");
        body.put("juros", 20);
        body.put("saldoDevedor", 1000);
        body.put("sistemaAmortizacao", "so_juros_aberto");
        body.put("modoPagamento", "juros_rotativo");
        body.put("periodicidade", "mensal");
        body.put("baseCalculo", "saldo");
        MvcResult created = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseCalculo").value("saldo"))
                .andExpect(jsonPath("$.regrasSnapshot.taxa").exists())
                .andReturn();
        String contractId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/contracts/" + contractId + "/actions/extra")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(1500));

        mockMvc.perform(post("/api/contracts/" + contractId + "/actions/nova_operacao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 500, "modoOperacao", "separar"))))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/contracts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var contracts = objectMapper.readTree(list.getResponse().getContentAsString());
        int forClient = 0;
        for (var node : contracts) {
            if (clientId.equals(node.get("clienteId").asText())) forClient++;
        }
        assertThat(forClient).isGreaterThanOrEqualTo(2);

        Map<String, Object> debt = new java.util.LinkedHashMap<>();
        debt.put("clienteId", clientId);
        debt.put("tipo", "Empréstimo");
        debt.put("valorTotal", 2000);
        debt.put("parcelasTotais", 1);
        debt.put("proximoPagamento", "2026-10-20");
        debt.put("status", "ativo");
        debt.put("juros", 0);
        debt.put("saldoDevedor", 2000);
        debt.put("sistemaAmortizacao", "price");
        debt.put("modoPagamento", "parcela_cheia");
        debt.put("periodicidade", "mensal");
        String debtId = objectMapper.readTree(mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/contracts/" + debtId + "/actions/bem")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 800, "descricao", "moto DEMO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDevedor").value(1200));

        var charges = objectMapper.readTree(mockMvc.perform(get("/api/cobrancas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String chargeId = null;
        for (var node : charges) {
            if (debtId.equals(node.get("contratoId").asText())) {
                chargeId = node.get("id").asText();
                break;
            }
        }
        assertThat(chargeId).isNotNull();
        mockMvc.perform(post("/api/cobrancas/" + chargeId + "/pagamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valor", 5000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("excedente"));
    }

    private com.fasterxml.jackson.databind.JsonNode findCharge(
            com.fasterxml.jackson.databind.JsonNode charges, String contractId, String tipo
    ) {
        for (var node : charges) {
            if (contractId.equals(node.get("contratoId").asText()) && tipo.equals(node.get("tipoParcela").asText())) {
                return node;
            }
        }
        return null;
    }
}
