(async function () {
  const user = await PF.layout.mount("gastos");
  document.getElementById("fin-nav").innerHTML = PF.layout.financeNav("bancos");
  document.getElementById("banner").innerHTML = PF.layout.trialBanner(user);
  const list = document.getElementById("bank-list");
  const institutions = await PF.api.get("/api/open-finance/institutions");

  async function render() {
    const banks = PF.store.banks.all();
    let snapshotHtml = "";
    try {
      const snap = await PF.api.get("/api/open-finance/snapshot/DEMOBANK");
      snapshotHtml = '<div class="card" style="margin-bottom:16px"><h3>Open Finance DEMO</h3>' +
        "<p>" + PF.escapeHtml(snap.institution) + " · Conta " + PF.escapeHtml(snap.account.number) + "</p>" +
        "<p>Saldo fictício: " + PF.formatMoney(snap.account.balance) + "</p>" +
        "<p class='hint'>Simulação substituível por OpenFinanceRealService.</p></div>";
    } catch (err) {
      snapshotHtml = "";
    }
    list.innerHTML = snapshotHtml + banks.map((b) =>
      '<article class="card flex" style="justify-content:space-between;margin-bottom:10px"><div><strong>' + PF.escapeHtml(b.nome) +
      "</strong><p>Agência " + PF.escapeHtml(b.agencia || "—") + " · Conta " + PF.escapeHtml(b.conta || "—") +
      (b.saldo != null ? " · Saldo " + PF.formatMoney(b.saldo) : "") + "</p></div>" +
      '<button class="btn-danger" data-del="' + b.id + '">Desconectar</button></article>'
    ).join("") + (banks.length ? "" : PF.ui.empty("Nenhum banco conectado. Esta é uma simulação de Open Finance."));
    const select = document.getElementById("bank-nome");
    if (select && select.tagName === "SELECT") {
      select.innerHTML = institutions.map((i) => '<option value="' + PF.escapeHtml(i.name) + '" data-code="' + i.code + '">' + PF.escapeHtml(i.name) + "</option>").join("");
    }
  }

  document.getElementById("bank-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const nomeInput = document.getElementById("bank-nome");
    const option = nomeInput.selectedOptions ? nomeInput.selectedOptions[0] : null;
    await PF.store.banks.add({
      nome: nomeInput.value.trim(),
      agencia: document.getElementById("bank-ag").value.trim(),
      conta: document.getElementById("bank-cc").value.trim(),
      institutionCode: option ? option.getAttribute("data-code") : "DEMOBANK",
      connected: true,
    });
    e.target.reset();
    PF.ui.closeModal("bank-modal");
    await render();
    PF.ui.toast("Banco conectado (Open Finance DEMO).");
  });
  list.addEventListener("click", async (e) => {
    const del = e.target.closest("[data-del]");
    if (!del) return;
    if (!PF.ui.confirm("Desconectar este banco?")) return;
    await PF.store.banks.remove(del.getAttribute("data-del"));
    await render();
  });
  document.getElementById("open-bank").addEventListener("click", () => PF.ui.openModal("bank-modal"));
  await render();
})();
