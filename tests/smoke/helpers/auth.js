async function clearSession(page) {
  await page.addInitScript(() => {
    try {
      localStorage.removeItem("pixfactory.token");
    } catch (err) {
      /* ignore */
    }
  });
}

async function loginAsDemo(page, email = "demo@pixfactory.app", password = "Demo@123") {
  await clearSession(page);
  await page.goto("/");
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(password);
  await page.locator("#login-form button[type=submit]").click();
  await page.waitForURL(/dashboard\.html/);
  await page.locator("#dashboard-root h1").waitFor();
}

function uniqueCpf() {
  const n = String(Date.now() % 1_000_000_000).padStart(9, "3");
  const digits = n.split("").map(Number);
  const check = (len) => {
    let sum = 0;
    for (let i = 0; i < len; i += 1) sum += digits[i] * (len + 1 - i);
    const rest = (sum * 10) % 11;
    return rest === 10 ? 0 : rest;
  };
  digits.push(check(9));
  digits.push(check(10));
  return digits.join("");
}

module.exports = { clearSession, loginAsDemo, uniqueCpf };
