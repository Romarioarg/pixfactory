const { test, expect } = require("@playwright/test");
const { loginAsDemo } = require("../helpers/auth");

test.describe("Navegação crítica", () => {
  test("abre contratos, agenda e simulador", async ({ page }) => {
    await loginAsDemo(page);
    await page.locator(".sidebar a[href='contratos.html']").click();
    await expect(page.locator("h1")).toContainText("Contratos");
    await expect(page.locator("#contracts-body")).toBeVisible();

    await page.locator(".sidebar a[href='agenda.html']").click();
    await expect(page.locator("h1")).toContainText("Agenda");
    await expect(page.locator("#calendar-grid")).toBeVisible();

    await page.locator(".sidebar a[href='simulador.html']").click();
    await expect(page.locator("h1")).toContainText("Simulador");
    await expect(page.locator("#emprestimo-form")).toBeVisible();
  });

  test("sair encerra a sessão", async ({ page }) => {
    await loginAsDemo(page);
    await page.locator("#pf-logout").click();
    await page.waitForURL(/index\.html|\/$/);
    await page.goto("/clientes.html");
    await page.waitForURL(/index\.html|\/$/);
  });
});
