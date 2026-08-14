(async function () {
  const user = await PF.layout.mount("planos");
  document.querySelectorAll("[data-plan]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const plan = btn.getAttribute("data-plan");
      await PF.api.put("/api/auth/plan", { plan: plan });
      PF.ui.toast("Plano atualizado para " + plan + " (simulação DEMO, sem gateway real).");
      window.location.reload();
    });
  });
})();
