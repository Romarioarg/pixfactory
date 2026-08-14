(function () {
  const form = document.getElementById("recover-form");
  if (!form) return;
  let step = 1;
  let email = "";
  let demoCode = "";

  function showStep(n) {
    step = n;
    document.querySelectorAll("[data-step]").forEach((el) => {
      el.hidden = Number(el.getAttribute("data-step")) !== n;
    });
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (step === 1) {
      email = document.getElementById("email").value.trim();
      const err = PF.validators.email(email);
      PF.ui.showErrors({ email: err });
      if (err) return;
      const result = await PF.auth.forgotPassword(email);
      if (!result.ok) {
        PF.ui.showErrors({ email: result.error });
        return;
      }
      demoCode = result.data && result.data.demoCode ? result.data.demoCode : "";
      document.getElementById("code-hint").textContent =
        "EMAIL DEMO: o código foi registrado no backend. Código de demonstração: " + demoCode;
      showStep(2);
      return;
    }
    if (step === 2) {
      const code = document.getElementById("code").value.trim();
      if (!code) {
        PF.ui.showErrors({ code: "Informe o código." });
        return;
      }
      PF.ui.showErrors({ code: "" });
      showStep(3);
      return;
    }
    const password = document.getElementById("new-password").value;
    const confirm = document.getElementById("confirm-new-password").value;
    const code = document.getElementById("code").value.trim();
    const errors = {};
    const pwdErr = PF.validators.password(password);
    if (pwdErr) errors.password = pwdErr;
    if (password !== confirm) errors.confirm = "As senhas não coincidem.";
    PF.ui.showErrors(errors);
    if (Object.keys(errors).length) return;
    const result = await PF.auth.resetPassword(email, code, password);
    if (!result.ok) {
      PF.ui.toast(result.error, "error");
      return;
    }
    PF.ui.toast("Senha atualizada. Faça login.");
    window.location.href = "index.html";
  });
})();
