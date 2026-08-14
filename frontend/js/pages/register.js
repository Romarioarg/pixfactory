(function () {
  const form = document.getElementById("register-form");
  if (!form) return;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value;
    const phone = document.getElementById("phone").value;
    const password = document.getElementById("password").value;
    const confirm = document.getElementById("confirm-password").value;
    const errors = PF.validate({
      name: { value: name, checks: [PF.validators.required] },
      email: { value: email, checks: [PF.validators.email] },
      password: { value: password, checks: [PF.validators.password] },
    });
    if (password !== confirm) errors.confirm = "As senhas não coincidem.";
    if (phone && PF.validators.phone(phone)) errors.phone = PF.validators.phone(phone);
    PF.ui.showErrors(errors);
    if (Object.keys(errors).length) return;

    const result = await PF.auth.register({ name, email, phone, password });
    if (!result.ok) {
      PF.ui.showErrors({ email: result.error });
      return;
    }
    window.location.href = "dashboard.html";
  });

  document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.getAttribute("data-toggle-password"));
      input.type = input.type === "password" ? "text" : "password";
    });
  });
})();
