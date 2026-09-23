(async function () {
  await PF.layout.mount("cadastro");
  const form = document.getElementById("cadastro-form");
  const cpf = document.getElementById("cpf");
  const telefone = document.getElementById("telefone");
  cpf.addEventListener("input", () => { cpf.value = PF.maskCpf(cpf.value); });
  telefone.addEventListener("input", () => { telefone.value = PF.maskPhone(telefone.value); });

  document.getElementById("foto-cliente").addEventListener("change", (event) => {
    const file = event.target.files[0];
    if (!file) return;
    const img = document.getElementById("preview");
    img.src = URL.createObjectURL(file);
    img.hidden = false;
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
      nome: document.getElementById("nome").value.trim(),
      cpf: cpf.value,
      telefone: telefone.value,
      email: document.getElementById("email").value.trim(),
      dataNascimento: document.getElementById("dataNascimento").value,
      endereco: document.getElementById("endereco").value.trim(),
      profissao: document.getElementById("profissao").value.trim(),
      rendaMensal: Number(document.getElementById("rendaMensal").value || 0),
      banco: document.getElementById("banco").value.trim(),
      classificacao: document.getElementById("classificacao").value,
      foto: document.getElementById("preview").getAttribute("src") || "assets/favicon.svg",
      observacoes: "",
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
        { nome: document.getElementById("ref2-nome").value.trim(), telefone: document.getElementById("ref2-telefone").value.trim(), relacao: document.getElementById("ref2-relacao").value.trim() },
      ].filter((r) => r.nome),
    };
    const errors = PF.validate({
      nome: { value: payload.nome, checks: [PF.validators.required] },
      cpf: { value: payload.cpf, checks: [PF.validators.cpf] },
      telefone: { value: payload.telefone, checks: [PF.validators.phone] },
      email: { value: payload.email, checks: [PF.validators.email] },
      endereco: { value: payload.endereco, checks: [PF.validators.required] },
      classificacao: { value: payload.classificacao, checks: [PF.validators.required] },
    });
    PF.ui.showErrors(errors);
    if (Object.keys(errors).length) return;
    try {
      await PF.store.clients.add(payload);
      PF.ui.toast("Cliente salvo.");
      window.location.href = "clientes.html";
    } catch (err) {
      PF.ui.showErrors({ cpf: err.message || "Não foi possível salvar." });
    }
  });
})();
