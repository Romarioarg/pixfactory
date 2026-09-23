(async function () {
  await PF.layout.mount("agenda");
  let month = new Date().getMonth();
  let year = new Date().getFullYear();
  const grid = document.getElementById("calendar-grid");
  const title = document.getElementById("month-year");
  const details = document.getElementById("day-details");
  const filters = document.getElementById("agenda-filters");
  let selected = PF.isoDate(0);
  let range = "mes";

  function cobrancas() {
    const fromCharges = PF.store.charges.all()
      .filter((c) => c.vencimento && c.status !== "pago" && c.status !== "cancelado")
      .map((c) => ({
        id: c.id,
        date: c.vencimento,
        nome: c.clienteNome || "Cliente",
        telefone: c.telefone || "",
        valor: c.saldo || c.valor,
        status: c.status,
        numero: c.numero,
        vencimento: c.vencimento,
        source: "cobranca",
      }));
    const fromAppointments = PF.store.appointments.all().map((a) => ({
      date: a.date,
      nome: a.title,
      telefone: "",
      valor: 0,
      notes: a.notes,
      id: a.id,
      source: "agenda",
    }));
    return fromCharges.concat(fromAppointments);
  }

  function inRange(item) {
    const today = PF.isoDate(0);
    const tomorrow = PF.isoDate(1);
    if (range === "hoje") return item.date === today;
    if (range === "amanha") return item.date === tomorrow;
    if (range === "atrasados") return item.source === "cobranca" && item.status === "atrasado";
    if (range === "semana") {
      const start = new Date();
      start.setHours(12, 0, 0, 0);
      const end = new Date(start);
      end.setDate(start.getDate() + 7);
      const d = new Date(item.date + "T12:00:00");
      return d >= start && d <= end;
    }
    return true;
  }

  function render() {
    const names = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];
    title.textContent = names[month] + " " + year;
    const first = new Date(year, month, 1).getDay();
    const days = new Date(year, month + 1, 0).getDate();
    const items = cobrancas();
    let html = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"].map((d) => '<div class="day-name">' + d + "</div>").join("");
    for (let i = 0; i < first; i += 1) html += '<div class="day empty"></div>';
    const today = PF.isoDate(0);
    for (let day = 1; day <= days; day += 1) {
      const iso = new Date(year, month, day).toISOString().slice(0, 10);
      const has = items.some((c) => c.date === iso && c.source === "cobranca");
      html += '<button type="button" class="day' + (iso === today ? " today" : "") + (iso === selected ? " active" : "") + '" data-date="' + iso + '">' + day + (has ? '<span class="dot"></span>' : "") + "</button>";
    }
    grid.innerHTML = html;
  }

  function chargeRow(c) {
    return "<li class='flex' style='justify-content:space-between;gap:8px;flex-wrap:wrap'><span><strong>" + PF.escapeHtml(c.nome) + "</strong><br>" +
      (c.valor ? PF.formatMoney(c.valor) : PF.escapeHtml(c.notes || "Agendamento")) +
      (c.status ? ' · <span class="badge ' + PF.statusClass(c.status) + '">' + PF.statusLabel(c.status) + "</span>" : "") +
      "</span><span class='actions-row'>" +
      (c.source === "agenda" ? '<button type="button" class="btn-danger" data-del-ap="' + c.id + '">Excluir</button>' : "") +
      (c.telefone ? '<button type="button" data-wa="' + c.id + '">Cobrar</button>' : "") +
      (c.source === "cobranca" ? '<button type="button" class="btn-ghost" data-pay="' + c.id + '">Receber</button>' : "") +
      (c.source === "cobranca" ? '<a class="btn-ghost" href="recibo.html?id=' + encodeURIComponent(c.id) + '">Recibo</a>' : "") +
      (c.source === "cobranca" ? '<button type="button" class="btn-ghost" data-rev="' + c.id + '">Estorno</button>' : "") +
      "</span></li>";
  }

  function showDay(iso) {
    selected = iso;
    const items = cobrancas().filter((c) => c.date === iso);
    details.innerHTML = "<h3>" + PF.formatDate(iso) + "</h3>" + (items.length ? "<ul>" + items.map(chargeRow).join("") + "</ul>" : PF.ui.empty("Nenhuma cobrança prevista para esta data.")) +
      '<form id="ap-form" class="actions-row" style="margin-top:12px;flex-wrap:wrap">' +
      '<label class="sr-only" for="ap-title">Título</label>' +
      '<input id="ap-title" required placeholder="Novo agendamento">' +
      '<button type="submit">Adicionar</button></form>';
  }

  function showFiltered() {
    const items = cobrancas().filter(inRange);
    const label = { hoje: "Hoje", amanha: "Amanhã", semana: "Próximos 7 dias", atrasados: "Atrasados" }[range] || "";
    details.innerHTML = "<h3>" + label + "</h3>" + (items.length ? "<ul>" + items.map(chargeRow).join("") + "</ul>" : PF.ui.empty("Nenhuma cobrança neste filtro."));
  }

  function refreshList() {
    if (range === "mes") showDay(selected);
    else showFiltered();
  }

  grid.addEventListener("click", (e) => {
    const day = e.target.closest("[data-date]");
    if (!day) return;
    range = "mes";
    filters.querySelectorAll(".filter-chip").forEach((el) => el.classList.toggle("active", el.getAttribute("data-range") === "mes"));
    grid.querySelectorAll(".day").forEach((el) => el.classList.remove("active"));
    day.classList.add("active");
    showDay(day.getAttribute("data-date"));
  });
  filters.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-range]");
    if (!btn) return;
    range = btn.getAttribute("data-range");
    filters.querySelectorAll(".filter-chip").forEach((el) => el.classList.toggle("active", el === btn));
    if (range === "hoje") selected = PF.isoDate(0);
    if (range === "amanha") selected = PF.isoDate(1);
    render();
    refreshList();
  });
  details.addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-wa]");
    const del = e.target.closest("[data-del-ap]");
    const pay = e.target.closest("[data-pay]");
    const rev = e.target.closest("[data-rev]");
    if (btn) {
      const charge = PF.store.charges.get(btn.getAttribute("data-wa"));
      if (!charge) return;
      const ok = PF.openWhatsApp(charge.telefone, PF.whatsappChargeMessage(charge));
      if (!ok) return PF.ui.toast("Cliente sem telefone.", "error");
      try { await PF.store.contactCharge(charge.id, "whatsapp", "mensagem enviada", "Agenda"); } catch (err) { /* noop */ }
    }
    if (pay) {
      const charge = PF.store.charges.get(pay.getAttribute("data-pay"));
      if (!charge) return;
      const valor = Number(prompt("Valor recebido:", charge.saldo || charge.valor));
      if (!valor || valor <= 0) return;
      try {
        await PF.store.payCharge(charge.id, valor);
        render();
        refreshList();
        PF.ui.toast("Pagamento registrado.");
        if (PF.ui.confirm("Abrir recibo para imprimir ou enviar?")) {
          window.location.href = "recibo.html?id=" + encodeURIComponent(charge.id);
        }
      } catch (err) {
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
        render();
        refreshList();
        PF.ui.toast("Estorno registrado.");
      } catch (err) {
        PF.ui.toast(err.message || "Não foi possível estornar.", "error");
      }
    }
    if (del) {
      if (!PF.ui.confirm("Excluir este agendamento?")) return;
      await PF.store.appointments.remove(del.getAttribute("data-del-ap"));
      render();
      refreshList();
    }
  });
  details.addEventListener("submit", async (e) => {
    if (e.target.id !== "ap-form") return;
    e.preventDefault();
    const titleValue = document.getElementById("ap-title").value.trim();
    if (!titleValue) return;
    await PF.store.appointments.add({ title: titleValue, date: selected, notes: "" });
    render();
    refreshList();
    PF.ui.toast("Agendamento salvo.");
  });
  document.getElementById("prev-month").addEventListener("click", () => {
    month -= 1;
    if (month < 0) { month = 11; year -= 1; }
    render();
  });
  document.getElementById("next-month").addEventListener("click", () => {
    month += 1;
    if (month > 11) { month = 0; year += 1; }
    render();
  });
  render();
  showDay(PF.isoDate(0));
})();
