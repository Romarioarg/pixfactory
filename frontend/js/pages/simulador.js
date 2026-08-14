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

  document.getElementById("emprestimo-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const nome = document.getElementById("nome-cliente").value.trim();
    const telefone = document.getElementById("telefone-cliente").value;
    const P = Number(document.getElementById("valor-emprestimo").value);
    const taxa = Number(document.getElementById("taxa-juros").value) / 100;
    const n = Number(document.getElementById("num-parcelas").value);
    const freq = document.getElementById("frequencia-pagamento").value;
    if (!nome || !(P > 0) || !(taxa >= 0) || !(n > 0)) {
      PF.ui.toast("Preencha os campos com valores válidos.", "error");
      return;
    }
    let periods = n;
    let i = taxa;
    if (freq === "semanal") { periods = n; i = taxa / 4; }
    if (freq === "diario") { periods = n; i = taxa / 30; }
    if (freq === "anual") { periods = n; i = Math.pow(1 + taxa, 12) - 1; }
    const parcela = i === 0 ? P / periods : P * (i / (1 - Math.pow(1 + i, -periods)));
    const total = parcela * periods;
    const rows = [];
    let saldo = P;
    for (let k = 1; k <= periods; k += 1) {
      const juros = saldo * i;
      const amort = parcela - juros;
      saldo = Math.max(0, saldo - amort);
      rows.push({ n: k, amort, juros, parcela, saldo });
    }
    simulation = { nome, telefone, P, n, parcela, total, juros: total - P };
    setResult(nome, parcela, total - P, total, rows);
    document.getElementById("wa-box").hidden = false;
    PF.ui.toast("Simulação calculada.");
  });

  document.getElementById("juros-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const nome = document.getElementById("juros-nome").value.trim();
    const telefone = document.getElementById("juros-telefone").value;
    const saldo = Number(document.getElementById("juros-saldo").value);
    const taxa = Number(document.getElementById("juros-taxa").value) / 100;
    if (!nome || !(saldo > 0) || !(taxa > 0)) {
      PF.ui.toast("Preencha os campos com valores válidos.", "error");
      return;
    }
    const juros = saldo * taxa;
    simulation = { nome, telefone, saldo, jurosMensal: juros };
    setResult(nome, juros, juros, juros, null);
    document.getElementById("wa-box").hidden = false;
  });

  document.getElementById("send-wa").addEventListener("click", () => {
    if (!simulation || !simulation.telefone) return PF.ui.toast("Informe o telefone.", "error");
    const msg = simulation.P
      ? "Olá " + simulation.nome + ", simulação PixFactory: " + PF.formatMoney(simulation.P) + " em " + simulation.n + "x de " + PF.formatMoney(simulation.parcela)
      : "Olá " + simulation.nome + ", juros mensais: " + PF.formatMoney(simulation.jurosMensal);
    window.open("https://wa.me/55" + PF.digits(simulation.telefone) + "?text=" + encodeURIComponent(msg), "_blank");
  });
})();
