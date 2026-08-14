(async function () {
  await PF.layout.mount("clientes");
  const id = new URLSearchParams(window.location.search).get("id") || new URLSearchParams(window.location.search).get("clienteId");
  const client = PF.store.clients.get(id);
  if (!client) {
    document.getElementById("edit-root").innerHTML = PF.ui.empty("Cliente não encontrado.") + '<p><a href="clientes.html">Voltar</a></p>';
    return;
  }

  const set = (k, v) => { const el = document.getElementById(k); if (el) el.value = v || ""; };
  set("nome", client.nome);
  set("cpf", client.cpf);
  set("telefone", client.telefone);
  set("email", client.email);
  set("dataNascimento", client.dataNascimento);
  set("endereco", client.endereco);
  set("profissao", client.profissao);
  set("rendaMensal", client.rendaMensal);
  set("banco", client.banco);
  set("classificacao", client.classificacao);

  document.getElementById("cpf").addEventListener("input", (e) => { e.target.value = PF.maskCpf(e.target.value); });
  document.getElementById("telefone").addEventListener("input", (e) => { e.target.value = PF.maskPhone(e.target.value); });

  document.getElementById("edit-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
      nome: document.getElementById("nome").value.trim(),
      cpf: document.getElementById("cpf").value,
      telefone: document.getElementById("telefone").value,
      email: document.getElementById("email").value.trim(),
      dataNascimento: document.getElementById("dataNascimento").value,
      endereco: document.getElementById("endereco").value.trim(),
      profissao: document.getElementById("profissao").value.trim(),
      rendaMensal: Number(document.getElementById("rendaMensal").value || 0),
      banco: document.getElementById("banco").value.trim(),
      classificacao: document.getElementById("classificacao").value,
    };
    const errors = PF.validate({
      nome: { value: payload.nome, checks: [PF.validators.required] },
      cpf: { value: payload.cpf, checks: [PF.validators.cpf] },
      telefone: { value: payload.telefone, checks: [PF.validators.phone] },
      email: { value: payload.email, checks: [PF.validators.email] },
    });
    PF.ui.showErrors(errors);
    if (Object.keys(errors).length) return;
    try {
      await PF.store.clients.update(client.id, payload);
      PF.ui.toast("Cliente atualizado.");
      window.location.href = "clientes.html";
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível atualizar.", "error");
    }
  });
})();
