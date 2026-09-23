(async function () {
  const user = await PF.layout.mount("dashboard");
  if (!user) return;

  const metrics = PF.store.metrics();
  const hide = PF.store.getSettings().hideValues;
  const money = (v) => '<span class="hidden-value' + (hide ? " hidden" : "") + '">' + PF.formatMoney(v) + "</span>";
  const trend = (n) => {
    const v = Number(n) || 0;
    if (!v) return "";
    const cls = v > 0 ? "up" : "down";
    const sign = v > 0 ? "+" : "";
    return '<span class="trend ' + cls + '">' + sign + v + "%</span>";
  };

  const activity = PF.store.activity();
  const upcoming = metrics.proximosVencimentos || PF.store.charges.all()
    .filter((c) => c.status !== "pago" && c.status !== "cancelado")
    .sort((a, b) => String(a.vencimento).localeCompare(String(b.vencimento)))
    .slice(0, 8);
  const alertas = metrics.alertas || [];
  const jurosDoPeriodo = metrics.jurosDoPeriodo || [];
  const receitaMeta = 10000;
  const receita = Number(metrics.receitaRecebida || 0);
  const pct = Math.min(100, Math.round((receita / receitaMeta) * 100));

  document.getElementById("dashboard-root").innerHTML =
    PF.layout.trialBanner(user) +
    '<header class="page-header">' +
    "<div><h1>Olá, " + PF.escapeHtml(user.name.split(" ")[0]) + "</h1>" +
    "<p>Resumo da operação · dados do banco</p></div>" +
    '<div class="flex">' +
    '<button type="button" id="toggle-values" class="btn-ghost" aria-label="Ocultar valores">' + (hide ? "Mostrar valores" : "Ocultar valores") + "</button>" +
    '<a class="plan-chip' + (user.plan !== "gratuito" ? " premium" : "") + '" href="planos.html">Plano: ' + user.plan + "</a>" +
    '<div class="cta-row actions-row">' +
    '<a class="btn-ghost" href="cadastro.html">Novo cliente</a>' +
    '<a class="btn" href="novo_contrato.html">Novo contrato</a>' +
    "</div></div></header>" +
    '<nav class="quick-nav" aria-label="Atalhos">' +
    '<a class="btn" href="clientes.html">Clientes</a>' +
    '<a class="btn" href="contratos.html">Contratos</a>' +
    '<a class="btn" href="cadastro.html">Cadastro</a>' +
    '<a class="btn" href="agenda.html">Agenda</a>' +
    '<a class="btn" href="inadimplencia.html">Inadimplência</a>' +
    '<a class="btn" href="planos.html">Planos</a>' +
    '<a class="btn" href="caixa.html">Caixa</a>' +
    "</nav>" +
    (alertas.length ? '<section class="card" style="margin-bottom:20px"><h3>Alertas</h3><ul>' +
      alertas.map((a) => "<li>" + PF.escapeHtml(a) + "</li>").join("") + "</ul></section>" : "") +
    (jurosDoPeriodo.length ? '<section class="card period-interest"><h3>Juros do período</h3><p class="hint">Contratos de só juros até quitar. O sistema gera o próximo juro quando a data chega; o capital continua em aberto.</p><ul>' +
      jurosDoPeriodo.map((c) =>
        "<li class='flex' style='justify-content:space-between;gap:8px'><span><strong>" + PF.escapeHtml(c.clienteNome || "Cliente") +
        "</strong><br><small>" + money(c.saldo || c.valor) + " · " + PF.formatDate(c.vencimento) +
        " · " + PF.escapeHtml(c.rotulo || "") +
        (c.status ? ' · <span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span>" : "") +
        "</small></span><span class='actions-row'>" +
        (c.acao === "gerar" && c.contratoId
          ? '<button type="button" data-gerar-juro="' + c.contratoId + '">Gerar juro</button>'
          : "") +
        (c.telefone ? '<button type="button" data-charge-wa="' + (c.id || "") + '" data-wa-phone="' + PF.escapeHtml(c.telefone) + '" data-wa-name="' + PF.escapeHtml(c.clienteNome || "") + '" data-wa-valor="' + (c.saldo || c.valor || 0) + '">Cobrar</button>' : "") +
        (c.id ? '<a class="btn-ghost" href="agenda.html">Agenda</a>' : "") +
        "</span></li>"
      ).join("") + "</ul></section>" : "") +
    '<section class="card grid grid-4" style="margin-bottom:20px">' +
    '<a class="stat" href="clientes.html"><span class="icon"><i class="fa-solid fa-users"></i></span><h4>Clientes ativos</h4><p>' + (metrics.clientesAtivos || 0) + "</p></a>" +
    '<a class="stat" href="contratos.html"><span class="icon"><i class="fa-solid fa-file-contract"></i></span><h4>Contratos vigentes</h4><p>' + (metrics.contratosVigentes || 0) + "</p></a>" +
    '<a class="stat" href="novos-mes.html"><span class="icon"><i class="fa-solid fa-star"></i></span><h4>Novos no mês</h4><p>' + (metrics.novosNoMes || 0) + trend(metrics.tendenciaNovos) + "</p></a>" +
    '<a class="stat" href="inadimplencia.html"><span class="icon"><i class="fa-solid fa-triangle-exclamation"></i></span><h4>Pendências</h4><p>' + (metrics.pendencias || 0) + "</p></a>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:20px">' +
    '<div class="card"><h3>Capital emprestado</h3><p class="stat">' + money(metrics.totalEmprestado) + "</p></div>" +
    '<div class="card"><h3>Carteira em aberto</h3><p class="stat">' + money(metrics.capitalCarteira || metrics.aReceber) + "</p></div>" +
    '<div class="card"><h3>Em atraso</h3><p class="stat">' + money(metrics.totalEmAtraso) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:20px">' +
    '<div class="card"><h3>Recebido</h3><p class="stat">' + money(metrics.receitaRecebida) + "</p></div>" +
    '<div class="card"><h3>A receber</h3><p class="stat">' + money(metrics.aReceber) + "</p></div>" +
    '<div class="card"><h3>Vencendo hoje</h3><p class="stat">' + (metrics.vencendoHoje || 0) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:20px">' +
    '<div class="card"><h3>Follow-ups hoje</h3><p class="stat">' + (metrics.followUpsHoje || 0) + "</p></div>" +
    '<div class="card"><h3>Multa + mora</h3><p class="stat">' + money(metrics.multaMoraAplicada) + "</p></div>" +
    '<div class="card"><h3>Promessas hoje</h3><p class="stat">' + (metrics.promessasHoje || 0) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-3" style="margin-bottom:20px">' +
    '<div class="card"><h3>Carteira aberta</h3><p class="stat">' + money(metrics.carteiraAberto || metrics.capitalCarteira) + "</p></div>" +
    '<div class="card"><h3>A vencer</h3><p class="stat">' + money(metrics.carteiraAVencer) + "</p></div>" +
    '<div class="card"><h3>Inadimplentes</h3><p class="stat">' + (metrics.clientesInadimplentes || 0) + "</p></div>" +
    "</section>" +
    '<section class="grid grid-2">' +
    '<div class="card"><h2>Atividade recente</h2>' +
    (activity.length ? "<ul>" + activity.slice(0, 8).map((a) => "<li><strong>" + PF.formatDate(a.date) + "</strong> — " + PF.escapeHtml(a.text) + "</li>").join("") + "</ul>" : PF.ui.empty("Nenhuma atividade ainda.")) +
    '<h3 style="margin-top:20px">Movimentação</h3><canvas id="dash-chart" width="420" height="180" aria-label="Gráfico financeiro"></canvas>' +
    "</div>" +
    '<div class="card"><h2>Próximos vencimentos</h2>' +
    (upcoming.length ? "<ul>" + upcoming.map((c) =>
      "<li class='flex' style='justify-content:space-between;gap:8px;margin-bottom:8px'><span><strong>" + PF.escapeHtml(c.clienteNome || "Cliente") +
      "</strong><br><small>" + PF.formatMoney(c.saldo || c.valor) + " · " + PF.formatDate(c.vencimento) +
      ' · <span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span></small></span>" +
      (c.telefone ? '<button type="button" data-charge-wa="' + c.id + '">Cobrar</button>' : "") + "</li>"
    ).join("") + "</ul>" : PF.ui.empty("Nenhuma cobrança prevista.")) +
    '<h3 style="margin-top:20px">Meta de receita</h3><p>' + PF.formatMoney(receita) + " de " + PF.formatMoney(receitaMeta) + "</p>" +
    '<div class="progress"><span style="width:' + pct + '%"></span></div>' +
    "<p>Portfólio: " + (metrics.emDia || 0) + " em dia · " + (metrics.atrasados || 0) + " atrasados · " + (metrics.quitados || 0) + " quitados</p>" +
    "</div></section>";

  document.getElementById("toggle-values").addEventListener("click", async () => {
    const next = !PF.store.getSettings().hideValues;
    await PF.store.saveSettings({ hideValues: next });
    window.location.reload();
  });
  document.querySelectorAll("[data-charge-wa]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.getAttribute("data-charge-wa");
      const charge = id ? PF.store.charges.get(id) : null;
      const phone = charge ? charge.telefone : btn.getAttribute("data-wa-phone");
      const message = charge
        ? PF.whatsappChargeMessage(charge)
        : "Olá " + (btn.getAttribute("data-wa-name") || "") + ", o juro do período no PixFactory é " + PF.formatMoney(btn.getAttribute("data-wa-valor")) + ".";
      const ok = PF.openWhatsApp(phone, message);
      if (!ok) return PF.ui.toast("Cliente sem telefone.", "error");
      if (!charge) return;
      try {
        await PF.store.contactCharge(charge.id, "whatsapp", "mensagem enviada", "Cobrança pelo dashboard");
      } catch (err) { /* contato é complementar */ }
    });
  });
  document.querySelectorAll("[data-gerar-juro]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      try {
        await PF.store.contractAction(btn.getAttribute("data-gerar-juro"), "gerar_juros", {});
        await PF.store.hydrate();
        PF.ui.toast("Juro do período gerado.");
        window.location.reload();
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível gerar o juro.", "error");
      }
    });
  });

  try {
    const report = await PF.api.get("/api/reports");
    const series = report.series || [];
    const canvas = document.getElementById("dash-chart");
    if (window.Chart && canvas) {
      new window.Chart(canvas, {
        type: "bar",
        data: {
          labels: series.map((s) => s.label),
          datasets: [{
            label: "R$",
            data: series.map((s) => Number(s.value) || 0),
            backgroundColor: "#04B45F",
          }],
        },
        options: {
          plugins: { legend: { display: false } },
          scales: {
            x: { ticks: { color: "#ccc" }, grid: { color: "rgba(255,255,255,0.06)" } },
            y: { ticks: { color: "#ccc" }, grid: { color: "rgba(255,255,255,0.06)" } },
          },
        },
      });
    }
  } catch (err) {
    const canvas = document.getElementById("dash-chart");
    if (canvas) canvas.replaceWith(Object.assign(document.createElement("p"), { className: "hint", textContent: "Gráfico indisponível no momento." }));
  }
})();
