const { test, expect } = require("@playwright/test");
const { LoginPage, DashboardPage } = require("../pages");

test.describe("Layout e responsividade", () => {
  test("sidebar e hambúrguer no viewport estreito", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const login = new LoginPage(page);
    const dashboard = new DashboardPage(page);
    await login.login("demo@pixfactory.app", "Demo@123");
    await dashboard.expectLoaded();
    await expect(dashboard.hamburger).toBeVisible();
    await dashboard.hamburger.click();
    await expect(dashboard.sidebar).toHaveClass(/open/);
    await dashboard.sidebar.locator("a[href='planos.html']").click();
    await expect(page).toHaveURL(/planos\.html/);
  });

  test("ocultar valores no dashboard", async ({ page }) => {
    const login = new LoginPage(page);
    const dashboard = new DashboardPage(page);
    await login.login("demo@pixfactory.app", "Demo@123");
    await dashboard.expectLoaded();
    await dashboard.toggleValues.click();
    await page.waitForLoadState("networkidle");
  });
});
