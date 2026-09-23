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
  const ind = client.indicador || {};
  set("indicador-nome", ind.nome);
  set("indicador-telefone", ind.telefone || ind.whatsapp);
  set("indicador-endereco", ind.endereco);
  set("indicador-relacao", ind.relacao);
  set("indicador-obs", ind.observacao);
  const refs = client.referencias || [];
  if (refs[0]) {
    set("ref1-nome", refs[0].nome);
    set("ref1-telefone", refs[0].telefone);
    set("ref1-relacao", refs[0].relacao);
  }

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
      indicador: {
        nome: document.getElementById("indicador-nome").value.trim(),
        telefone: document.getElementById("indicador-telefone").value.trim(),
        whatsapp: document.getElementById("indicador-telefone").value.trim(),
        endereco: document.getElementById("indicador-endereco").value.trim(),
        relacao: document.getElementById("indicador-relacao").value.trim(),
        observacao: document.getElementById("indicador-obs").value.trim(),
      },
      referencias: [
        { nome: document.getElementById("ref1-nome").value.trim(), telefone: document.getElementById("ref1-telefone").value.trim(), relacao: document.getElementById("ref1-relacao").value.trim() },
      ].filter((r) => r.nome),
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
