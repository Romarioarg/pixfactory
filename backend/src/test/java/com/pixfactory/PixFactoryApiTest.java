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
}
