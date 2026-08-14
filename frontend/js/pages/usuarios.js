(async function () {
  const user = await PF.layout.mount("usuarios");
  if (user.role !== "ADMIN") {
    document.getElementById("users-root").innerHTML = PF.ui.empty("Acesso restrito a administradores.");
    return;
  }
  const tbody = document.getElementById("users-body");
  const search = document.getElementById("user-search");

  function render() {
    const q = String(search.value || "").toLowerCase();
    const rows = PF.store.users.all().filter((u) => (u.name + u.email).toLowerCase().includes(q));
    tbody.innerHTML = rows.map((u) =>
      "<tr><td data-label='Nome'>" + PF.escapeHtml(u.name) + "</td><td data-label='E-mail'>" + PF.escapeHtml(u.email) +
      "</td><td data-label='Papel'>" + u.role + "</td><td data-label='Plano'>" + u.plan + "</td><td data-label='Status'>" + u.status +
      "</td><td data-label='Ações' class='actions-row'>" +
      '<button data-toggle="' + u.id + '">' + (u.status === "ativo" ? "Inativar" : "Ativar") + "</button>" +
      '<button data-role="' + u.id + '">Alternar papel</button>' +
      (u.id === user.id ? "" : '<button class="btn-danger" data-del="' + u.id + '">Excluir</button>') +
      "</td></tr>"
    ).join("");
  }

  document.getElementById("add-user-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = document.getElementById("new-name").value.trim();
    const email = document.getElementById("new-email").value.trim();
    const password = document.getElementById("new-pass").value;
    const role = document.getElementById("new-role").value;
    if (PF.validators.email(email)) return PF.ui.toast(PF.validators.email(email), "error");
    if (PF.validators.password(password)) return PF.ui.toast(PF.validators.password(password), "error");
    try {
      await PF.store.users.add({ name, email: email.toLowerCase(), phone: "", password, role });
      e.target.reset();
      PF.ui.toast("Usuário criado.");
      render();
    } catch (err) {
      PF.ui.toast(err.message || "E-mail já cadastrado.", "error");
    }
  });
  search.addEventListener("input", render);
  tbody.addEventListener("click", async (e) => {
    const tog = e.target.closest("[data-toggle]");
    const role = e.target.closest("[data-role]");
    const del = e.target.closest("[data-del]");
    if (tog) {
      const u = PF.store.users.get(tog.getAttribute("data-toggle"));
      await PF.store.users.update(u.id, { status: u.status === "ativo" ? "inativo" : "ativo" });
    }
    if (role) {
      const u = PF.store.users.get(role.getAttribute("data-role"));
      await PF.store.users.update(u.id, { role: u.role === "ADMIN" ? "USER" : "ADMIN" });
    }
    if (del) {
      if (!PF.ui.confirm("Excluir este usuário?")) return;
      await PF.store.users.remove(del.getAttribute("data-del"));
    }
    render();
  });
  render();
})();
