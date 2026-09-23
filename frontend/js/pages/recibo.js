(async function () {
  await PF.layout.mount("agenda");
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");
  const root = document.getElementById("recibo-root");
  if (!id) {
    root.innerHTML = PF.ui.empty("Informe a cobrança do recibo.");
    return;
  }
  try {
    const r = await PF.api.get("/api/cobrancas/" + encodeURIComponent(id) + "/recibo");
    root.innerHTML =
      "<p><strong>" + PF.escapeHtml(r.empresa || "PixFactory") + "</strong></p>" +
      "<p>Recibo " + PF.escapeHtml(r.reciboId) + " · " + PF.formatDate(r.emitidoEm) + "</p>" +
      "<h2>" + PF.escapeHtml(r.clienteNome || "Cliente") + "</h2>" +
      "<p>CPF " + PF.escapeHtml(r.clienteCpf || "—") + "</p>" +
      "<p>Contrato #" + PF.escapeHtml(String(r.contratoId || "—")) + " · Parcela " + PF.escapeHtml(String(r.numero || "—")) + "</p>" +
      "<p>Valor da parcela: " + PF.formatMoney(r.valor) + "</p>" +
      "<p>Valor pago: " + PF.formatMoney(r.valorPago) + "</p>" +
      "<p>Saldo: " + PF.formatMoney(r.saldo) + "</p>" +
      (Number(r.multaAplicada) ? "<p>Multa: " + PF.formatMoney(r.multaAplicada) + "</p>" : "") +
      (Number(r.moraAplicada) ? "<p>Juros de mora: " + PF.formatMoney(r.moraAplicada) + "</p>" : "") +
      "<h3>Movimentos</h3>" +
      ((r.historico || []).length ? "<ul>" + r.historico.map((h) => "<li>" + PF.escapeHtml(h.tipo) + " · " + PF.formatMoney(h.valor) + " · " + PF.escapeHtml(h.motivo || "") + "</li>").join("") + "</ul>" : PF.ui.empty("Sem lançamentos no extrato.")) +
      "<p class='hint'>" + PF.escapeHtml(r.aviso || "") + "</p>" +
      '<div class="actions-row no-print">' +
      (r.telefone ? '<button type="button" id="wa-recibo">Enviar no WhatsApp</button>' : "") +
      "</div>";
    const wa = document.getElementById("wa-recibo");
    if (wa) {
      wa.addEventListener("click", () => {
        const ok = PF.openWhatsApp(r.telefone, PF.whatsappReceiptMessage(r));
        if (!ok) PF.ui.toast("Cliente sem telefone.", "error");
      });
    }
  } catch (err) {
    root.innerHTML = PF.ui.empty(err.message || "Não foi possível gerar o recibo.");
  }
  document.getElementById("print-recibo").addEventListener("click", () => window.print());
})();
