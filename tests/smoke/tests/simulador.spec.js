const { test, expect } = require("@playwright/test");
const { loginAsDemo } = require("../helpers/auth");

test.describe("Simulador", () => {
  test("calcula parcela sem gravar contrato", async ({ page }) => {
    await loginAsDemo(page);
    await page.goto("/simulador.html");
    await page.locator("#nome-cliente").fill("Ana Smoke");
    await page.locator("#valor-emprestimo").fill("1000");
    await page.locator("#taxa-juros").fill("2.5");
    await page.locator("#num-parcelas").fill("12");
    await page.locator("#emprestimo-form button[type=submit]").click();
    await expect(page.locator("#res-nome")).toHaveText("Ana Smoke");
    await expect(page.locator("#res-parcela")).not.toHaveText("R$ 0,00");
    await expect(page.locator("#amort-body tr")).toHaveCount(12);
  });
});
