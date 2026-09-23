(async function () {
  const user = await PF.layout.mount("planos");
  const current = (user && user.plan) || "gratuito";
  const label = { gratuito: "Gratuito", pro: "Pro", premium: "Premium" }[current] || current;
  document.getElementById("plan-current").textContent = "Plano atual: " + label + " · valores ilustrativos, sem cobrança real.";
  document.querySelectorAll("[data-plan-card]").forEach((card) => {
    card.classList.toggle("current", card.getAttribute("data-plan-card") === current);
  });
  document.querySelectorAll("[data-plan]").forEach((btn) => {
    if (btn.getAttribute("data-plan") === current) {
      btn.textContent = "Plano ativo";
      btn.disabled = true;
    }
    btn.addEventListener("click", async () => {
      const plan = btn.getAttribute("data-plan");
      await PF.api.put("/api/auth/plan", { plan: plan });
      PF.ui.toast("Plano atualizado para " + plan + " (simulação DEMO, sem gateway real).");
      window.location.reload();
    });
  });
})();
