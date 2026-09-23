(async function () {
  await PF.layout.mount("clientes");
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");
  const root = document.getElementById("cliente-root");
  if (!id) {
    root.innerHTML = PF.ui.empty("Cliente não informado.") + '<p><a class="btn" href="clientes.html">Voltar</a></p>';
    return;
  }

  let data;
  try {
    data = await PF.api.get("/api/clients/" + encodeURIComponent(id) + "/dossie");
  } catch (err) {
    root.innerHTML = PF.ui.empty(err.message || "Não foi possível carregar o cliente.") + '<p><a class="btn" href="clientes.html">Voltar</a></p>';
    return;
  }

  const contracts = data.contratos || [];
  const charges = data.cobrancas || [];
  root.innerHTML =
    '<header class="page-header"><h1>' + PF.escapeHtml(data.nome) + "</h1>" +
    '<div class="actions-row"><a class="btn-ghost" href="clientes.html">Voltar</a>' +
    '<a class="btn" href="editar-cliente.html?id=' + encodeURIComponent(data.id) + '">Editar</a>' +
    '<a class="btn" href="novo_contrato.html?id=' + encodeURIComponent(data.id) + '">Novo contrato</a></div></header>' +
    '<section class="card client-row" style="margin-bottom:16px">' +
    '<img class="avatar" alt="" src="' + PF.escapeHtml(data.foto || "assets/favicon.svg") + '">' +
    "<div><p><span class='badge " + PF.statusClass(data.status) + "'>" + PF.statusLabel(data.status) + "</span> " +
    '<span class="badge badge-muted">' + PF.escapeHtml(data.classificacao || "N/A") + "</span></p>" +
    "<p>" + PF.escapeHtml(data.score || "") + "</p>" +
    "<p>CPF " + PF.escapeHtml(data.cpf || "—") + " · " + PF.maskPhone(data.telefone) + "</p>" +
    "<p>" + PF.escapeHtml(data.email || "") + "</p>" +
    "<p>" + PF.escapeHtml(data.endereco || "") + "</p>" +
    (data.observacoes ? "<p>" + PF.escapeHtml(data.observacoes) + "</p>" : "") +
    "</div></section>" +
    '<section class="grid grid-3" style="margin-bottom:16px">' +
    '<div class="card"><h3>Total emprestado</h3><p class="stat">' + PF.formatMoney(data.totalEmprestado) + "</p></div>" +
    '<div class="card"><h3>Total recebido</h3><p class="stat">' + PF.formatMoney(data.totalRecebido) + "</p></div>" +
    '<div class="card"><h3>Em aberto</h3><p class="stat">' + PF.formatMoney(data.totalAberto) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:16px">' +
    '<div class="card"><h3>Atrasado</h3><p class="stat">' + PF.formatMoney(data.totalAtrasado) + "</p></div>" +
    '<div class="card"><h3>Multa + mora</h3><p class="stat">' + PF.formatMoney(Number(data.totalMulta || 0) + Number(data.totalMora || 0)) + "</p></div>" +
    '<div class="card"><h3>Operações</h3><p class="stat">' + (data.operacoes || contracts.length) + "</p><p class='hint'>" + (data.parcelasPagas || 0) + " pagas · " + (data.parcelasPendentes || 0) + " pendentes</p></div>" +
    "</section>" +
    '<section class="card" style="margin-bottom:16px"><h2>Indicador</h2>' +
    (data.indicador && data.indicador.nome
      ? "<p>" + PF.escapeHtml(data.indicador.nome) + " · " + PF.maskPhone(data.indicador.telefone) + " · " + PF.escapeHtml(data.indicador.relacao || "") + "</p><p class='hint'>Indicador não é responsável pela dívida.</p>"
      : PF.ui.empty("Sem indicador.")) +
    "</section>" +
    '<section class="card" style="margin-bottom:16px"><h2>Referências</h2>' +
    ((data.referencias || []).length
      ? "<ul>" + data.referencias.map((r) => "<li>" + PF.escapeHtml(r.nome || "") + " · " + PF.escapeHtml(r.telefone || "") + " · " + PF.escapeHtml(r.relacao || "") + "</li>").join("") + "</ul>"
      : PF.ui.empty("Sem referências.")) +
    "</section>" +
    '<section class="card" style="margin-bottom:16px"><h2>Garantias</h2>' +
    ((data.garantias || []).length
      ? "<ul>" + data.garantias.map((g) => "<li>" + PF.escapeHtml(g.tipo || "") + " · " + PF.escapeHtml(g.descricao || "") + " · " + PF.escapeHtml(g.situacao || "") + "</li>").join("") + "</ul>"
      : PF.ui.empty("Sem garantia cadastrada.")) +
    "</section>" +
    '<section class="card" style="margin-bottom:16px"><h2>Contratos</h2>' +
    (contracts.length ? "<ul>" + contracts.map((c) => "<li>" + PF.escapeHtml(c.tipo) + " · " + PF.formatMoney(c.valorTotal) +
      " · saldo " + PF.formatMoney(c.saldoDevedor) + ' · <span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span></li>").join("") + "</ul>" : PF.ui.empty("Nenhum contrato.")) +
    "</section>" +
    '<section class="card" style="margin-bottom:16px"><h2>Cobranças</h2>' +
    (charges.length ? "<ul>" + charges.map((c) => "<li>Parcela " + c.numero + " · " + PF.formatMoney(c.valor) +
      " · " + PF.formatDate(c.vencimento) + ' · <span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span>" +
      (Number(c.multaAplicada) || Number(c.moraAplicada) ? " · multa/mora " + PF.formatMoney(Number(c.multaAplicada || 0) + Number(c.moraAplicada || 0)) : "") +
      (c.promessaData ? " · Promessa " + PF.formatDate(c.promessaData) : "") +
      ' · <a href="recibo.html?id=' + encodeURIComponent(c.id) + '">Recibo</a></li>').join("") + "</ul>" : PF.ui.empty("Nenhuma cobrança.")) +
    "</section>" +
    '<section class="card"><h2>Histórico</h2>' +
    (data.historico && data.historico.length ? "<ul>" + data.historico.map((h) => "<li>" + PF.formatDate(h.data) + " — " + PF.escapeHtml(h.info || h.tipo || "") + "</li>").join("") + "</ul>" : PF.ui.empty("Sem histórico.")) +
    "</section>";
})();
