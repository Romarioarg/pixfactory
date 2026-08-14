(async function () {
  await PF.layout.mount("contratos");
  const select = document.getElementById("cliente");
  const preset = new URLSearchParams(window.location.search).get("clienteId") || new URLSearchParams(window.location.search).get("id");
  select.innerHTML = '<option value="">Selecione</option>' + PF.store.clients.all().map((c) =>
    '<option value="' + c.id + '"' + (String(c.id) === String(preset) ? " selected" : "") + ">" + PF.escapeHtml(c.nome) + "</option>"
  ).join("");

  const preview = document.getElementById("contrato-preview");

  function calc() {
    const valor = Number(document.getElementById("valor").value || 0);
    const juros = Number(document.getElementById("juros").value || 0) / 100;
    const n = Number(document.getElementById("parcelas").value || 0);
    if (!valor || !n) return null;
    const i = juros;
    const parcela = i === 0 ? valor / n : valor * (i / (1 - Math.pow(1 + i, -n)));
    return { valor, juros, n, parcela, total: parcela * n };
  }

  function refreshPreview() {
    const client = PF.store.clients.get(select.value);
    const data = calc();
    if (!client || !data) {
      preview.textContent = "Preencha os dados para ver a prévia.";
      return;
    }
    preview.innerHTML =
      "<h3>Contrato de " + PF.escapeHtml(document.getElementById("tipo").value) + "</h3>" +
      "<p>Cliente: <strong>" + PF.escapeHtml(client.nome) + "</strong></p>" +
      "<p>Valor: " + PF.formatMoney(data.valor) + " em " + data.n + "x de " + PF.formatMoney(data.parcela) + "</p>" +
      "<p>Total: " + PF.formatMoney(data.total) + "</p>" +
      "<p>Primeiro vencimento: " + PF.formatDate(document.getElementById("vencimento").value || PF.isoDate(30)) + "</p>" +
      "<p>" + PF.escapeHtml(document.getElementById("clausulas").value || "Cláusulas padrão de cobrança e juros de mora.") + "</p>";
  }

  document.getElementById("novo-contrato-form").addEventListener("input", refreshPreview);
  document.getElementById("novo-contrato-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const client = PF.store.clients.get(select.value);
    const data = calc();
    if (!client) return PF.ui.toast("Selecione um cliente.", "error");
    if (!data) return PF.ui.toast("Informe valor e parcelas válidos.", "error");
    try {
      await PF.store.contracts.add({
        clienteId: client.id,
        tipo: document.getElementById("tipo").value,
        valorTotal: data.valor,
        valorPago: 0,
        parcelasPagas: 0,
        parcelasTotais: data.n,
        proximoPagamento: document.getElementById("vencimento").value || PF.isoDate(30),
        status: "pendente",
        juros: Number(document.getElementById("juros").value || 0),
        multa: 20,
        saldoDevedor: data.valor,
      });
      PF.ui.toast("Contrato salvo.");
      window.location.href = "contratos.html";
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível salvar o contrato.", "error");
    }
  });

  document.getElementById("print-contract").addEventListener("click", () => window.print());
  document.getElementById("vencimento").value = PF.isoDate(30);
  refreshPreview();
})();
