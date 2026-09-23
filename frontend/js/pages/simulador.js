(async function () {
  await PF.layout.mount("simulador");
  let simulation = null;

  function showTab(name) {
    document.querySelectorAll("[data-tab]").forEach((btn) => btn.classList.toggle("active", btn.getAttribute("data-tab") === name));
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.toggle("active", p.id === "tab-" + name));
  }

  document.getElementById("tabs").addEventListener("click", (e) => {
    const tab = e.target.closest("[data-tab]");
    if (tab) showTab(tab.getAttribute("data-tab"));
  });

  function setResult(nome, parcela, juros, total, rows) {
    document.getElementById("res-nome").textContent = nome || "—";
    document.getElementById("res-parcela").textContent = PF.formatMoney(parcela);
    document.getElementById("res-juros").textContent = PF.formatMoney(juros);
    document.getElementById("res-total").textContent = PF.formatMoney(total);
    const wrap = document.getElementById("amort-wrap");
    const tbody = document.getElementById("amort-body");
    if (!rows) {
      wrap.hidden = true;
      return;
    }
    wrap.hidden = false;
    tbody.innerHTML = rows.map((r) =>
      "<tr><td data-label='Parcela'>" + r.n + "</td><td data-label='Amortização'>" + PF.formatMoney(r.amort) + "</td><td data-label='Juros'>" + PF.formatMoney(r.juros) + "</td><td data-label='Valor'>" + PF.formatMoney(r.parcela) + "</td><td data-label='Saldo'>" + PF.formatMoney(r.saldo) + "</td></tr>"
    ).join("");
  }

  document.getElementById("emprestimo-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const nome = document.getElementById("nome-cliente").value.trim();
    const telefone = document.getElementById("telefone-cliente").value;
    const P = Number(document.getElementById("valor-emprestimo").value);
    const taxa = Number(document.getElementById("taxa-juros").value);
    const n = Number(document.getElementById("num-parcelas").value);
    if (!nome || !(P > 0) || !(taxa >= 0) || !(n > 0)) {
      PF.ui.toast("Preencha os campos com valores válidos.", "error");
      return;
    }
    const sistema = document.getElementById("sistema-emprestimo").value;
    try {
      const result = await PF.api.post("/api/simulacoes", {
        valor: P,
        juros: Number(document.getElementById("taxa-juros").value),
        parcelas: n,
        sistema: sistema,
        modo: sistema === "so_juros" || sistema === "americano" ? "so_juros"
          : (sistema === "so_juros_aberto" ? "juros_rotativo" : "parcela_cheia"),
        baseCalculo: document.getElementById("base-emprestimo").value,
        periodicidade: "mensal",
        tipo: sistema === "aluguel" ? "aluguel" : (sistema === "so_juros_aberto" ? "emprestimo rotativo" : "emprestimo"),
      });
      const rows = (result.cronograma || []).map((r) => ({ n: r.numero, amort: r.principal, juros: r.juros, parcela: r.parcela, saldo: r.saldo }));
      simulation = { nome, telefone, P, n, parcela: result.parcelaInicial, total: result.totalPagar, juros: result.totalJuros };
      setResult(nome, result.parcelaInicial, result.totalJuros, result.totalPagar, rows);
      document.getElementById("wa-box").hidden = false;
      PF.ui.toast(result.explicacao);
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível simular.", "error");
    }
  });

  document.getElementById("juros-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const nome = document.getElementById("juros-nome").value.trim();
    const telefone = document.getElementById("juros-telefone").value;
    const saldo = Number(document.getElementById("juros-saldo").value);
    const taxa = Number(document.getElementById("juros-taxa").value);
    if (!nome || !(saldo > 0) || !(taxa > 0)) {
      PF.ui.toast("Preencha os campos com valores válidos.", "error");
      return;
    }
    try {
      const result = await PF.api.post("/api/simulacoes", {
        valor: saldo,
        juros: taxa,
        parcelas: 1,
        sistema: "so_juros_aberto",
        modo: "juros_rotativo",
        baseCalculo: "saldo",
        periodicidade: "mensal",
      });
      simulation = { nome, telefone, saldo, jurosMensal: result.parcelaInicial };
      setResult(nome, result.parcelaInicial, result.parcelaInicial, result.parcelaInicial, null);
      document.getElementById("wa-box").hidden = false;
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível simular.", "error");
    }
  });

  document.getElementById("send-wa").addEventListener("click", () => {
    if (!simulation || !simulation.telefone) return PF.ui.toast("Informe o telefone.", "error");
    const msg = simulation.P
      ? "Olá " + simulation.nome + ", simulação PixFactory: " + PF.formatMoney(simulation.P) + " em " + simulation.n + "x de " + PF.formatMoney(simulation.parcela)
      : "Olá " + simulation.nome + ", juros mensais: " + PF.formatMoney(simulation.jurosMensal);
    window.open("https://wa.me/55" + PF.digits(simulation.telefone) + "?text=" + encodeURIComponent(msg), "_blank");
  });
})();
