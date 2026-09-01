const { test, expect } = require("@playwright/test");
const { clearSession, loginAsDemo } = require("../helpers/auth");

test.describe("Login", () => {
  test("aceita operador DEMO e abre o dashboard", async ({ page }) => {
    await loginAsDemo(page);
    await expect(page).toHaveURL(/dashboard\.html/);
    await expect(page.locator("#dashboard-root h1")).toContainText("Olá");
    await expect(page.locator(".sidebar")).toContainText("PixFactory");
  });

  test("rejeita senha incorreta", async ({ page }) => {
    await clearSession(page);
    await page.goto("/");
    await page.locator("#email").fill("demo@pixfactory.app");
    await page.locator("#password").fill("senha-invalida");
    await page.locator("#login-form button[type=submit]").click();
    await expect(page).toHaveURL(/index\.html|\/$/);
    await expect(page.locator("[data-error-for=password]")).not.toHaveText("");
  });
});
