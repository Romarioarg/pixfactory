(async function () {
  await PF.layout.mount("clientes");
  const list = document.getElementById("clientes-lista");
  const search = document.getElementById("clientSearch");

  function render(term) {
    const q = String(term || "").toLowerCase();
    const clients = PF.store.clients.all().filter((c) => {
      const blob = [c.nome, c.endereco, c.telefone, c.email, c.classificacao, c.status].join(" ").toLowerCase();
      return blob.includes(q);
    });
    if (!clients.length) {
      list.innerHTML = PF.ui.empty("Nenhum cliente encontrado.");
      return;
    }
    list.innerHTML = clients.map((c) =>
      '<article class="card client-row" style="margin-bottom:16px">' +
      '<img class="avatar" alt="" src="' + PF.escapeHtml(c.foto || "assets/favicon.svg") + '">' +
      "<div style='flex:1'><h2>" + PF.escapeHtml(c.nome) + "</h2>" +
      "<p>Perfil: " + PF.escapeHtml(c.classificacao || "N/A") + " · " +
      '<span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span></p>" +
      "<p>" + PF.escapeHtml(c.endereco || "") + "</p>" +
      "<p>" + PF.maskPhone(c.telefone) + " · " + PF.escapeHtml(c.email || "") + "</p>" +
      '<div class="actions-row" style="margin-top:10px">' +
      '<button type="button" data-detail="' + c.id + '">Ver detalhes</button>' +
      '<button type="button" class="btn-ghost" data-wa="' + c.id + '">Cobrança</button>' +
      '<a class="btn btn-warn" href="editar-cliente.html?id=' + encodeURIComponent(c.id) + '">Editar</a>' +
      '<button type="button" class="btn-danger" data-del="' + c.id + '">Remover</button>' +
      "</div></div></article>"
    ).join("");
  }

  search.addEventListener("input", () => render(search.value));
  list.addEventListener("click", async (event) => {
    const del = event.target.closest("[data-del]");
    const wa = event.target.closest("[data-wa]");
    const detail = event.target.closest("[data-detail]");
    if (del) {
      const client = PF.store.clients.get(del.getAttribute("data-del"));
      if (!client) return;
      if (!PF.ui.confirm('Remover o cliente "' + client.nome + '" e seus contratos?')) return;
      try {
        await PF.store.clients.remove(client.id);
        PF.ui.toast("Cliente removido.");
        render(search.value);
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível remover.", "error");
      }
    }
    if (wa) {
      const client = PF.store.clients.get(wa.getAttribute("data-wa"));
      if (!client || !client.telefone) return PF.ui.toast("Cliente sem telefone.", "error");
      const msg = encodeURIComponent("Olá " + client.nome + ", lembrete de cobrança do PixFactory.");
      window.open("https://wa.me/55" + PF.digits(client.telefone) + "?text=" + msg, "_blank");
    }
    if (detail) {
      const client = PF.store.clients.get(detail.getAttribute("data-detail"));
      const contracts = PF.store.clientContracts(client.id);
      document.getElementById("modal-body").innerHTML =
        "<h2>" + PF.escapeHtml(client.nome) + "</h2>" +
        "<p>CPF " + PF.escapeHtml(client.cpf) + "</p>" +
        "<h3>Contratos</h3>" +
        (contracts.length ? "<ul>" + contracts.map((ct) => "<li>" + PF.escapeHtml(ct.tipo) + " · " + PF.formatMoney(ct.valorTotal) + " · " + PF.statusLabel(ct.status) + "</li>").join("") + "</ul>" : PF.ui.empty("Nenhum contrato.")) +
        "<h3>Histórico</h3>" +
        (client.historico && client.historico.length ? "<ul>" + client.historico.map((h) => "<li>" + PF.formatDate(h.data) + " — " + PF.escapeHtml(h.info || h.tipo) + "</li>").join("") + "</ul>" : PF.ui.empty("Sem histórico."));
      PF.ui.openModal("cliente-modal");
    }
  });

  render("");
})();
