(function (root) {
  const PF = root.PF = root.PF || {};

  PF.validators = {
    required(value) {
      return String(value || "").trim() ? "" : "Campo obrigatório.";
    },
    email(value) {
      const v = String(value || "").trim();
      if (!v) return "Informe o e-mail.";
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? "" : "E-mail inválido.";
    },
    password(value) {
      const v = String(value || "");
      if (v.length < 8) return "A senha deve ter pelo menos 8 caracteres.";
      if (!/[A-Z]/.test(v) || !/[a-z]/.test(v) || !/\d/.test(v)) {
        return "Use letras maiúsculas, minúsculas e número.";
      }
      return "";
    },
    cpf(value) {
      const d = PF.digits(value);
      if (d.length !== 11 || /^(\d)\1+$/.test(d)) return "CPF inválido.";
      const calc = (len) => {
        let sum = 0;
        for (let i = 0; i < len; i += 1) sum += Number(d[i]) * (len + 1 - i);
        const rest = (sum * 10) % 11;
        return rest === 10 ? 0 : rest;
      };
      if (calc(9) !== Number(d[9]) || calc(10) !== Number(d[10])) return "CPF inválido.";
      return "";
    },
    phone(value) {
      const d = PF.digits(value);
      return d.length >= 10 && d.length <= 11 ? "" : "Telefone inválido.";
    },
    money(value) {
      const n = Number(value);
      return Number.isFinite(n) && n >= 0 ? "" : "Valor inválido.";
    },
  };

  PF.validate = function validate(rules) {
    const errors = {};
    Object.keys(rules).forEach((key) => {
      const { value, checks } = rules[key];
      for (let i = 0; i < checks.length; i += 1) {
        const msg = checks[i](value);
        if (msg) {
          errors[key] = msg;
          break;
        }
      }
    });
    return errors;
  };
})(typeof window !== "undefined" ? window : globalThis);
