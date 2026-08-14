(async function () {
  const user = await PF.layout.mount("gastos");
  document.getElementById("fin-nav").innerHTML = PF.layout.financeNav("relatorios");
  document.getElementById("banner").innerHTML = PF.layout.trialBanner(user);
  const report = await PF.api.get("/api/reports");
  const series = report.series || [];

  document.getElementById("report-root").innerHTML =
    '<div class="grid grid-3"><div class="card"><h3>Emprestado</h3><p>' + PF.formatMoney(report.totalEmprestado) + "</p></div>" +
    '<div class="card"><h3>Em atraso</h3><p>' + PF.formatMoney(report.totalEmAtraso) + "</p></div>" +
    '<div class="card"><h3>Recebido (aprox.)</h3><p>' + PF.formatMoney(report.lucroGerado) + "</p></div></div>" +
    '<div class="grid grid-2" style="margin-top:16px"><div class="card"><h3>Lançamentos</h3><p>Receitas ' + PF.formatMoney(report.receitas) + "<br>Despesas " + PF.formatMoney(report.despesas) + "</p>" +
    '<canvas id="report-chart" width="480" height="220" aria-label="Gráfico de relatórios"></canvas></div>' +
    '<div class="card"><h3>Insight</h3><p>' + PF.escapeHtml(report.insight || "") + "</p><p class='hint'>Dados reais do banco. Integrações externas permanecem em modo DEMO.</p></div></div>";

  const canvas = document.getElementById("report-chart");
  if (canvas && canvas.getContext) {
    const ctx = canvas.getContext("2d");
    const max = Math.max(1, ...series.map((s) => Number(s.value) || 0));
    const barW = 60;
    series.forEach((s, i) => {
      const h = (Number(s.value) / max) * 160;
      const x = 30 + i * 90;
      ctx.fillStyle = i % 2 ? "#2ecc71" : "#3498db";
      ctx.fillRect(x, 180 - h, barW, h);
      ctx.fillStyle = "#ccc";
      ctx.font = "11px sans-serif";
      ctx.fillText(s.label, x, 200);
    });
  }
})();
