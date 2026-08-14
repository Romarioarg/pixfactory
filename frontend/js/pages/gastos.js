(async function () {
  const user = await PF.layout.mount("gastos");
  const root = document.getElementById("gastos-root");

  function render() {
    const pay = PF.store.payables.all();
    const rec = PF.store.receivables.all();
    const acc = PF.store.accounts.all();
    const saldo = acc.reduce((s, a) => s + Number(a.saldo || 0), 0);
    const hide = PF.store.getSettings().hideValues;
    const money = (v) => '<span class="hidden-value' + (hide ? " hidden" : "") + '">' + PF.formatMoney(v) + "</span>";
    root.innerHTML = PF.layout.trialBanner(user) + PF.layout.financeNav("visao") +
      '<header class="page-header"><h1>Controle de gastos</h1><button type="button" id="hide">' + (hide ? "Mostrar" : "Ocultar") + " valores</button></header>" +
      '<section class="grid grid-3" style="margin-bottom:16px">' +
      '<div class="card"><h3>Saldo em contas</h3><p>' + money(saldo) + '</p><button type="button" id="add-acc">Gerenciar contas</button></div>' +
      '<div class="card"><h3>A pagar</h3><p>' + money(pay.filter((p) => p.status === "pendente").reduce((s, p) => s + Number(p.valor), 0)) + '</p><button type="button" data-open="pagar-modal">Nova conta</button></div>' +
      '<div class="card"><h3>A receber</h3><p>' + money(rec.filter((p) => p.status === "pendente").reduce((s, p) => s + Number(p.valor), 0)) + '</p><button type="button" data-open="receber-modal">Novo recebível</button></div>' +
      "</section>" +
      '<section class="grid grid-2"><div class="card"><h2>Contas a pagar</h2>' +
      (pay.length ? "<ul>" + pay.map((p) => "<li class='flex' style='justify-content:space-between'><span>" + PF.escapeHtml(p.descricao) + " · " + PF.formatDate(p.vencimento) + "</span><span>" + money(p.valor) + ' <button type="button" data-pay="' + p.id + '">' + (p.status === "pendente" ? "Pagar" : "Pago") + "</button></span></li>").join("") + "</ul>" : PF.ui.empty("Nada a pagar.")) +
      '</div><div class="card"><h2>Contas a receber</h2>' +
      (rec.length ? "<ul>" + rec.map((p) => "<li class='flex' style='justify-content:space-between'><span>" + PF.escapeHtml(p.descricao) + " · " + PF.formatDate(p.vencimento) + "</span><span>" + money(p.valor) + ' <button type="button" data-rec="' + p.id + '">' + (p.status === "pendente" ? "Receber" : "Recebido") + "</button></span></li>").join("") + "</ul>" : PF.ui.empty("Nada a receber.")) +
      "</div></section>";
  }

  document.body.addEventListener("click", async (e) => {
    if (e.target.id === "hide") {
      await PF.store.saveSettings({ hideValues: !PF.store.getSettings().hideValues });
      render();
    }
    if (e.target.id === "add-acc") PF.ui.openModal("conta-modal");
    const open = e.target.closest("[data-open]");
    if (open) PF.ui.openModal(open.getAttribute("data-open"));
    const pay = e.target.closest("[data-pay]");
    if (pay) {
      await PF.store.payables.update(pay.getAttribute("data-pay"), { status: "pago" });
      render();
    }
    const rec = e.target.closest("[data-rec]");
    if (rec) {
      await PF.store.receivables.update(rec.getAttribute("data-rec"), { status: "recebido" });
      render();
    }
  });

  document.getElementById("pagar-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await PF.store.payables.add({
      descricao: document.getElementById("pagar-desc").value.trim(),
      valor: Number(document.getElementById("pagar-valor").value),
      vencimento: document.getElementById("pagar-data").value,
      status: "pendente",
    });
    PF.ui.closeModal("pagar-modal");
    e.target.reset();
    render();
    PF.ui.toast("Conta a pagar salva.");
  });
  document.getElementById("receber-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await PF.store.receivables.add({
      descricao: document.getElementById("rec-desc").value.trim(),
      valor: Number(document.getElementById("rec-valor").value),
      vencimento: document.getElementById("rec-data").value,
      status: "pendente",
    });
    PF.ui.closeModal("receber-modal");
    e.target.reset();
    render();
    PF.ui.toast("Recebível salvo.");
  });
  document.getElementById("conta-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    await PF.store.accounts.add({
      tipo: "conta",
      nome: document.getElementById("acc-nome").value.trim(),
      saldo: Number(document.getElementById("acc-saldo").value || 0),
    });
    PF.ui.closeModal("conta-modal");
    e.target.reset();
    render();
    PF.ui.toast("Conta adicionada.");
  });

  render();
})();
