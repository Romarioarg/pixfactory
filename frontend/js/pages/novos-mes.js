(async function () {
  await PF.layout.mount("clientes");
  const month = PF.isoDate(0).slice(0, 7);
  const tbody = document.getElementById("novos-body");
  const search = document.getElementById("client-search");

  function render() {
    const q = String(search.value || "").toLowerCase();
    const rows = PF.store.clients.all().filter((c) => String(c.criadoEm || "").startsWith(month) && c.nome.toLowerCase().includes(q));
    tbody.innerHTML = rows.map((c) =>
      "<tr>" +
      "<td data-label='Foto'><img class='avatar-sm' alt='' src='" + PF.escapeHtml(c.foto || "assets/favicon.svg") + "'></td>" +
      "<td data-label='Nome'>" + PF.escapeHtml(c.nome) + "</td>" +
      "<td data-label='Telefone'>" + PF.maskPhone(c.telefone) + "</td>" +
      "<td data-label='CPF'>" + PF.escapeHtml(c.cpf || "—") + "</td>" +
      "<td data-label='Cadastro'>" + PF.formatDate(c.criadoEm) + "</td>" +
      "<td data-label='Ações'><a class='btn' href='cliente.html?id=" + encodeURIComponent(c.id) + "'>Ver</a></td>" +
      "</tr>"
    ).join("") || "<tr><td colspan='6'>" + PF.ui.empty("Nenhum cliente novo neste mês.") + "</td></tr>";
  }
  search.addEventListener("input", render);
  render();
})();
