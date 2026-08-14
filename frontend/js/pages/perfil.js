(async function () {
  const user = await PF.layout.mount("perfil");
  document.getElementById("nome").value = user.name || "";
  document.getElementById("email").value = user.email || "";
  document.getElementById("telefone").value = user.phone || "";
  document.getElementById("endereco").value = user.address || "";
  document.getElementById("role").textContent = user.role;
  document.getElementById("plan").textContent = user.plan;
  document.getElementById("admin-box").hidden = user.role !== "ADMIN";

  document.getElementById("profile-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = document.getElementById("nome").value.trim();
    const email = document.getElementById("email").value.trim();
    const phone = document.getElementById("telefone").value.trim();
    if (PF.validators.email(email)) return PF.ui.toast(PF.validators.email(email), "error");
    try {
      const updated = await PF.api.put("/api/auth/me", {
        name,
        email,
        phone,
        address: document.getElementById("endereco").value.trim(),
      });
      PF.store.setCurrentUser(updated);
      PF.ui.toast("Perfil atualizado.");
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível atualizar.", "error");
    }
  });
})();
