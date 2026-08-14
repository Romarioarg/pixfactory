(function () {
  const form = document.getElementById("login-form");
  if (!form) return;

  if (PF.api.getToken()) {
    window.location.href = "dashboard.html";
    return;
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const errors = PF.validate({
      email: { value: email, checks: [PF.validators.email] },
      password: { value: password, checks: [PF.validators.required] },
    });
    PF.ui.showErrors(errors);
    if (Object.keys(errors).length) return;

    const submit = form.querySelector("[type=submit]");
    if (submit) submit.disabled = true;
    const result = await PF.auth.login(email, password);
    if (submit) submit.disabled = false;
    if (!result.ok) {
      PF.ui.showErrors({ password: result.error });
      return;
    }
    window.location.href = "dashboard.html";
  });
})();
