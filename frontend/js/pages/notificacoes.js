(async function () {
  await PF.layout.mount("notificacoes");
  const list = document.getElementById("notif-list");
  const search = document.getElementById("notif-search");

  function render() {
    const q = String(search.value || "").toLowerCase();
    const items = PF.store.notifications.all().filter((n) => (n.title + " " + n.body).toLowerCase().includes(q));
    const emails = PF.store.emails.all();
    list.innerHTML = items.map((n) =>
      '<article class="card" style="margin-bottom:10px;opacity:' + (n.read ? "0.7" : "1") + '"><h3>' + PF.escapeHtml(n.title) +
      "</h3><p>" + PF.escapeHtml(n.body) + "</p><p class='hint'>" + PF.formatDate(n.createdAt) + "</p>" +
      '<div class="actions-row"><button type="button" data-read="' + n.id + '">' + (n.read ? "Lida" : "Marcar como lida") +
      '</button><button class="btn-danger" data-del="' + n.id + '">Excluir</button></div></article>'
    ).join("") + (emails.length ? "<h2>E-mails DEMO</h2>" + emails.map((m) =>
      '<article class="card" style="margin-bottom:10px"><h3>' + PF.escapeHtml(m.subject) + "</h3><p>Para: " +
      PF.escapeHtml(m.recipient) + " · " + PF.escapeHtml(m.status) + "</p><p class='hint'>" + PF.escapeHtml(m.body || "") + "</p></article>"
    ).join("") : "") || PF.ui.empty("Nenhuma notificação.");
  }

  search.addEventListener("input", render);
  list.addEventListener("click", async (e) => {
    const read = e.target.closest("[data-read]");
    const del = e.target.closest("[data-del]");
    if (read) await PF.store.notifications.update(read.getAttribute("data-read"), { read: true });
    if (del) await PF.store.notifications.remove(del.getAttribute("data-del"));
    render();
  });
  document.getElementById("mark-all").addEventListener("click", async () => {
    await PF.api.post("/api/notifications/read-all");
    await PF.store.hydrate();
    render();
  });
  document.getElementById("clear-all").addEventListener("click", async () => {
    if (!PF.ui.confirm("Limpar todas as notificações?")) return;
    await PF.api.del("/api/notifications");
    await PF.store.hydrate();
    render();
  });
  document.getElementById("enable-push").addEventListener("click", async () => {
    if (!("Notification" in window)) return PF.ui.toast("Este navegador não suporta notificações.", "error");
    const perm = await Notification.requestPermission();
    PF.ui.toast(perm === "granted" ? "Notificações do navegador ativadas." : "Permissão negada.", perm === "granted" ? "" : "warn");
  });
  render();
})();
