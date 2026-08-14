(async function () {
  await PF.layout.mount("exportacao");

  function download(name, content, type) {
    const blob = new Blob([content], { type: type || "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }

  function toCsv(rows) {
    return rows.map((r) => r.map((c) => '"' + String(c).replace(/"/g, '""') + '"').join(";")).join("\n");
  }

  document.getElementById("export-clients").addEventListener("click", () => {
    const rows = [["Nome", "CPF", "Telefone", "Email", "Status"]].concat(
      PF.store.clients.all().map((c) => [c.nome, c.cpf, c.telefone, c.email, c.status])
    );
    download("clientes.csv", toCsv(rows), "text/csv");
    PF.ui.toast("CSV de clientes gerado.");
  });
  document.getElementById("export-contracts").addEventListener("click", () => {
    const rows = [["Id", "Cliente", "Tipo", "Valor", "Saldo", "Status"]].concat(
      PF.store.contracts.all().map((c) => {
        const cli = PF.store.clients.get(c.clienteId);
        return [c.id, cli ? cli.nome : "", c.tipo, c.valorTotal, c.saldoDevedor, c.status];
      })
    );
    download("contratos.csv", toCsv(rows), "text/csv");
  });
  document.getElementById("backup-now").addEventListener("click", () => {
    download("pixfactory-backup.json", PF.store.exportJson(), "application/json");
    PF.ui.toast("Snapshot JSON baixado (consulta à API).");
  });
  document.getElementById("restore-file").addEventListener("change", () => {
    PF.ui.toast("A restauração agora é feita pelo banco MySQL/seed DEMO.", "warn");
  });
})();
