(async function () {
  const user = await PF.layout.mount("dashboard");
  if (!user) return;

  const metrics = PF.store.metrics();
  const hide = PF.store.getSettings().hideValues;
  const money = (v) => '<span class="hidden-value' + (hide ? " hidden" : "") + '">' + PF.formatMoney(v) + "</span>";

  const atrasados = PF.store.contracts.all().filter((c) => c.status === "atrasado");
  const clients = PF.store.clients.all();
  const activity = PF.store.activity();
  const receitaMeta = 10000;
  const receita = Number(metrics.receitaRecebida || PF.store.contracts.all().reduce((s, c) => s + Number(c.valorPago || 0), 0));
  const pct = Math.min(100, Math.round((receita / receitaMeta) * 100));

  document.getElementById("dashboard-root").innerHTML =
    PF.layout.trialBanner(user) +
    '<header class="page-header">' +
    "<div><h1>Olá, " + PF.escapeHtml(user.name.split(" ")[0]) + "</h1>" +
    "<p>Resumo da sua operação · dados do banco MySQL</p></div>" +
    '<div class="flex">' +
    '<button type="button" id="toggle-values" class="btn-ghost" aria-label="Ocultar valores">' + (hide ? "Mostrar valores" : "Ocultar valores") + "</button>" +
    '<a class="plan-chip' + (user.plan !== "gratuito" ? " premium" : "") + '" href="planos.html">Plano: ' + user.plan + "</a>" +
    '<div class="dropdown"><button type="button">+ Novo</button><div class="dropdown-menu">' +
    '<a href="cadastro.html">Novo cliente</a><a href="novo_contrato.html">Novo contrato</a></div></div></div></header>' +
    '<section class="card grid grid-4" style="margin-bottom:20px">' +
    '<a class="stat" href="clientes.html"><span class="icon">👥</span><h4>Clientes ativos</h4><p>' + (metrics.clientesAtivos || 0) + "</p></a>" +
    '<a class="stat" href="contratos.html"><span class="icon">📄</span><h4>Contratos vigentes</h4><p>' + (metrics.contratosVigentes || 0) + "</p></a>" +
    '<a class="stat" href="novos-mes.html"><span class="icon">✨</span><h4>Novos no mês</h4><p>' + (metrics.novosNoMes || 0) + "</p></a>" +
    '<a class="stat" href="agenda.html"><span class="icon">⚠️</span><h4>Pendências</h4><p>' + (metrics.pendencias || 0) + "</p></a>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:20px">' +
    '<div class="card"><h3>Total emprestado</h3><p class="stat">' + money(metrics.totalEmprestado) + "</p></div>" +
    '<div class="card"><h3>Total em atraso</h3><p class="stat">' + money(metrics.totalEmAtraso) + "</p></div>" +
    '<div class="card"><h3>Recebido (estimativa de lucro)</h3><p class="stat">' + money(metrics.lucroGerado) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-2">' +
    '<div class="card"><h2>Atividade recente</h2>' +
    (activity.length ? "<ul>" + activity.slice(0, 8).map((a) => "<li><strong>" + PF.formatDate(a.date) + "</strong> — " + PF.escapeHtml(a.text) + "</li>").join("") + "</ul>" : PF.ui.empty("Nenhuma atividade ainda.")) +
    "</div>" +
    '<div class="card"><h2>Inadimplência</h2>' +
    (atrasados.length ? "<ul>" + atrasados.map((c) => {
      const cli = clients.find((x) => String(x.id) === String(c.clienteId));
      return "<li class='flex' style='justify-content:space-between'><span>" + PF.escapeHtml(cli ? cli.nome : "Cliente") + "<br><small>" + PF.formatMoney(c.saldoDevedor) + "</small></span>" +
        (cli ? '<button type="button" data-wa="' + PF.digits(cli.telefone) + '" data-name="' + PF.escapeHtml(cli.nome) + '">Cobrar</button>' : "") + "</li>";
    }).join("") + "</ul>" : PF.ui.empty("Nenhum contrato em atraso.")) +
    '<h3 style="margin-top:20px">Meta de receita</h3><p>' + PF.formatMoney(receita) + " de " + PF.formatMoney(receitaMeta) + "</p>" +
    '<div class="progress"><span style="width:' + pct + '%"></span></div>' +
    "<p>Portfólio: " + (metrics.emDia || 0) + " em dia · " + (metrics.atrasados || 0) + " atrasados · " + (metrics.quitados || 0) + " quitados</p>" +
    "</div></section>";

  document.getElementById("toggle-values").addEventListener("click", async () => {
    const next = !PF.store.getSettings().hideValues;
    await PF.store.saveSettings({ hideValues: next });
    window.location.reload();
  });
  document.querySelectorAll("[data-wa]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const msg = encodeURIComponent("Olá " + btn.getAttribute("data-name") + ", lembrete de cobrança do PixFactory.");
      window.open("https://wa.me/55" + btn.getAttribute("data-wa") + "?text=" + msg, "_blank");
    });
  });
})();
