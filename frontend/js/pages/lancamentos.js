(async function () {
  const user = await PF.layout.mount("gastos");
  document.getElementById("fin-nav").innerHTML = PF.layout.financeNav("lancamentos");
  document.getElementById("banner").innerHTML = PF.layout.trialBanner(user);
  const list = document.getElementById("launch-list");

  function render() {
    const items = PF.store.launches.all().slice().sort((a, b) => String(b.data).localeCompare(a.data));
    if (!items.length) {
      list.innerHTML = PF.ui.empty("Nenhum lançamento. Adicione uma despesa ou receita.");
      return;
    }
    list.innerHTML = items.map((i) =>
      '<article class="card flex" style="justify-content:space-between;margin-bottom:10px"><div><strong>' +
      PF.escapeHtml(i.descricao) + "</strong><p>" + PF.formatDate(i.data) + " · " + PF.escapeHtml(i.conta) + " · " + i.tipo +
      "</p></div><div>" + PF.formatMoney(i.valor) + ' <button class="btn-danger" data-del="' + i.id + '">Excluir</button></div></article>'
    ).join("");
  }

  document.getElementById("launch-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      tipo: document.getElementById("tipo").value,
      descricao: document.getElementById("descricao").value.trim(),
      valor: Number(document.getElementById("valor").value),
      data: document.getElementById("data").value,
      conta: document.getElementById("conta").value.trim() || "Caixa",
    };
    if (!payload.descricao || !(payload.valor > 0) || !payload.data) {
      PF.ui.toast("Preencha descrição, valor e data.", "error");
      return;
    }
    await PF.store.launches.add(payload);
    e.target.reset();
    PF.ui.closeModal("launch-modal");
    render();
    PF.ui.toast("Lançamento salvo.");
  });
  list.addEventListener("click", async (e) => {
    const del = e.target.closest("[data-del]");
    if (!del) return;
    if (!PF.ui.confirm("Excluir este lançamento?")) return;
    await PF.store.launches.remove(del.getAttribute("data-del"));
    render();
  });
  document.getElementById("open-launch").addEventListener("click", () => PF.ui.openModal("launch-modal"));
  render();
})();
