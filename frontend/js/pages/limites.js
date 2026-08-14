(async function () {
  const user = await PF.layout.mount("gastos");
  document.getElementById("fin-nav").innerHTML = PF.layout.financeNav("limites");
  document.getElementById("banner").innerHTML = PF.layout.trialBanner(user);
  const list = document.getElementById("budget-list");

  function render() {
    const items = PF.store.budgets.all();
    list.innerHTML = items.map((b) => {
      const pct = Math.min(100, Math.round((Number(b.gasto) / Math.max(1, Number(b.limite))) * 100));
      return '<div class="card" style="margin-bottom:12px"><h3>' + PF.escapeHtml(b.categoria) + "</h3><p>" +
        PF.formatMoney(b.gasto) + " de " + PF.formatMoney(b.limite) + " (" + pct + "%)</p>" +
        '<div class="progress"><span style="width:' + pct + '%;background:' + (pct > 90 ? "#e74c3c" : "#2ecc71") + '"></span></div>' +
        '<button class="btn-danger" style="margin-top:10px" data-del="' + b.id + '">Remover</button></div>';
    }).join("") || PF.ui.empty("Nenhum limite cadastrado.");
  }

  document.getElementById("limit-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await PF.store.budgets.add({
      categoria: document.getElementById("limit-cat").value.trim(),
      limite: Number(document.getElementById("limit-val").value),
      gasto: Number(document.getElementById("limit-gasto").value || 0),
    });
    e.target.reset();
    PF.ui.closeModal("limit-modal");
    render();
  });
  list.addEventListener("click", async (e) => {
    const del = e.target.closest("[data-del]");
    if (!del) return;
    await PF.store.budgets.remove(del.getAttribute("data-del"));
    render();
  });
  document.getElementById("open-limit").addEventListener("click", () => PF.ui.openModal("limit-modal"));
  render();
})();
