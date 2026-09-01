const { test, expect } = require("@playwright/test");
const { loginAsDemo } = require("../helpers/auth");

test.describe("Dashboard", () => {
  test("exibe métricas e atalhos da operação", async ({ page }) => {
    await loginAsDemo(page);
    await expect(page.locator("#dashboard-root")).toContainText("Clientes ativos");
    await expect(page.locator("#dashboard-root")).toContainText("Contratos vigentes");
    await expect(page.locator("#toggle-values")).toBeVisible();
    await expect(page.locator(".sidebar a[href='clientes.html']")).toBeVisible();
  });
});
