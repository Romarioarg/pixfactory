(async function () {
  await PF.layout.mount("agenda");
  let month = new Date().getMonth();
  let year = new Date().getFullYear();
  const grid = document.getElementById("calendar-grid");
  const title = document.getElementById("month-year");
  const details = document.getElementById("day-details");
  let selected = PF.isoDate(0);

  function cobrancas() {
    const fromContracts = PF.store.contracts.all()
      .filter((c) => c.proximoPagamento && c.proximoPagamento !== "-" && c.status !== "encerrado" && c.status !== "falecimento")
      .map((c) => {
        const client = PF.store.clients.get(c.clienteId);
        return {
          date: c.proximoPagamento,
          nome: client ? client.nome : "Cliente",
          telefone: client ? client.telefone : "",
          valor: (Number(c.saldoDevedor) / Math.max(1, Number(c.parcelasTotais) - Number(c.parcelasPagas))) || Number(c.saldoDevedor),
          source: "contrato",
        };
      });
    const fromAppointments = PF.store.appointments.all().map((a) => ({
      date: a.date,
      nome: a.title,
      telefone: "",
      valor: 0,
      notes: a.notes,
      id: a.id,
      source: "agenda",
    }));
    return fromContracts.concat(fromAppointments);
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
      const has = items.some((c) => c.date === iso);
      html += '<button type="button" class="day' + (iso === today ? " today" : "") + (iso === selected ? " active" : "") + '" data-date="' + iso + '">' + day + (has ? '<span class="dot"></span>' : "") + "</button>";
    }
    grid.innerHTML = html;
  }

  function showDay(iso) {
    selected = iso;
    const items = cobrancas().filter((c) => c.date === iso);
    details.innerHTML = "<h3>" + PF.formatDate(iso) + "</h3>" + (items.length ? "<ul>" + items.map((c) =>
      "<li class='flex' style='justify-content:space-between'><span><strong>" + PF.escapeHtml(c.nome) + "</strong><br>" +
      (c.valor ? PF.formatMoney(c.valor) : PF.escapeHtml(c.notes || "Agendamento")) + "</span>" +
      (c.source === "agenda" ? '<button type="button" class="btn-danger" data-del-ap="' + c.id + '">Excluir</button>' : "") +
      (c.telefone ? '<button type="button" data-wa="' + PF.digits(c.telefone) + '" data-name="' + PF.escapeHtml(c.nome) + '">Cobrar</button>' : "") + "</li>"
    ).join("") + "</ul>" : PF.ui.empty("Nenhuma cobrança neste dia.")) +
      '<form id="ap-form" class="actions-row" style="margin-top:12px;flex-wrap:wrap">' +
      '<label class="sr-only" for="ap-title">Título</label>' +
      '<input id="ap-title" required placeholder="Novo agendamento">' +
      '<button type="submit">Adicionar</button></form>';
  }

  grid.addEventListener("click", (e) => {
    const day = e.target.closest("[data-date]");
    if (!day) return;
    grid.querySelectorAll(".day").forEach((el) => el.classList.remove("active"));
    day.classList.add("active");
    showDay(day.getAttribute("data-date"));
  });
  details.addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-wa]");
    const del = e.target.closest("[data-del-ap]");
    if (btn) {
      const msg = encodeURIComponent("Olá " + btn.getAttribute("data-name") + ", lembrete de cobrança do PixFactory.");
      window.open("https://wa.me/55" + btn.getAttribute("data-wa") + "?text=" + msg, "_blank");
    }
    if (del) {
      if (!PF.ui.confirm("Excluir este agendamento?")) return;
      await PF.store.appointments.remove(del.getAttribute("data-del-ap"));
      render();
      showDay(selected);
    }
  });
  details.addEventListener("submit", async (e) => {
    if (e.target.id !== "ap-form") return;
    e.preventDefault();
    const titleValue = document.getElementById("ap-title").value.trim();
    if (!titleValue) return;
    await PF.store.appointments.add({ title: titleValue, date: selected, notes: "" });
    render();
    showDay(selected);
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
