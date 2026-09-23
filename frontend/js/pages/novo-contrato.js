(async function () {
  await PF.layout.mount("contratos");
  const select = document.getElementById("cliente");
  const preset = new URLSearchParams(window.location.search).get("clienteId") || new URLSearchParams(window.location.search).get("id");
  select.innerHTML = '<option value="">Selecione</option>' + PF.store.clients.all().map((c) =>
    '<option value="' + c.id + '"' + (String(c.id) === String(preset) ? " selected" : "") + ">" + PF.escapeHtml(c.nome) + "</option>"
  ).join("");

  const preview = document.getElementById("contrato-preview");
  const modoInput = document.getElementById("modo");
  const parcelas = document.getElementById("parcelas");
  const abertoOption = document.getElementById("parcelas-aberto");
  let simulation = null;

  function tipoValue() {
    return document.getElementById("tipo").value;
  }

  function isRent() {
    return tipoValue() === "Aluguel";
  }

  function isOpenInterest() {
    return !isRent() && parcelas.value === "aberto";
  }

  function syncModeFromTipo() {
    if (tipoValue() === "Empréstimo rotativo") {
      parcelas.value = "aberto";
      setModo("juros_rotativo");
      return;
    }
    if (isRent()) {
      document.getElementById("modo-box").hidden = true;
      document.getElementById("sistema-wrap").hidden = true;
      document.getElementById("carencia-wrap").hidden = true;
      document.getElementById("parcelas-wrap").hidden = false;
      if (abertoOption) abertoOption.hidden = true;
      if (parcelas.value === "aberto") parcelas.value = "6";
      document.getElementById("parcelas-hint").textContent = "Quantos aluguéis lançar na agenda.";
    } else {
      document.getElementById("modo-box").hidden = false;
      if (abertoOption) abertoOption.hidden = false;
      document.getElementById("parcelas-hint").textContent = "Prazo fechado (Price, SAC, balloon) ou somente juros até o cliente quitar o capital.";
      setModo(isOpenInterest() ? "juros_rotativo" : modoInput.value === "juros_rotativo" ? "parcela_cheia" : modoInput.value);
    }
  }

  function setModo(modo) {
    if (modo === "juros_rotativo") {
      parcelas.value = "aberto";
    } else if (parcelas.value === "aberto") {
      parcelas.value = "6";
    }
    modoInput.value = modo;
    document.querySelectorAll("#modo-choices .choice").forEach((el) => {
      el.classList.toggle("active", el.getAttribute("data-modo") === modo);
    });
    const sistema = document.getElementById("sistema");
    if (modo === "so_juros") sistema.value = "americano";
    document.getElementById("carencia-wrap").hidden = modo !== "parcela_cheia" || isRent();
    document.getElementById("sistema-wrap").hidden = modo !== "parcela_cheia" || isRent();
    document.getElementById("parcelas-wrap").hidden = false;
    const help = document.getElementById("modo-help");
    if (modo === "juros_rotativo") {
      help.textContent = "Somente juros até quitação: não existe 6 parcelas. R$ 1.000 a 20% = R$ 200 por período, até o capital voltar. O próximo juro entra na agenda quando a data chegar.";
    } else if (modo === "so_juros") {
      help.textContent = "Sistema americano: as parcelas do meio são só juros. Na última, juros + os R$ 1.000 juntos. Aqui sim vale quantidade de parcelas.";
    } else {
      help.textContent = "Parcela cheia: cada vencimento já abate um pedaço do capital. Use Price (fixa) ou SAC (cai com o tempo).";
    }
  }

  function syncFromParcelas() {
    if (isRent()) return;
    if (parcelas.value === "aberto") {
      setModo("juros_rotativo");
    } else if (modoInput.value === "juros_rotativo") {
      setModo("parcela_cheia");
    }
  }

  function sistemaForSave() {
    if (isRent()) return "aluguel";
    if (isOpenInterest()) return "so_juros_aberto";
    if (modoInput.value === "so_juros") return "americano";
    return document.getElementById("sistema").value;
  }

  async function calc() {
    const valor = Number(document.getElementById("valor").value || 0);
    const juros = Number(document.getElementById("juros").value || 0);
    const open = isOpenInterest();
    const n = open ? 1 : Number(parcelas.value || 0);
    if (!valor || (!open && !n)) return null;
    simulation = await PF.api.post("/api/simulacoes", {
      valor: valor,
      juros: juros,
      parcelas: n,
      sistema: sistemaForSave(),
      modo: isRent() ? "parcela_cheia" : (open ? "juros_rotativo" : modoInput.value),
      tipo: tipoValue().toLowerCase(),
      periodicidade: document.getElementById("periodicidade").value,
      carencia: Number(document.getElementById("carencia").value || 0),
      primeiroVencimento: document.getElementById("vencimento").value || PF.isoDate(30),
      baseCalculo: document.getElementById("baseCalculo").value,
      jurosFixo: Number(document.getElementById("jurosFixo").value || 0),
    });
    return simulation;
  }

  function rowLabel(tipo) {
    if (tipo === "principal") return "Devolução do capital";
    if (tipo === "balloon") return "Juros + capital";
    if (tipo === "so_juros") return "Só juros";
    if (tipo === "aluguel") return "Aluguel";
    if (tipo === "carencia") return "Carência";
    return "Parcela";
  }

  async function refreshPreview() {
    const client = PF.store.clients.get(select.value);
    try {
      const data = await calc();
      if (!data) {
        preview.innerHTML = "<p class='hint'>Preencha valor e períodos para ver a prévia.</p>";
        return;
      }
      const rows = data.cronograma || [];
      const open = isOpenInterest();
      preview.innerHTML =
        "<h3>" + PF.escapeHtml(tipoValue()) + "</h3>" +
        "<p>Cliente: <strong>" + PF.escapeHtml(client ? client.nome : "Selecione") + "</strong></p>" +
        "<p>" + PF.escapeHtml(data.explicacao) + "</p>" +
        (open
          ? "<p class='hero-example'>Juro do período: <strong>" + PF.formatMoney(data.parcelaInicial) + "</strong> · Capital em aberto, sem data final: <strong>" + PF.formatMoney(data.principal) + "</strong>.</p>"
          : "<p>1º vencimento " + PF.formatMoney(data.parcelaInicial) + " · Total previsto: " + PF.formatMoney(data.totalPagar) + "</p>") +
        (rows.length ? "<div class='preview-table'><table><thead><tr><th>#</th><th>Tipo</th><th>Valor</th><th>Quando</th></tr></thead><tbody>" +
          rows.map((r) => "<tr><td>" + r.numero + "</td><td>" + PF.escapeHtml(rowLabel(r.tipo)) + "</td><td>" + PF.formatMoney(r.parcela) + "</td><td>" + (r.vencimento ? PF.formatDate(r.vencimento) : "Quando quitar") + "</td></tr>").join("") +
          "</tbody></table></div>" : "");
    } catch (err) {
      preview.textContent = err.message || "Não foi possível simular.";
    }
  }

  document.getElementById("modo-choices").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-modo]");
    if (!btn) return;
    setModo(btn.getAttribute("data-modo"));
    refreshPreview();
  });
  parcelas.addEventListener("change", () => {
    syncFromParcelas();
    refreshPreview();
  });
  document.getElementById("tipo").addEventListener("change", () => {
    syncModeFromTipo();
    refreshPreview();
  });
  document.getElementById("baseCalculo").addEventListener("change", () => {
    document.getElementById("fixo-wrap").hidden = document.getElementById("baseCalculo").value !== "valor_fixo";
  });
  document.getElementById("periodicidade").addEventListener("change", () => {
    document.getElementById("dias-wrap").hidden = document.getElementById("periodicidade").value !== "personalizada";
  });
  document.getElementById("novo-contrato-form").addEventListener("input", () => { refreshPreview(); });
  document.getElementById("novo-contrato-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const client = PF.store.clients.get(select.value);
    const data = simulation || await calc();
    if (!client) return PF.ui.toast("Selecione um cliente.", "error");
    if (!data) return PF.ui.toast("Informe valor e parcelas válidos.", "error");
    const open = isOpenInterest();
    try {
      await PF.store.contracts.add({
        clienteId: client.id,
        tipo: tipoValue(),
        tipoOperacao: tipoValue().toLowerCase(),
        valorTotal: Number(document.getElementById("valor").value),
        valorPago: 0,
        parcelasPagas: 0,
        parcelasTotais: open ? 1 : ((data.cronograma || []).length || Number(parcelas.value)),
        proximoPagamento: document.getElementById("vencimento").value || PF.isoDate(30),
        status: "pendente",
        juros: Number(document.getElementById("juros").value || 0),
        multa: 20,
        saldoDevedor: Number(document.getElementById("valor").value),
        sistemaAmortizacao: sistemaForSave(),
        modoPagamento: isRent() ? "parcela_cheia" : (open ? "juros_rotativo" : modoInput.value),
        periodicidade: document.getElementById("periodicidade").value,
        periodicidadeDias: Number(document.getElementById("periodicidadeDias").value || 0),
        carenciaMeses: Number(document.getElementById("carencia").value || 0),
        baseCalculo: document.getElementById("baseCalculo").value,
        jurosFixo: Number(document.getElementById("jurosFixo").value || 0),
        ordemPagamento: document.getElementById("ordemPagamento").value,
        clausulas: document.getElementById("clausulas").value,
        garantias: document.getElementById("garantia-tipo").value ? [{
          tipo: document.getElementById("garantia-tipo").value,
          descricao: document.getElementById("garantia-desc").value,
          situacao: "ativa",
        }] : [],
        avalista: {
          nome: document.getElementById("avalista-nome").value.trim(),
          telefone: document.getElementById("avalista-telefone").value.trim(),
        },
      });
      PF.ui.toast("Contrato salvo com cronograma.");
      window.location.href = "contratos.html";
    } catch (err) {
      PF.ui.toast(err.message || "Não foi possível salvar o contrato.", "error");
    }
  });

  document.getElementById("print-contract").addEventListener("click", () => window.print());
  document.getElementById("vencimento").value = PF.isoDate(30);
  syncModeFromTipo();
  refreshPreview();
})();
