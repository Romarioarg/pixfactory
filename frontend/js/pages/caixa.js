(async function () {
  const user = await PF.layout.mount("caixa");
  document.getElementById("fin-nav").innerHTML = PF.layout.financeNav("caixa");
  const root = document.getElementById("caixa-root");

  async function load() {
    const today = await PF.api.get("/api/caixa/hoje");
    const history = await PF.api.get("/api/caixa");
    if (!today.aberto) {
      root.innerHTML =
        PF.ui.empty("Nenhum caixa aberto hoje.") +
        '<form id="open-form" class="card stack" style="margin-top:16px">' +
        '<div class="field"><label for="open-saldo">Saldo de abertura</label><input id="open-saldo" type="number" step="0.01" min="0" value="0" required></div>' +
        "<button type='submit'>Abrir caixa</button></form>" +
        historyBlock(history);
      return;
    }
    root.innerHTML =
      '<section class="grid grid-4" style="margin-bottom:16px">' +
      '<div class="card"><h3>Abertura</h3><p class="stat">' + PF.formatMoney(today.opening) + "</p></div>" +
      '<div class="card"><h3>Entradas</h3><p class="stat">' + PF.formatMoney(today.entradas) + "</p></div>" +
      '<div class="card"><h3>Saídas</h3><p class="stat">' + PF.formatMoney(today.saidas) + "</p></div>" +
      '<div class="card"><h3>Esperado</h3><p class="stat">' + PF.formatMoney(today.expected) + "</p></div>" +
      "</section>" +
      '<section class="card" style="margin-bottom:16px"><h2>Movimentos</h2>' +
      ((today.movimentos || []).length ? "<ul>" + today.movimentos.map((m) => "<li>" + PF.escapeHtml(m.tipo) + " · " + PF.formatMoney(m.valor) + " · " + PF.escapeHtml(m.descricao || m.origem || "") + "</li>").join("") + "</ul>" : PF.ui.empty("Ainda sem movimentos.")) +
      "</section>" +
      '<section class="grid grid-2"><form id="move-form" class="card stack"><h3>Lançar no caixa</h3>' +
      '<div class="field"><label for="move-tipo">Tipo</label><select id="move-tipo"><option value="entrada">Entrada</option><option value="saida">Saída</option></select></div>' +
      '<div class="field"><label for="move-valor">Valor</label><input id="move-valor" type="number" step="0.01" min="0.01" required></div>' +
      '<div class="field"><label for="move-desc">Descrição</label><input id="move-desc" placeholder="Sangria, despesa, reforço..."></div>' +
      "<button type='submit'>Registrar</button></form>" +
      '<form id="close-form" class="card stack"><h3>Fechar o dia</h3>' +
      '<div class="field"><label for="close-saldo">Valor contado</label><input id="close-saldo" type="number" step="0.01" min="0" required></div>' +
      '<div class="field"><label for="close-notes">Divergência / observação</label><textarea id="close-notes" rows="3"></textarea></div>' +
      "<button type='submit'>Fechar caixa</button></form></section>" +
      historyBlock(history);
  }

  function historyBlock(history) {
    const rows = (history || []).filter((s) => s.status === "fechado").slice(0, 8);
    if (!rows.length) return "";
    return '<section class="card" style="margin-top:16px"><h2>Fechamentos recentes</h2><ul>' +
      rows.map((s) => "<li>" + PF.formatDate(s.date) + " · esperado " + PF.formatMoney(s.expected) + " · contado " + PF.formatMoney(s.informed) + " · diferença " + PF.formatMoney(s.difference) + "</li>").join("") +
      "</ul></section>";
  }

  root.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      if (e.target.id === "open-form") {
        await PF.api.post("/api/caixa/abrir", { saldoInicial: Number(document.getElementById("open-saldo").value) });
        PF.ui.toast("Caixa aberto.");
      }
      if (e.target.id === "move-form") {
        await PF.api.post("/api/caixa/movimentos", {
          tipo: document.getElementById("move-tipo").value,
          valor: Number(document.getElementById("move-valor").value),
          descricao: document.getElementById("move-desc").value,
        });
        PF.ui.toast("Movimento lançado.");
      }
      if (e.target.id === "close-form") {
        const informed = Number(document.getElementById("close-saldo").value);
        if (!PF.ui.confirm("Fechar o caixa com " + PF.formatMoney(informed) + "?")) return;
        const result = await PF.api.post("/api/caixa/fechar", {
          saldoInformado: informed,
          justificativa: document.getElementById("close-notes").value,
        });
        PF.ui.toast(Number(result.difference) === 0 ? "Caixa fechado sem diferença." : "Caixa fechado. Diferença: " + PF.formatMoney(result.difference));
      }
      await load();
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível atualizar o caixa.", "error");
    }
  });

  await load();
})();
