(function (root) {
  const PF = root.PF = root.PF || {};

  PF.hashPassword = function hashPassword(password) {
    const str = "pf.v1|" + String(password);
    let h = 5381;
    for (let i = 0; i < str.length; i += 1) {
      h = (h << 5) + h + str.charCodeAt(i);
    }
    return "djb2:" + (h >>> 0).toString(16);
  };

  PF.uid = function uid(prefix) {
    return (prefix || "id") + "_" + Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
  };

  PF.isoDate = function isoDate(offsetDays) {
    const d = new Date();
    d.setHours(12, 0, 0, 0);
    d.setDate(d.getDate() + (offsetDays || 0));
    return d.toISOString().slice(0, 10);
  };

  PF.formatMoney = function formatMoney(value) {
    const n = Number(value) || 0;
    return n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  };

  PF.formatDate = function formatDate(iso) {
    if (!iso || iso === "-") return "—";
    const [y, m, d] = String(iso).split("-");
    if (!d) return iso;
    return d + "/" + m + "/" + y;
  };

  PF.digits = function digits(value) {
    return String(value || "").replace(/\D/g, "");
  };

  PF.maskCpf = function maskCpf(value) {
    const d = PF.digits(value).slice(0, 11);
    return d
      .replace(/(\d{3})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
  };

  PF.maskPhone = function maskPhone(value) {
    const d = PF.digits(value).slice(0, 11);
    if (d.length <= 10) {
      return d.replace(/(\d{2})(\d{4})(\d{0,4})/, "($1) $2-$3").replace(/-$/, "");
    }
    return d.replace(/(\d{2})(\d{5})(\d{0,4})/, "($1) $2-$3").replace(/-$/, "");
  };

  PF.escapeHtml = function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  };

  PF.statusLabel = function statusLabel(status) {
    const map = {
      ativo: "Em dia",
      atrasado: "Em atraso",
      pendente: "Pendente",
      encerrado: "Quitado",
      acordo: "Acordo",
      hold: "Em espera",
      falecimento: "Encerrado",
    };
    return map[status] || status || "N/A";
  };

  PF.statusClass = function statusClass(status) {
    if (status === "atrasado") return "badge-danger";
    if (status === "pendente" || status === "hold" || status === "acordo") return "badge-warn";
    if (status === "encerrado" || status === "ativo") return "badge-ok";
    return "badge-muted";
  };
})(typeof window !== "undefined" ? window : globalThis);
