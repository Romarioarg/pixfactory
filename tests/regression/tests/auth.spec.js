const { test, expect } = require("@playwright/test");
const { LoginPage, DashboardPage } = require("../pages");

test.describe("Autenticação — bordas", () => {
  test("e-mail vazio não autentica", async ({ page }) => {
    const login = new LoginPage(page);
    await login.open();
    await login.password.fill("Demo@123");
    await login.submit.click();
    await expect(page).not.toHaveURL(/dashboard\.html/);
    await expect(page.locator("[data-error-for=email]")).not.toHaveText("");
  });

  test("token ausente bloqueia clientes", async ({ page }) => {
    const login = new LoginPage(page);
    await login.open();
    await page.goto("/clientes.html");
    await expect(page).toHaveURL(/index\.html|\/$/);
  });

  test("admin autentica e vê saudação", async ({ page }) => {
    const login = new LoginPage(page);
    const dashboard = new DashboardPage(page);
    await login.login("admin@pixfactory.app", "Admin@123");
    await dashboard.expectLoaded();
    await expect(dashboard.title).toContainText("Olá");
  });

  test("logout limpa o acesso às páginas internas", async ({ page }) => {
    const login = new LoginPage(page);
    const dashboard = new DashboardPage(page);
    await login.login("demo@pixfactory.app", "Demo@123");
    await dashboard.expectLoaded();
    await dashboard.logout.click();
    await expect(page).toHaveURL(/index\.html|\/$/);
    await page.goto("/contratos.html");
    await expect(page).toHaveURL(/index\.html|\/$/);
  });
});
