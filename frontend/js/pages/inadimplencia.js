(async function () {
  await PF.layout.mount("inadimplencia");
  let aging = "todos";
  const body = document.getElementById("inad-body");
  const summary = document.getElementById("inad-summary");

  function daysLate(iso) {
    if (!iso) return 0;
    const due = new Date(iso + "T12:00:00");
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    return Math.max(0, Math.floor((today - due) / 86400000));
  }

  function inBucket(row) {
    const days = row.dias;
    if (aging === "todos") return true;
    if (aging === "1-7") return days >= 1 && days <= 7;
    if (aging === "8-30") return days >= 8 && days <= 30;
    if (aging === "31-60") return days >= 31 && days <= 60;
    if (aging === "61-90") return days >= 61 && days <= 90;
    if (aging === "90+") return days > 90;
    if (aging === "sem_contato") {
      if (!row.ultimoContato) return true;
      const last = new Date(row.ultimoContato);
      return (Date.now() - last.getTime()) > 7 * 86400000;
    }
    if (aging === "promessa_hoje") return row.promessaStatus === "pendente" && row.promessaData === PF.isoDate(0);
    if (aging === "quebrada") return row.promessaStatus === "quebrada";
    return true;
  }

  function rows() {
    return PF.store.charges.all()
      .filter((c) => c.status === "atrasado" || c.promessaStatus === "quebrada" || (aging === "promessa_hoje" && c.promessaStatus === "pendente"))
      .map((c) => Object.assign({}, c, { dias: c.diasAtraso != null ? Number(c.diasAtraso) : daysLate(c.vencimento) }))
      .filter((c) => inBucket(c))
      .sort((a, b) => Number(b.saldo || b.valor) - Number(a.saldo || a.valor));
  }

  function render() {
    const list = rows();
    const total = list.reduce((s, c) => s + Number(c.saldo || c.valor || 0), 0);
    const critical = new Set(list.filter((c) => c.dias > 30).map((c) => c.clienteId));
    summary.innerHTML =
      '<div class="card"><h3>Valor em atraso</h3><p class="stat">' + PF.formatMoney(total) + "</p></div>" +
      '<div class="card"><h3>Parcelas</h3><p class="stat">' + list.length + "</p></div>" +
      '<div class="card"><h3>Atenção</h3><p class="stat">' + critical.size + "</p><p class='hint'>Clientes com atraso acima de 30 dias. Indicador operacional, sem bloqueio automático.</p></div>";
    if (!list.length) {
      body.innerHTML = '<tr><td colspan="7">' + PF.ui.empty("Nenhuma parcela em atraso neste filtro.") + "</td></tr>";
      return;
    }
    body.innerHTML = list.map((c) =>
      "<tr>" +
      '<td data-label="Cliente"><a href="cliente.html?id=' + encodeURIComponent(c.clienteId) + '">' + PF.escapeHtml(c.clienteNome || "Cliente") + "</a>" +
      (c.dias > 30 ? ' <span class="badge badge-danger">Atenção</span>' : "") + "</td>" +
      '<td data-label="Parcela">' + c.numero + "</td>" +
      '<td data-label="Valor">' + PF.formatMoney(c.saldo || c.valor) +
      (Number(c.multaAplicada) || Number(c.moraAplicada)
        ? "<br><small>Multa " + PF.formatMoney(c.multaAplicada) + " · Mora " + PF.formatMoney(c.moraAplicada) + "</small>"
        : "") + "</td>" +
      '<td data-label="Dias">' + c.dias + "</td>" +
      '<td data-label="Contato">' + PF.escapeHtml(c.ultimoCanal || "—") + "</td>" +
      '<td data-label="Promessa">' + (c.promessaData ? PF.formatDate(c.promessaData) + " (" + PF.escapeHtml(c.promessaStatus || "") + ")" : "—") +
      (c.proximaAcao ? "<br><small>" + PF.escapeHtml(c.proximaAcao) + "</small>" : "") + "</td>" +
      '<td data-label="Ações"><div class="actions-row">' +
      '<button type="button" data-wa="' + c.id + '">Cobrar</button>' +
      '<button type="button" class="btn-ghost" data-promise="' + c.id + '">Promessa</button>' +
      '<button type="button" class="btn-ghost" data-pay="' + c.id + '">Receber</button>' +
      '<a class="btn-ghost" href="recibo.html?id=' + encodeURIComponent(c.id) + '">Recibo</a>' +
      '<button type="button" class="btn-ghost" data-rev="' + c.id + '">Estorno</button>' +
      '<a class="btn-ghost" href="cliente.html?id=' + encodeURIComponent(c.clienteId) + '">Cliente</a>' +
      '<a class="btn-ghost" href="contratos.html">Contrato</a>' +
      '<button type="button" class="btn-ghost" data-contact="' + c.id + '">Contato</button>' +
      '<button type="button" class="btn-ghost" data-task="' + c.id + '">Tarefa</button>' +
      "</div></td></tr>"
    ).join("");
  }

  document.getElementById("aging").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-aging]");
    if (!btn) return;
    aging = btn.getAttribute("data-aging");
    document.querySelectorAll("#aging [data-aging]").forEach((el) => el.classList.toggle("active", el === btn));
    render();
  });
  body.addEventListener("click", async (e) => {
    const wa = e.target.closest("[data-wa]");
    const promise = e.target.closest("[data-promise]");
    const pay = e.target.closest("[data-pay]");
    const rev = e.target.closest("[data-rev]");
    const contact = e.target.closest("[data-contact]");
    const task = e.target.closest("[data-task]");
    if (wa) {
      const charge = PF.store.charges.get(wa.getAttribute("data-wa"));
      if (!charge) return;
      const ok = PF.openWhatsApp(charge.telefone, PF.whatsappChargeMessage(charge));
      if (!ok) return PF.ui.toast("Cliente sem telefone.", "error");
      try { await PF.store.contactCharge(charge.id, "whatsapp", "mensagem enviada", "Inadimplência"); } catch (err) { /* noop */ }
    }
    if (promise) {
      const charge = PF.store.charges.get(promise.getAttribute("data-promise"));
      if (!charge) return;
      const data = prompt("Data da promessa (AAAA-MM-DD):", PF.isoDate(3));
      if (!data) return;
      const valor = Number(prompt("Valor prometido:", charge.saldo || charge.valor));
      const observacao = prompt("Observação da promessa:", "") || "";
      try {
        await PF.store.promiseCharge(charge.id, data, valor, { observacao: observacao });
        PF.ui.toast("Promessa registrada.");
        render();
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível registrar a promessa.", "error");
      }
    }
    if (pay) {
      const charge = PF.store.charges.get(pay.getAttribute("data-pay"));
      if (!charge) return;
      const valor = Number(prompt("Valor recebido:", charge.saldo || charge.valor));
      if (!valor) return;
      const forma = prompt("Forma: pix, dinheiro, transferencia, cartao, outro", "pix") || "pix";
      try {
        await PF.store.payCharge(charge.id, valor, { forma: forma });
        PF.ui.toast("Pagamento registrado.");
        render();
        if (PF.ui.confirm("Abrir recibo para imprimir ou enviar?")) {
          window.location.href = "recibo.html?id=" + encodeURIComponent(charge.id);
        }
      } catch (err) {
        if (err.payload && err.payload.codigo === "excedente") {
          const dest = prompt("Excedente de " + PF.formatMoney(err.payload.excedente) + ".\n1 amortizar principal\n2 antecipar próxima\n3 crédito", "1");
          const map = { "1": "amortizar_principal", "2": "antecipar", "3": "credito" };
          if (!dest) return;
          try {
            await PF.store.payCharge(charge.id, valor, { forma: forma, excedente: map[dest] || "credito" });
            PF.ui.toast("Pagamento com excedente registrado.");
            render();
          } catch (err2) {
            PF.ui.toast(err2.message || "Não foi possível registrar o pagamento.", "error");
          }
          return;
        }
        PF.ui.toast(err.message || "Não foi possível registrar o pagamento.", "error");
      }
    }
    if (rev) {
      const charge = PF.store.charges.get(rev.getAttribute("data-rev"));
      if (!charge) return;
      if (!Number(charge.valorPago)) return PF.ui.toast("Não há pagamento para estornar nesta parcela.", "error");
      if (!PF.ui.confirm("Estornar " + PF.formatMoney(charge.valorPago) + " desta parcela?")) return;
      const motivo = prompt("Motivo do estorno:", "Digitação duplicada") || "Estorno operacional";
      try {
        await PF.store.reverseCharge(charge.id, motivo);
        PF.ui.toast("Estorno registrado.");
        render();
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível estornar.", "error");
      }
    }
    if (contact) {
      const charge = PF.store.charges.get(contact.getAttribute("data-contact"));
      if (!charge) return;
      const canal = prompt("Canal: whatsapp, ligacao, email, sms, presencial, outro", "ligacao") || "ligacao";
      const resultado = prompt("Resultado:", "contato realizado") || "contato realizado";
      const proxima = prompt("Próxima ação: cobrar hoje, cobrar amanha, ligar, aguardar promessa, renegociar, revisar garantia", "cobrar amanha");
      try {
        await PF.store.contactCharge(charge.id, canal, resultado, "Inadimplência", null);
        if (proxima) await PF.store.contactCharge(charge.id, canal, resultado, "Inadimplência", PF.isoDate(1));
        await PF.api.post("/api/cobrancas/" + charge.id + "/contato", { canal: canal, resultado: resultado, observacao: "Registro de cobrança", proximaAcao: proxima });
        await PF.store.hydrate();
        PF.ui.toast("Contato registrado.");
        render();
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível registrar o contato.", "error");
      }
    }
    if (task) {
      const charge = PF.store.charges.get(task.getAttribute("data-task"));
      if (!charge) return;
      const date = prompt("Data da tarefa (AAAA-MM-DD):", PF.isoDate(1));
      if (!date) return;
      const motivo = prompt("Motivo:", "Cobrança") || "Cobrança";
      try {
        await PF.store.appointments.add({
          title: motivo + " · " + (charge.clienteNome || "Cliente"),
          notes: "Parcela " + charge.numero + " · " + PF.formatMoney(charge.saldo || charge.valor),
          date: date,
          clientId: charge.clienteId,
          contractId: charge.contratoId,
          status: "aberto",
        });
        PF.ui.toast("Tarefa na agenda.");
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível criar a tarefa.", "error");
      }
    }
  });
  render();
})();
