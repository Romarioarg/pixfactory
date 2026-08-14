(async function () {
  const user = await PF.layout.mount("configuracoes");
  const s = PF.store.getSettings();
  document.getElementById("companyName").value = s.companyName || "";
  document.getElementById("slogan").value = s.slogan || "";
  document.getElementById("defaultInterestRate").value = s.defaultInterestRate || 0;
  document.getElementById("emailNotifications").checked = !!s.emailNotifications;
  document.getElementById("paymentReminders").checked = !!s.paymentReminders;

  document.getElementById("settings-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await PF.store.saveSettings({
      companyName: document.getElementById("companyName").value.trim(),
      slogan: document.getElementById("slogan").value.trim(),
      defaultInterestRate: Number(document.getElementById("defaultInterestRate").value || 0),
      emailNotifications: document.getElementById("emailNotifications").checked,
      paymentReminders: document.getElementById("paymentReminders").checked,
    });
    PF.ui.toast("Configurações salvas.");
  });

  document.getElementById("password-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const current = document.getElementById("current-password").value;
    const next = document.getElementById("next-password").value;
    const confirm = document.getElementById("next-password-2").value;
    const err = PF.validators.password(next);
    if (err) return PF.ui.toast(err, "error");
    if (next !== confirm) return PF.ui.toast("As senhas não coincidem.", "error");
    const result = await PF.auth.changePassword(user.id, current, next);
    PF.ui.toast(result.ok ? "Senha alterada." : result.error, result.ok ? "" : "error");
    if (result.ok) e.target.reset();
  });

  document.getElementById("reset-demo").addEventListener("click", () => {
    PF.ui.toast("Para restaurar o seed DEMO, recrie o banco (docker compose down -v && docker compose up).");
  });
})();
