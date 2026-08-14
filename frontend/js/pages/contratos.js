(async function () {
  await PF.layout.mount("contratos");
  let filter = "todos";
  const body = document.getElementById("contracts-body");
  const search = document.getElementById("contract-search");
  let currentId = null;

  function clientName(id) {
    const c = PF.store.clients.get(id);
    return c ? c.nome : "Cliente";
  }

  function list() {
    const q = String(search.value || "").toLowerCase();
    return PF.store.contracts.all().filter((c) => {
      if (filter !== "todos" && c.status !== filter) return false;
      const name = clientName(c.clienteId).toLowerCase();
      return name.includes(q) || String(c.tipo).toLowerCase().includes(q) || String(c.id).toLowerCase().includes(q);
    });
  }

  function render() {
    const rows = list();
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6">' + PF.ui.empty("Nenhum contrato encontrado.") + "</td></tr>";
      return;
    }
    body.innerHTML = rows.map((c) =>
      "<tr>" +
      '<td data-label="Cliente">' + PF.escapeHtml(clientName(c.clienteId)) + "</td>" +
      '<td data-label="Tipo">' + PF.escapeHtml(c.tipo) + "</td>" +
      '<td data-label="Valor">' + PF.formatMoney(c.valorTotal) + "</td>" +
      '<td data-label="Saldo">' + PF.formatMoney(c.saldoDevedor) + "</td>" +
      '<td data-label="Status"><span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span></td>" +
      '<td data-label="Ações"><button type="button" data-open="' + c.id + '">Gerenciar</button></td>' +
      "</tr>"
    ).join("");
  }

  function paymentBox(payment) {
    if (!payment) return "";
    return '<div class="card" style="margin-top:12px;border:1px dashed #2ecc71">' +
      "<p><strong>PIX DEMO</strong> · " + PF.escapeHtml(payment.txid) + "</p>" +
      "<p>Status: " + PF.escapeHtml(payment.status) + " · Valor " + PF.formatMoney(payment.amount) + "</p>" +
      "<p class='hint'>Copia e cola fictício:</p><code>" + PF.escapeHtml(payment.copyPaste || "") + "</code>" +
      (payment.status === "PENDING"
        ? '<div class="actions-row" style="margin-top:10px">' +
          '<button type="button" data-confirm-pay="' + payment.id + '">Confirmar pagamento DEMO</button>' +
          '<button type="button" class="btn-ghost" data-fail-pay="' + payment.id + '">Simular falha</button>' +
          '<button type="button" class="btn-danger" data-cancel-pay="' + payment.id + '">Cancelar</button></div>'
        : "") +
      "</div>";
  }

  function openManage(id) {
    currentId = id;
    const c = PF.store.contracts.get(id);
    if (!c) return;
    const lastPay = PF.store.payments.all().filter((p) => String(p.contractId) === String(id)).slice(-1)[0];
    document.getElementById("manage-body").innerHTML =
      "<h2>" + PF.escapeHtml(c.tipo) + " · " + PF.escapeHtml(clientName(c.clienteId)) + "</h2>" +
      "<p>Total " + PF.formatMoney(c.valorTotal) + " · Pago " + PF.formatMoney(c.valorPago) + " · Saldo " + PF.formatMoney(c.saldoDevedor) + "</p>" +
      "<p>Parcelas " + c.parcelasPagas + "/" + c.parcelasTotais + " · Próximo: " + PF.formatDate(c.proximoPagamento) + "</p>" +
      "<p>Juros " + c.juros + "% · Multa " + PF.formatMoney(c.multa) + "</p>" +
      '<div class="actions-row">' +
      '<button type="button" data-act="pix">Pix Demo</button>' +
      '<button type="button" data-act="pagar">Registrar pagamento</button>' +
      '<button type="button" data-act="alongar">Alongar prazo</button>' +
      '<button type="button" data-act="extra">Novo empréstimo</button>' +
      '<button type="button" data-act="bem">Pagamento com bem</button>' +
      '<button type="button" data-act="reneg">Renegociar</button>' +
      '<button type="button" data-act="quitar">Quitar</button>' +
      '<button type="button" data-act="acordo">Acordo</button>' +
      '<button type="button" data-act="transferir">Transferir</button>' +
      '<button type="button" data-act="hold">Colocar em espera</button>' +
      '<button type="button" data-act="reativar">Reativar</button>' +
      '<button type="button" class="btn-danger" data-act="falecimento">Encerrar por falecimento</button>' +
      '<button type="button" class="btn-ghost" data-act="whats">WhatsApp</button>' +
      "</div>" +
      paymentBox(lastPay) +
      "<h3>Histórico</h3><ul>" + (c.historico || []).map((h) => "<li>" + PF.formatDate(h.data) + " — " + PF.escapeHtml(h.descricao) + "</li>").join("") + "</ul>";
    PF.ui.openModal("manage-modal");
  }

  async function runAction(id, action, extra) {
    try {
      await PF.store.contractAction(id, action, extra || {});
      render();
      openManage(id);
      PF.ui.toast("Contrato atualizado.");
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível atualizar o contrato.", "error");
    }
  }

  document.getElementById("filters").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-filter]");
    if (!btn) return;
    filter = btn.getAttribute("data-filter");
    render();
  });
  search.addEventListener("input", render);
  body.addEventListener("click", (e) => {
    const open = e.target.closest("[data-open]");
    if (open) openManage(open.getAttribute("data-open"));
  });

  document.getElementById("manage-body").addEventListener("click", async (e) => {
    const confirmPay = e.target.closest("[data-confirm-pay]");
    const failPay = e.target.closest("[data-fail-pay]");
    const cancelPay = e.target.closest("[data-cancel-pay]");
    if (confirmPay) {
      await PF.store.confirmPayment(confirmPay.getAttribute("data-confirm-pay"));
      render();
      openManage(currentId);
      return PF.ui.toast("PIX DEMO confirmado. Contrato atualizado.");
    }
    if (failPay) {
      await PF.api.post("/api/payments/" + failPay.getAttribute("data-fail-pay") + "/fail");
      await PF.store.hydrate();
      openManage(currentId);
      return;
    }
    if (cancelPay) {
      await PF.api.post("/api/payments/" + cancelPay.getAttribute("data-cancel-pay") + "/cancel");
      await PF.store.hydrate();
      openManage(currentId);
      return;
    }

    const act = e.target.closest("[data-act]");
    if (!act || !currentId) return;
    const c = PF.store.contracts.get(currentId);
    const action = act.getAttribute("data-act");
    if (action === "pix") {
      const valor = Number(prompt("Valor do Pix Demo:", "250"));
      if (!valor || valor <= 0) return;
      const payment = await PF.store.createPixPayment(currentId, valor);
      openManage(currentId);
      return PF.ui.toast("Cobrança PIX DEMO criada: " + payment.txid);
    }
    if (action === "pagar") {
      const valor = Number(prompt("Valor do pagamento:", "250"));
      if (!valor || valor <= 0) return;
      return runAction(currentId, "pagar", { valor: valor });
    }
    if (action === "alongar") {
      const meses = Number(prompt("Novo total de parcelas:", c.parcelasTotais));
      if (!meses) return;
      return runAction(currentId, "alongar", { parcelasTotais: meses });
    }
    if (action === "extra") {
      const valor = Number(prompt("Valor adicional:", "500"));
      if (!valor) return;
      return runAction(currentId, "extra", { valor: valor });
    }
    if (action === "bem") {
      const desc = prompt("Descrição do bem:");
      const valor = Number(prompt("Valor de abatimento:"));
      if (!desc || !valor) return;
      return runAction(currentId, "bem", { descricao: desc, valor: valor });
    }
    if (action === "reneg") {
      const juros = Number(prompt("Nova taxa de juros (%):", c.juros));
      if (juros == null) return;
      return runAction(currentId, "reneg", { juros: juros });
    }
    if (action === "quitar") {
      if (!PF.ui.confirm("Quitar o saldo de " + PF.formatMoney(c.saldoDevedor) + "?")) return;
      return runAction(currentId, "quitar", {});
    }
    if (action === "acordo") {
      const valor = Number(prompt("Valor do acordo:", c.saldoDevedor));
      if (!valor) return;
      return runAction(currentId, "acordo", { valor: valor });
    }
    if (action === "transferir") {
      const names = PF.store.clients.all().map((cl) => cl.id + " = " + cl.nome).join("\n");
      const next = prompt("ID do novo cliente:\n" + names);
      if (!next) return;
      return runAction(currentId, "transferir", { clienteId: next });
    }
    if (action === "hold") return runAction(currentId, "hold", {});
    if (action === "reativar") return runAction(currentId, "reativar", {});
    if (action === "falecimento") {
      if (!PF.ui.confirm("Encerrar por falecimento? As cobranças param.")) return;
      return runAction(currentId, "falecimento", {});
    }
    if (action === "whats") {
      const cli = PF.store.clients.get(c.clienteId);
      if (!cli) return;
      const msg = encodeURIComponent("Olá " + cli.nome + ", contato sobre seu contrato no PixFactory. Saldo: " + PF.formatMoney(c.saldoDevedor));
      window.open("https://wa.me/55" + PF.digits(cli.telefone) + "?text=" + msg, "_blank");
    }
  });

  render();
})();
