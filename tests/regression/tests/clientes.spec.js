const { test, expect } = require("@playwright/test");
const { LoginPage, DashboardPage, CadastroPage, ClientesPage } = require("../pages");

test.describe("Clientes — variações", () => {
  test.beforeEach(async ({ page }) => {
    const login = new LoginPage(page);
    await login.login("demo@pixfactory.app", "Demo@123");
    await new DashboardPage(page).expectLoaded();
  });

  test("CPF inválido impede o save", async ({ page }) => {
    const cadastro = new CadastroPage(page);
    await cadastro.open();
    await cadastro.fillValid({ cpf: "111.111.111-11" });
    await cadastro.submit();
    await expect(page).toHaveURL(/cadastro\.html/);
    await expect(cadastro.cpfError).toContainText("CPF");
  });

  test("campos obrigatórios vazios", async ({ page }) => {
    const cadastro = new CadastroPage(page);
    await cadastro.open();
    await cadastro.submit();
    await expect(page).toHaveURL(/cadastro\.html/);
    await expect(page.locator("[data-error-for=nome]")).not.toHaveText("");
  });

  test("busca na lista de clientes", async ({ page }) => {
    const clientes = new ClientesPage(page);
    await clientes.open();
    await expect(clientes.list).toBeVisible();
    await clientes.search.fill("xyz-nao-existe-qa");
    await page.waitForTimeout(300);
  });

  test("fluxo feliz de cadastro aparece na lista", async ({ page }) => {
    const cadastro = new CadastroPage(page);
    await cadastro.open();
    const stamp = await cadastro.fillValid();
    await cadastro.submit();
    await page.waitForURL(/clientes\.html/);
    const clientes = new ClientesPage(page);
    await expect(clientes.list).toContainText("Cliente Regressao " + stamp);
  });
});
