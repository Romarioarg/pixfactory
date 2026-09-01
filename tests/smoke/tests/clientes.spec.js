const { test, expect } = require("@playwright/test");
const { loginAsDemo, uniqueCpf } = require("../helpers/auth");

test.describe("Clientes", () => {
  test("lista clientes existentes", async ({ page }) => {
    await loginAsDemo(page);
    await page.locator(".sidebar a[href='clientes.html']").click();
    await expect(page).toHaveURL(/clientes\.html/);
    await expect(page.locator("h1")).toContainText("Clientes");
    await expect(page.locator("#clientes-lista")).toContainText(/João|Maria|Ana/);
  });

  test("cadastra um cliente novo", async ({ page }) => {
    await loginAsDemo(page);
    await page.goto("/cadastro.html");
    const stamp = Date.now();
    await page.locator("#nome").fill("Cliente Smoke " + stamp);
    await page.locator("#cpf").fill(uniqueCpf());
    await page.locator("#telefone").fill("11988887777");
    await page.locator("#email").fill("smoke." + stamp + "@example.com");
    await page.locator("#endereco").fill("Rua de Teste, 100");
    await page.locator("#classificacao").selectOption("Médio");
    await page.locator("#cadastro-form button[type=submit]").click();
    await page.waitForURL(/clientes\.html/);
    await expect(page.locator("#clientes-lista")).toContainText("Cliente Smoke " + stamp);
  });
});
