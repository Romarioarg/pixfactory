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

  PF.openWhatsApp = function openWhatsApp(phone, message) {
    const d = PF.digits(phone);
    if (!d) return false;
    window.open("https://wa.me/55" + d + "?text=" + encodeURIComponent(message), "_blank");
    return true;
  };

  PF.whatsappChargeMessage = function whatsappChargeMessage(opts) {
    const o = opts || {};
    const nome = o.nome || o.clienteNome || "cliente";
    const valor = PF.formatMoney(o.valor);
    const data = PF.formatDate(o.vencimento);
    const today = PF.isoDate(0);
    let msg = "Olá, " + nome + "! Tudo bem? Sua cobrança no valor de " + valor;
    if (o.status === "atrasado" || (o.vencimento && o.vencimento < today)) {
      msg += " venceu em " + data;
    } else if (o.vencimento === today) {
      msg += " vence hoje";
    } else {
      msg += " vence em " + data;
    }
    if (o.numero) msg += " (parcela " + o.numero + ")";
    msg += ". PixFactory.";
    return msg;
  };

  PF.whatsappReceiptMessage = function whatsappReceiptMessage(opts) {
    const o = opts || {};
    const nome = o.nome || o.clienteNome || "cliente";
    return "Olá, " + nome + "! Segue o recibo operacional da parcela " + (o.numero || "") +
      " no valor pago de " + PF.formatMoney(o.valorPago || o.valor) +
      ". Recibo " + (o.reciboId || ("PF-" + (o.id || ""))) + ". PixFactory DEMO.";
  };

  PF.statusLabel = function statusLabel(status) {
    const map = {
      pago: "Pago",
      parcial: "Parcial",
      cancelado: "Cancelado",
      renegociada: "Renegociada",
      ativo: "Em dia",
      atrasado: "Em atraso",
      pendente: "Pendente",
      vencendo_hoje: "Vencendo hoje",
      a_vencer: "A vencer",
      encerrado: "Quitado",
      renegociado: "Renegociado",
      acordo: "Acordo",
      hold: "Em espera",
      falecimento: "Encerrado",
    };
    return map[status] || status || "N/A";
  };

  PF.statusClass = function statusClass(status) {
    if (status === "atrasado") return "badge-danger";
    if (status === "pendente" || status === "hold" || status === "acordo" || status === "parcial" || status === "renegociado") return "badge-warn";
    if (status === "encerrado" || status === "ativo" || status === "pago") return "badge-ok";
    return "badge-muted";
  };
})(typeof window !== "undefined" ? window : globalThis);
