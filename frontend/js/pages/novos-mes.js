(async function () {
  await PF.layout.mount("clientes");
  const month = PF.isoDate(0).slice(0, 7);
  const tbody = document.getElementById("novos-body");
  const search = document.getElementById("client-search");

  function render() {
    const q = String(search.value || "").toLowerCase();
    const rows = PF.store.clients.all().filter((c) => String(c.criadoEm || "").startsWith(month) && c.nome.toLowerCase().includes(q));
    tbody.innerHTML = rows.map((c) =>
      "<tr><td data-label='Nome'>" + PF.escapeHtml(c.nome) + "</td><td data-label='Cadastro'>" + PF.formatDate(c.criadoEm) +
      "</td><td data-label='Status'><span class='badge " + PF.statusClass(c.status) + "'>" + PF.statusLabel(c.status) +
      "</span></td><td data-label='Ações'><a class='btn' href='novo_contrato.html?id=" + c.id + "'>Novo contrato</a></td></tr>"
    ).join("") || "<tr><td colspan='4'>" + PF.ui.empty("Nenhum cliente novo neste mês.") + "</td></tr>";
  }
  search.addEventListener("input", render);
  render();
})();
