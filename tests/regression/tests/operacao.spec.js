const { test, expect } = require("@playwright/test");
const { LoginPage, DashboardPage, ContratosPage, NovoContratoPage, SimuladorPage, AgendaPage } = require("../pages");

test.describe("Operação — contratos, simulador e agenda", () => {
  test.beforeEach(async ({ page }) => {
    const login = new LoginPage(page);
    await login.login("demo@pixfactory.app", "Demo@123");
    await new DashboardPage(page).expectLoaded();
  });

  test("filtros da lista de contratos", async ({ page }) => {
    const contratos = new ContratosPage(page);
    await contratos.open();
    await expect(contratos.body).toBeVisible();
    await contratos.filter("ativo");
    await contratos.filter("atrasado");
    await contratos.filter("todos");
    await expect(contratos.filters.locator("[data-filter=todos]")).toHaveClass(/active/);
  });

  test("formulário de novo contrato carrega clientes", async ({ page }) => {
    const form = new NovoContratoPage(page);
    await form.open();
    await expect(page.locator("h1")).toContainText("Novo contrato");
    await expect(form.cliente).toBeVisible();
    const options = await form.cliente.locator("option").count();
    expect(options).toBeGreaterThan(0);
    await expect(form.preview).toBeVisible();
  });

  test("simulador de juros na segunda aba", async ({ page }) => {
    const sim = new SimuladorPage(page);
    await sim.open();
    await sim.tabJuros.click();
    await page.locator("#juros-nome").fill("Carlos Juros");
    await page.locator("#juros-saldo").fill("1000");
    await page.locator("#juros-taxa").fill("20");
    await page.locator("#juros-form button[type=submit]").click();
    await expect(sim.resultadoNome).toHaveText("Carlos Juros");
    await expect(sim.resultadoParcela).not.toHaveText("R$ 0,00");
  });

  test("agenda responde aos filtros de período", async ({ page }) => {
    const agenda = new AgendaPage(page);
    await agenda.open();
    await expect(agenda.grid).toBeVisible();
    await agenda.filters.locator("[data-range=hoje]").click();
    await agenda.filters.locator("[data-range=atrasados]").click();
    await agenda.filters.locator("[data-range=mes]").click();
    await expect(page.locator("#month-year")).not.toHaveText("");
  });
});
